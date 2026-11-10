import SwiftUI

struct CollectionsView: View {
    let photoLibrary: PhotoLibraryService

    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.displayScale) private var displayScale
    @State private var albums: [LocalAlbum] = []
    @State private var favorites: [LocalPhoto] = []

    private let columns = [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)]

    var body: some View {
        Group {
            if hasPhotoAccess {
                ScrollView {
                    LazyVGrid(columns: columns, spacing: 12) {
                        NavigationLink {
                            AlbumsView(photoLibrary: photoLibrary)
                        } label: {
                            CollectionTile(
                                title: AppStrings.albums,
                                subtitle: "\(albums.count) \(AppStrings.albumCount)",
                                symbol: "rectangle.stack",
                                cover: albums.first?.keyPhoto,
                                photoLibrary: photoLibrary,
                                targetSize: CGSize(width: 600 * displayScale, height: 420 * displayScale)
                            )
                        }

                        NavigationLink {
                            FavoritesView(photoLibrary: photoLibrary)
                        } label: {
                            CollectionTile(
                                title: AppStrings.favorites,
                                subtitle: "\(favorites.count) \(AppStrings.favoriteCount)",
                                symbol: "heart",
                                cover: favorites.first,
                                photoLibrary: photoLibrary,
                                targetSize: CGSize(width: 600 * displayScale, height: 420 * displayScale)
                            )
                        }

                        NavigationLink {
                            PeoplePetsPlaceholderView()
                        } label: {
                            CollectionTile(
                                title: AppStrings.peopleAndPets,
                                subtitle: AppStrings.comingSoon,
                                symbol: "person.2.crop.square.stack",
                                cover: nil,
                                photoLibrary: photoLibrary,
                                targetSize: CGSize(width: 600 * displayScale, height: 420 * displayScale)
                            )
                        }
                    }
                    .padding()
                }
            } else {
                PhotoAccessPromptView(photoLibrary: photoLibrary)
            }
        }
        .navigationTitle(AppStrings.collections)
        .task { refreshCollections() }
        .onChange(of: photoLibrary.authorizationStatus) { _, _ in refreshCollections() }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { refreshCollections() }
        }
    }

    private var hasPhotoAccess: Bool {
        photoLibrary.authorizationStatus == .authorized
            || photoLibrary.authorizationStatus == .limited
    }

    private func refreshCollections() {
        photoLibrary.refreshAuthorizationStatus()
        guard hasPhotoAccess else {
            albums = []
            favorites = []
            return
        }
        albums = photoLibrary.fetchAlbums()
        favorites = photoLibrary.fetchFavoritePhotos()
    }
}

private struct CollectionTile: View {
    let title: String
    let subtitle: String
    let symbol: String
    let cover: LocalPhoto?
    let photoLibrary: PhotoLibraryService
    let targetSize: CGSize

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ZStack {
                if let cover {
                    PhotoThumbnailView(photo: cover, photoLibrary: photoLibrary, targetSize: targetSize)
                        .frame(maxWidth: .infinity)
                        .frame(height: 126)
                        .clipped()
                } else {
                    Rectangle()
                        .fill(.quaternary)
                        .overlay {
                            Image(systemName: symbol)
                                .font(.largeTitle)
                                .foregroundStyle(.secondary)
                        }
                        .frame(height: 126)
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: 14))

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.headline)
                    .foregroundStyle(.primary)
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .padding(.horizontal, 2)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .contentShape(Rectangle())
    }
}

private struct FavoritesView: View {
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
        .navigationTitle(AppStrings.favorites)
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
        photos = hasPhotoAccess ? photoLibrary.fetchFavoritePhotos() : []
    }
}

private struct PeoplePetsPlaceholderView: View {
    var body: some View {
        ContentUnavailableView(
            AppStrings.peopleAndPets,
            systemImage: "person.2.crop.square.stack",
            description: Text(AppStrings.comingSoon)
        )
        .navigationTitle(AppStrings.peopleAndPets)
    }
}
