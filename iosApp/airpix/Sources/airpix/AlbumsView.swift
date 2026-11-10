import SwiftUI

struct AlbumsView: View {
    let photoLibrary: PhotoLibraryService

    @Environment(\.displayScale) private var displayScale
    @State private var albums: [LocalAlbum] = []

    var body: some View {
        Group {
            if hasPhotoAccess {
                if albums.isEmpty {
                    ContentUnavailableView(
                        AppStrings.noAlbums,
                        systemImage: "rectangle.stack",
                        description: Text(AppStrings.noAlbumsDescription)
                    )
                } else {
                    List(albums) { album in
                        NavigationLink {
                            AlbumPhotosView(album: album, photoLibrary: photoLibrary)
                        } label: {
                            HStack(spacing: 12) {
                                if let cover = album.keyPhoto {
                                    PhotoThumbnailView(
                                        photo: cover,
                                        photoLibrary: photoLibrary,
                                        targetSize: CGSize(width: 180 * displayScale, height: 180 * displayScale)
                                    )
                                    .frame(width: 64, height: 64)
                                    .clipShape(RoundedRectangle(cornerRadius: 8))
                                } else {
                                    Image(systemName: "rectangle.stack")
                                        .font(.title2)
                                        .foregroundStyle(.secondary)
                                        .frame(width: 64, height: 64)
                                        .background(.quaternary, in: RoundedRectangle(cornerRadius: 8))
                                }
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(album.title)
                                        .foregroundStyle(.primary)
                                    Text("\(album.assetCount) \(AppStrings.albumPhotoCount)")
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                }
                            }
                        }
                    }
                    .listStyle(.plain)
                }
            } else {
                PhotoAccessPromptView(photoLibrary: photoLibrary)
            }
        }
        .navigationTitle(AppStrings.albums)
        .task { loadAlbums() }
        .onChange(of: photoLibrary.authorizationStatus) { _, _ in loadAlbums() }
    }

    private var hasPhotoAccess: Bool {
        photoLibrary.authorizationStatus == .authorized
            || photoLibrary.authorizationStatus == .limited
    }

    private func loadAlbums() {
        photoLibrary.refreshAuthorizationStatus()
        albums = hasPhotoAccess ? photoLibrary.fetchAlbums() : []
    }
}

private struct AlbumPhotosView: View {
    let album: LocalAlbum
    let photoLibrary: PhotoLibraryService

    @Environment(\.scenePhase) private var scenePhase
    @State private var photos: [LocalPhoto] = []
    @State private var isLimitedPickerPresented = false

    var body: some View {
        Group {
            if hasPhotoAccess {
                PhotoGridView(photos: photos, photoLibrary: photoLibrary) { deleted in
                    photos.removeAll { $0.id == deleted.id }
                } onDismiss: {
                    loadPhotos()
                }
            } else {
                PhotoAccessPromptView(photoLibrary: photoLibrary)
            }
        }
        .navigationTitle(album.title)
        .toolbar {
            if photoLibrary.authorizationStatus == .limited {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(AppStrings.chooseMorePhotos) { isLimitedPickerPresented = true }
                }
            }
        }
        .task { loadPhotos() }
        .onChange(of: photoLibrary.authorizationStatus) { _, _ in loadPhotos() }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { loadPhotos() }
        }
        .background {
            if isLimitedPickerPresented {
                LimitedLibraryPickerBridge(isPresented: $isLimitedPickerPresented) {
                    loadPhotos()
                }
                .frame(width: 0, height: 0)
            }
        }
    }

    private var hasPhotoAccess: Bool {
        photoLibrary.authorizationStatus == .authorized
            || photoLibrary.authorizationStatus == .limited
    }

    private func loadPhotos() {
        photoLibrary.refreshAuthorizationStatus()
        photos = hasPhotoAccess
            ? photoLibrary.fetchAlbumPhotos(albumIdentifier: album.id)
            : []
    }
}
