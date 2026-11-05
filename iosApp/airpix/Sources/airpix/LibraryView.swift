import SwiftUI

struct LibraryView: View {
    let photoLibrary: PhotoLibraryService

    @Environment(\.scenePhase) private var scenePhase
    @State private var photos: [LocalPhoto] = []
    @State private var sortOrder: LibrarySortOrder = .captureDate
    @State private var mediaFilter: LibraryMediaFilter = .all
    @State private var isLimitedPickerPresented = false

    var body: some View {
        Group {
            if hasPhotoAccess {
                PhotoGridView(photos: photos, photoLibrary: photoLibrary) { deletedPhoto in
                    photos.removeAll { $0.id == deletedPhoto.id }
                } onDismiss: {
                    loadPhotos()
                }
            } else {
                PhotoAccessPromptView(photoLibrary: photoLibrary)
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
        .onChange(of: photoLibrary.authorizationStatus) { _, status in
            if status == .authorized || status == .limited {
                loadPhotos()
            } else {
                photos = []
            }
        }
        .onChange(of: scenePhase) { _, phase in
            guard phase == .active else { return }
            Task { await refreshAccessAndLoad() }
        }
        .onChange(of: sortOrder) { _, _ in loadPhotos() }
        .onChange(of: mediaFilter) { _, _ in loadPhotos() }
        .background {
            if isLimitedPickerPresented {
                LimitedLibraryPickerBridge(isPresented: $isLimitedPickerPresented) {
                    Task { await refreshAccessAndLoad() }
                }
                .frame(width: 0, height: 0)
            }
        }
    }

    private var hasPhotoAccess: Bool {
        photoLibrary.authorizationStatus == .authorized
            || photoLibrary.authorizationStatus == .limited
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
