import Photos
import SwiftUI
import UIKit

struct PhotoGridView: View {
    let photos: [LocalPhoto]
    let photoLibrary: PhotoLibraryService
    let onDelete: (LocalPhoto) -> Void
    let onDismiss: () -> Void

    @Environment(\.displayScale) private var displayScale
    @State private var selectedPhoto: LocalPhoto?

    private let minimumCellSize: CGFloat = 80
    private let spacing: CGFloat = 1

    var body: some View {
        GeometryReader { geometry in
            let gridWidth = geometry.size.width - spacing * 2
            let columnCount = max(1, Int((gridWidth + spacing) / (minimumCellSize + spacing)))
            let cellSize = (gridWidth - spacing * CGFloat(columnCount - 1)) / CGFloat(columnCount)
            let columns = Array(repeating: GridItem(.flexible(), spacing: spacing), count: columnCount)

            ScrollView {
                if photos.isEmpty {
                    ContentUnavailableView(
                        AppStrings.noPhotos,
                        systemImage: "photo.on.rectangle.angled",
                        description: Text(AppStrings.noPhotosDescription)
                    )
                    .frame(maxWidth: .infinity, minHeight: 300)
                } else {
                    LazyVGrid(columns: columns, spacing: spacing) {
                        ForEach(photos) { photo in
                            Button {
                                selectedPhoto = photo
                            } label: {
                                Color.clear
                                    .aspectRatio(1, contentMode: .fit)
                                    .overlay {
                                        PhotoThumbnailView(
                                            photo: photo,
                                            photoLibrary: photoLibrary,
                                            targetSize: CGSize(
                                                width: cellSize * displayScale,
                                                height: cellSize * displayScale
                                            )
                                        )
                                    }
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
                    .padding(.horizontal, spacing)
                }
            }
        }
        .sheet(item: $selectedPhoto, onDismiss: onDismiss) { photo in
            NavigationStack {
                PhotoDetailView(
                    photos: photos,
                    initialPhoto: photo,
                    photoLibrary: photoLibrary,
                    onDelete: onDelete
                )
            }
        }
    }
}

struct PhotoThumbnailView: View {
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
