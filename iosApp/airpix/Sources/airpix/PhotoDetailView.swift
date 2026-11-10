import Photos
import SwiftUI
import UIKit

struct PhotoDetailView: View {
    let photos: [LocalPhoto]
    let initialPhoto: LocalPhoto
    let photoLibrary: PhotoLibraryService
    let onDelete: (LocalPhoto) -> Void

    @Environment(\.dismiss) private var dismiss
    @Environment(\.displayScale) private var displayScale
    @State private var selectedID: String
    @State private var detailImage: UIImage?
    @State private var isFavorite: Bool
    @State private var isInfoPresented = false
    @State private var isSharePresented = false
    @State private var isDeleteConfirmationPresented = false
    @State private var errorMessage: String?

    init(
        photos: [LocalPhoto],
        initialPhoto: LocalPhoto,
        photoLibrary: PhotoLibraryService,
        onDelete: @escaping (LocalPhoto) -> Void
    ) {
        self.photos = photos
        self.initialPhoto = initialPhoto
        self.photoLibrary = photoLibrary
        self.onDelete = onDelete
        _selectedID = State(initialValue: initialPhoto.id)
        _isFavorite = State(initialValue: initialPhoto.isFavorite)
    }

    private var selectedPhoto: LocalPhoto {
        photos.first(where: { $0.id == selectedID }) ?? initialPhoto
    }

    var body: some View {
        TabView(selection: $selectedID) {
            ForEach(photos) { photo in
                PhotoDetailImage(
                    photo: photo,
                    photoLibrary: photoLibrary,
                    targetSize: CGSize(width: 1600 * displayScale, height: 1600 * displayScale),
                    onImage: { image in
                        if selectedID == photo.id { detailImage = image }
                    }
                )
                .tag(photo.id)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .never))
        .background(.black)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button(AppStrings.close) { dismiss() }
            }
            ToolbarItemGroup(placement: .topBarTrailing) {
                if detailImage != nil {
                    Button {
                        isSharePresented = true
                    } label: {
                        Image(systemName: "square.and.arrow.up")
                    }
                    .accessibilityLabel(AppStrings.share)
                }
                Button {
                    Task { await toggleFavorite() }
                } label: {
                    Image(systemName: isFavorite ? "heart.fill" : "heart")
                }
                .accessibilityLabel(isFavorite ? AppStrings.removeFavorite : AppStrings.favorite)

                Button {
                    isInfoPresented = true
                } label: {
                    Image(systemName: "info.circle")
                }
                .accessibilityLabel(AppStrings.photoInfo)

                Button(role: .destructive) {
                    isDeleteConfirmationPresented = true
                } label: {
                    Image(systemName: "trash")
                }
                .accessibilityLabel(AppStrings.delete)
            }
        }
        .sheet(isPresented: $isInfoPresented) {
            NavigationStack {
                Form {
                    if let date = selectedPhoto.creationDate {
                        LabeledContent(AppStrings.sortCaptureDate, value: date.formatted(date: .long, time: .shortened))
                    }
                    LabeledContent(AppStrings.dimensions, value: "\(selectedPhoto.pixelWidth) × \(selectedPhoto.pixelHeight)")
                    if let location = selectedPhoto.location {
                        LabeledContent(AppStrings.coordinates, value: "\(location.latitude), \(location.longitude)")
                    }
                    if selectedPhoto.creationDate == nil && selectedPhoto.location == nil {
                        Text(AppStrings.noPhotoInfo)
                            .foregroundStyle(.secondary)
                    }
                }
                .navigationTitle(AppStrings.photoInfo)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button(AppStrings.close) { isInfoPresented = false }
                    }
                }
            }
            .presentationDetents([.medium, .large])
        }
        .sheet(isPresented: $isSharePresented) {
            if let detailImage {
                PhotoActivitySheet(image: detailImage)
            }
        }
        .alert(AppStrings.delete, isPresented: $isDeleteConfirmationPresented) {
            Button(AppStrings.cancel, role: .cancel) { }
            Button(AppStrings.delete, role: .destructive) {
                Task { await deleteSelectedPhoto() }
            }
        } message: {
            Text(AppStrings.deleteConfirmation)
        }
        .alert(AppStrings.error, isPresented: Binding(
            get: { errorMessage != nil },
            set: { if !$0 { errorMessage = nil } }
        )) {
            Button(AppStrings.close) { errorMessage = nil }
        } message: {
            Text(errorMessage ?? "")
        }
        .onChange(of: selectedID) { _, id in
            isFavorite = photos.first(where: { $0.id == id })?.isFavorite ?? false
            detailImage = nil
        }
    }

    @MainActor
    private func toggleFavorite() async {
        do {
            try await photoLibrary.setFavorite(!isFavorite, for: selectedPhoto)
            isFavorite.toggle()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    @MainActor
    private func deleteSelectedPhoto() async {
        do {
            try await photoLibrary.delete(selectedPhoto)
            onDelete(selectedPhoto)
            dismiss()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

private struct PhotoActivitySheet: UIViewControllerRepresentable {
    let image: UIImage

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: [image], applicationActivities: nil)
    }

    func updateUIViewController(_ controller: UIActivityViewController, context: Context) { }
}

private struct PhotoDetailImage: View {
    let photo: LocalPhoto
    let photoLibrary: PhotoLibraryService
    let targetSize: CGSize
    let onImage: (UIImage?) -> Void

    @State private var image: UIImage?
    @State private var requestID: PHImageRequestID?

    var body: some View {
        Group {
            if let image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFit()
            } else {
                ProgressView()
                    .tint(.white)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .onAppear {
            requestID = photoLibrary.requestDisplayImage(for: photo, targetSize: targetSize) { result in
                Task { @MainActor in
                    image = result
                    onImage(result)
                }
            }
        }
        .onDisappear {
            if let requestID { photoLibrary.cancelImageRequest(requestID) }
        }
    }
}
