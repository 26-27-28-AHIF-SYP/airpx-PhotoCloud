import Photos
import SwiftUI
import UIKit

struct LibraryView: View {
    let photoLibrary: PhotoLibraryService

    @Environment(\.openURL) private var openURL
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.displayScale) private var displayScale
    @State private var photos: [LocalPhoto] = []
    @State private var sortOrder: LibrarySortOrder = .captureDate
    @State private var mediaFilter: LibraryMediaFilter = .all
    @State private var selectedPhoto: LocalPhoto?
    @State private var isLimitedPickerPresented = false

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 2), count: 3)

    var body: some View {
        Group {
            if hasPhotoAccess {
                photoGrid
            } else {
                accessPrompt
            }
        }
        .navigationTitle(AppStrings.library)
        .toolbar {
            if hasPhotoAccess {
                ToolbarItem(placement: .topBarLeading) {
                    libraryMenu
                }
                if photoLibrary.authorizationStatus == .limited {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button(AppStrings.chooseMorePhotos) {
                            isLimitedPickerPresented = true
                        }
                    }
                }
            }
        }
        .task {
            await refreshAccessAndLoad()
        }
        .onChange(of: scenePhase) { _, phase in
            guard phase == .active else { return }
            Task { await refreshAccessAndLoad() }
        }
        .onChange(of: sortOrder) { _, _ in loadPhotos() }
        .onChange(of: mediaFilter) { _, _ in loadPhotos() }
        .sheet(item: $selectedPhoto, onDismiss: loadPhotos) { photo in
            NavigationStack {
                PhotoDetailView(
                    photos: photos,
                    initialPhoto: photo,
                    photoLibrary: photoLibrary,
                    onDelete: { deletedPhoto in
                        photos.removeAll { $0.id == deletedPhoto.id }
                    }
                )
            }
        }
        .background {
            LimitedLibraryPickerBridge(isPresented: $isLimitedPickerPresented) {
                Task { await refreshAccessAndLoad() }
            }
            .frame(width: 0, height: 0)
        }
    }

    private var hasPhotoAccess: Bool {
        photoLibrary.authorizationStatus == .authorized
            || photoLibrary.authorizationStatus == .limited
    }

    private var accessPrompt: some View {
        VStack(spacing: 16) {
            Image(systemName: "photo.on.rectangle.angled")
                .font(.system(size: 44))
                .foregroundStyle(.secondary)
            Text(AppStrings.photoAccessExplanation)
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
            if photoLibrary.authorizationStatus == .notDetermined {
                Button(AppStrings.allowPhotoAccess) {
                    Task {
                        _ = await photoLibrary.requestAuthorization()
                        await refreshAccessAndLoad()
                    }
                }
                .buttonStyle(.borderedProminent)
            } else {
                Text(AppStrings.photoAccessDenied)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.secondary)
                Button(AppStrings.openSystemSettings) {
                    if let url = URL(string: UIApplication.openSettingsURLString) {
                        openURL(url)
                    }
                }
                .buttonStyle(.bordered)
            }
        }
        .padding(32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var photoGrid: some View {
        ScrollView {
            if photos.isEmpty {
                ContentUnavailableView(
                    AppStrings.noPhotos,
                    systemImage: "photo.on.rectangle.angled",
                    description: Text(AppStrings.noPhotosDescription)
                )
                .frame(maxWidth: .infinity, minHeight: 300)
            } else {
                LazyVGrid(columns: columns, spacing: 2) {
                    ForEach(photos) { photo in
                        Button {
                            selectedPhoto = photo
                        } label: {
                            PhotoThumbnailView(
                                photo: photo,
                                photoLibrary: photoLibrary,
                                targetSize: CGSize(width: 360 * displayScale, height: 360 * displayScale)
                            )
                            .aspectRatio(1, contentMode: .fit)
                            .clipped()
                            .overlay(alignment: .bottomTrailing) {
                                if photo.mediaType == .video {
                                    Image(systemName: "video.fill")
                                        .font(.caption)
                                        .foregroundStyle(.white)
                                        .padding(6)
                                }
                            }
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(photo.creationDate?.formatted(date: .abbreviated, time: .shortened) ?? AppStrings.photo)
                    }
                }
                .padding(.horizontal, 2)
            }
        }
    }

    private var libraryMenu: some View {
        Menu {
            Menu(AppStrings.sort) {
                ForEach(LibrarySortOrder.allCases) { order in
                    Button {
                        sortOrder = order
                    } label: {
                        if sortOrder == order {
                            Label(order.title, systemImage: "checkmark")
                        } else {
                            Text(order.title)
                        }
                    }
                }
            }
            Menu(AppStrings.filter) {
                ForEach(LibraryMediaFilter.allCases) { filter in
                    Button {
                        mediaFilter = filter
                    } label: {
                        if mediaFilter == filter {
                            Label(filter.title, systemImage: "checkmark")
                        } else {
                            Text(filter.title)
                        }
                    }
                }
            }
        } label: {
            Image(systemName: "line.3.horizontal.decrease")
        }
        .accessibilityLabel(AppStrings.sortAndFilter)
    }

    @MainActor
    private func refreshAccessAndLoad() async {
        photoLibrary.refreshAuthorizationStatus()
        guard hasPhotoAccess else {
            photos = []
            return
        }
        loadPhotos()
    }

    private func loadPhotos() {
        guard hasPhotoAccess else { return }
        photos = photoLibrary.fetchLibraryPhotos(sortedBy: sortOrder, filteredBy: mediaFilter)
    }
}

private struct PhotoThumbnailView: View {
    let photo: LocalPhoto
    let photoLibrary: PhotoLibraryService
    let targetSize: CGSize

    @State private var image: UIImage?
    @State private var requestID: PHImageRequestID?

    var body: some View {
        Group {
            if let image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
            } else {
                Rectangle()
                    .fill(.quaternary)
                    .overlay {
                        ProgressView()
                    }
            }
        }
        .onAppear {
            photoLibrary.startCachingThumbnail(for: photo, targetSize: targetSize)
            requestID = photoLibrary.requestThumbnail(for: photo, targetSize: targetSize) { result in
                Task { @MainActor in image = result }
            }
        }
        .onDisappear {
            if let requestID { photoLibrary.cancelImageRequest(requestID) }
            photoLibrary.stopCachingThumbnail(for: photo, targetSize: targetSize)
        }
    }
}
