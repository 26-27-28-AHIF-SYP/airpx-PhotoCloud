import Foundation
import Observation
@preconcurrency import Photos
import UIKit
import UniformTypeIdentifiers
import PhotoCloudSync

enum LibrarySortOrder: String, CaseIterable, Identifiable {
    case captureDate = "creationDate"
    case dateAdded = "addedDate"

    var id: Self { self }
    var title: String { self == .captureDate ? AppStrings.sortCaptureDate : AppStrings.sortDateAdded }
}

enum LibraryMediaFilter: CaseIterable, Equatable, Identifiable {
    case all
    case photos
    case videos

    var id: Self { self }
    var title: String {
        switch self {
        case .all: AppStrings.mediaAll
        case .photos: AppStrings.mediaPhotos
        case .videos: AppStrings.mediaVideos
        }
    }
}

enum LocalMediaType: Equatable, Sendable {
    case photo
    case video
}

struct PhotoLocation: Codable, Equatable, Sendable {
    let latitude: Double
    let longitude: Double
}

struct LocalPhoto: Equatable, Identifiable, Sendable {
    var id: String { localIdentifier }

    let localIdentifier: String
    let creationDate: Date?
    let addedDate: Date?
    let modificationDate: Date?
    let location: PhotoLocation?
    let pixelWidth: Int
    let pixelHeight: Int
    let mediaType: LocalMediaType
    let isFavorite: Bool

    init(asset: PHAsset) {
        localIdentifier = asset.localIdentifier
        creationDate = asset.creationDate
        addedDate = asset.addedDate
        modificationDate = asset.modificationDate
        if let coordinate = asset.location?.coordinate {
            location = PhotoLocation(latitude: coordinate.latitude, longitude: coordinate.longitude)
        } else {
            location = nil
        }
        pixelWidth = asset.pixelWidth
        pixelHeight = asset.pixelHeight
        mediaType = asset.mediaType == .video ? .video : .photo
        isFavorite = asset.isFavorite
    }
}

struct LocalAlbum: Equatable, Identifiable, Sendable {
    let id: String
    let title: String
    let assetCount: Int
    let keyPhoto: LocalPhoto?
}

enum PhotoLibraryError: LocalizedError {
    case assetUnavailable
    case originalResourceUnavailable

    var errorDescription: String? {
        switch self {
        case .assetUnavailable:
            "The photo is no longer available in the visible library."
        case .originalResourceUnavailable:
            "The photo's original resource could not be found."
        }
    }
}

@Observable
@MainActor
final class PhotoLibraryService: NSObject, PHPhotoLibraryChangeObserver {
    private(set) var authorizationStatus = PHPhotoLibrary.authorizationStatus(for: .readWrite)
    @ObservationIgnored private var observedAssets: PHFetchResult<PHAsset>?
    @ObservationIgnored private var changeHandler: (([LocalPhoto]) -> Void)?
    @ObservationIgnored private let imageManager = PHCachingImageManager()

    func refreshAuthorizationStatus() {
        authorizationStatus = PHPhotoLibrary.authorizationStatus(for: .readWrite)
    }

    func requestAuthorization() async -> PHAuthorizationStatus {
        authorizationStatus = await PHPhotoLibrary.requestAuthorization(for: .readWrite)
        return authorizationStatus
    }

    func fetchVisiblePhotos() -> [LocalPhoto] {
        let options = PHFetchOptions()
        options.sortDescriptors = [NSSortDescriptor(key: "creationDate", ascending: false)]
        let results = PHAsset.fetchAssets(with: .image, options: options)
        var photos: [LocalPhoto] = []
        photos.reserveCapacity(results.count)
        results.enumerateObjects { asset, _, _ in
            photos.append(LocalPhoto(asset: asset))
        }
        return photos
    }

    func fetchSyncAssets() -> [PhotoSyncAsset] {
        let results = PHAsset.fetchAssets(with: .image, options: nil)
        var assets: [PhotoSyncAsset] = []
        assets.reserveCapacity(results.count)
        results.enumerateObjects { asset, _, _ in
            let resources = PHAssetResource.assetResources(for: asset)
            guard let resource = resources.first(where: { $0.type == .fullSizePhoto })
                    ?? resources.first(where: { $0.type == .photo }) else { return }
            let fileExtension = URL(fileURLWithPath: resource.originalFilename).pathExtension
            let mimeType = UTType(filenameExtension: fileExtension)?.preferredMIMEType ?? "image/jpeg"
            assets.append(PhotoSyncAsset(
                localIdentifier: asset.localIdentifier,
                modificationDate: asset.modificationDate,
                capturedAt: asset.creationDate,
                originalFilename: resource.originalFilename,
                mimeType: mimeType
            ))
        }
        return assets
    }

    func fetchLibraryPhotos(
        sortedBy sortOrder: LibrarySortOrder,
        filteredBy filter: LibraryMediaFilter
    ) -> [LocalPhoto] {
        let options = PHFetchOptions()
        options.sortDescriptors = [NSSortDescriptor(key: sortOrder.rawValue, ascending: false)]
        switch filter {
        case .all:
            options.predicate = NSPredicate(
                format: "mediaType == %d OR mediaType == %d",
                PHAssetMediaType.image.rawValue,
                PHAssetMediaType.video.rawValue
            )
        case .photos:
            options.predicate = NSPredicate(format: "mediaType == %d", PHAssetMediaType.image.rawValue)
        case .videos:
            options.predicate = NSPredicate(format: "mediaType == %d", PHAssetMediaType.video.rawValue)
        }

        let results = PHAsset.fetchAssets(with: options)
        var photos: [LocalPhoto] = []
        photos.reserveCapacity(results.count)
        results.enumerateObjects { asset, _, _ in
            photos.append(LocalPhoto(asset: asset))
        }
        return photos
    }

    func fetchAlbums() -> [LocalAlbum] {
        let collections = PHAssetCollection.fetchAssetCollections(
            with: .album,
            subtype: .any,
            options: nil
        )
        var albums: [LocalAlbum] = []
        collections.enumerateObjects { [authorizationStatus] collection, _, _ in
            let options = PHFetchOptions()
            options.sortDescriptors = [NSSortDescriptor(key: "creationDate", ascending: false)]
            options.predicate = NSPredicate(
                format: "mediaType == %d OR mediaType == %d",
                PHAssetMediaType.image.rawValue,
                PHAssetMediaType.video.rawValue
            )
            let assets = PHAsset.fetchAssets(in: collection, options: options)
            if authorizationStatus == .limited && assets.count == 0 { return }
            let keyAsset = PHAsset.fetchKeyAssets(in: collection, options: options)?.firstObject
                ?? assets.firstObject
            albums.append(LocalAlbum(
                id: collection.localIdentifier,
                title: collection.localizedTitle ?? AppStrings.untitledAlbum,
                assetCount: assets.count,
                keyPhoto: keyAsset.map(LocalPhoto.init(asset:))
            ))
        }
        return albums.sorted { $0.title.localizedStandardCompare($1.title) == .orderedAscending }
    }

    func fetchAlbumPhotos(
        albumIdentifier: String,
        sortedBy sortOrder: LibrarySortOrder = .captureDate
    ) -> [LocalPhoto] {
        guard let collection = PHAssetCollection.fetchAssetCollections(
            withLocalIdentifiers: [albumIdentifier],
            options: nil
        ).firstObject else { return [] }
        let options = PHFetchOptions()
        options.sortDescriptors = [NSSortDescriptor(key: sortOrder.rawValue, ascending: false)]
        options.predicate = NSPredicate(
            format: "mediaType == %d OR mediaType == %d",
            PHAssetMediaType.image.rawValue,
            PHAssetMediaType.video.rawValue
        )
        return localPhotos(from: PHAsset.fetchAssets(in: collection, options: options))
    }

    func fetchFavoritePhotos(
        sortedBy sortOrder: LibrarySortOrder = .captureDate
    ) -> [LocalPhoto] {
        let options = PHFetchOptions()
        options.sortDescriptors = [NSSortDescriptor(key: sortOrder.rawValue, ascending: false)]
        options.predicate = NSPredicate(
            format: "isFavorite == YES AND (mediaType == %d OR mediaType == %d)",
            PHAssetMediaType.image.rawValue,
            PHAssetMediaType.video.rawValue
        )
        return localPhotos(from: PHAsset.fetchAssets(with: options))
    }

    func requestThumbnail(
        for photo: LocalPhoto,
        targetSize: CGSize,
        completion: @escaping (UIImage?) -> Void
    ) -> PHImageRequestID {
        guard let asset = fetchAsset(with: photo.localIdentifier) else {
            completion(nil)
            return PHInvalidImageRequestID
        }
        let options = PHImageRequestOptions()
        options.deliveryMode = .opportunistic
        options.resizeMode = .fast
        options.isNetworkAccessAllowed = true
        return imageManager.requestImage(
            for: asset,
            targetSize: targetSize,
            contentMode: .aspectFill,
            options: options
        ) { image, _ in
            completion(image)
        }
    }

    func requestDisplayImage(
        for photo: LocalPhoto,
        targetSize: CGSize,
        completion: @escaping (UIImage?) -> Void
    ) -> PHImageRequestID {
        guard let asset = fetchAsset(with: photo.localIdentifier) else {
            completion(nil)
            return PHInvalidImageRequestID
        }
        let options = PHImageRequestOptions()
        options.deliveryMode = .highQualityFormat
        options.resizeMode = .fast
        options.isNetworkAccessAllowed = true
        return imageManager.requestImage(
            for: asset,
            targetSize: targetSize,
            contentMode: .aspectFit,
            options: options
        ) { image, _ in
            completion(image)
        }
    }

    func cancelImageRequest(_ requestID: PHImageRequestID) {
        imageManager.cancelImageRequest(requestID)
    }

    func startCachingThumbnail(for photo: LocalPhoto, targetSize: CGSize) {
        guard let asset = fetchAsset(with: photo.localIdentifier) else { return }
        imageManager.startCachingImages(
            for: [asset],
            targetSize: targetSize,
            contentMode: .aspectFill,
            options: nil
        )
    }

    func stopCachingThumbnail(for photo: LocalPhoto, targetSize: CGSize) {
        guard let asset = fetchAsset(with: photo.localIdentifier) else { return }
        imageManager.stopCachingImages(
            for: [asset],
            targetSize: targetSize,
            contentMode: .aspectFill,
            options: nil
        )
    }

    func setFavorite(_ isFavorite: Bool, for photo: LocalPhoto) async throws {
        guard let asset = fetchAsset(with: photo.localIdentifier) else {
            throw PhotoLibraryError.assetUnavailable
        }
        try await PHPhotoLibrary.shared().performChanges {
            PHAssetChangeRequest(for: asset).isFavorite = isFavorite
        }
    }

    func delete(_ photo: LocalPhoto) async throws {
        guard let asset = fetchAsset(with: photo.localIdentifier) else {
            throw PhotoLibraryError.assetUnavailable
        }
        try await PHPhotoLibrary.shared().performChanges {
            PHAssetChangeRequest.deleteAssets([asset] as NSArray)
        }
    }

    func startObservingChanges(_ handler: @escaping ([LocalPhoto]) -> Void) {
        guard authorizationStatus == .authorized || authorizationStatus == .limited else { return }
        changeHandler = handler
        if observedAssets == nil {
            observedAssets = fetchAssets()
            PHPhotoLibrary.shared().register(self)
        }
    }

    func stopObservingChanges() {
        PHPhotoLibrary.shared().unregisterChangeObserver(self)
        observedAssets = nil
        changeHandler = nil
    }

    func writeOriginalResource(for localIdentifier: String) async throws -> URL {
        let assets = PHAsset.fetchAssets(withLocalIdentifiers: [localIdentifier], options: nil)
        guard let asset = assets.firstObject else {
            throw PhotoLibraryError.assetUnavailable
        }

        let resources = PHAssetResource.assetResources(for: asset)
        guard let resource = resources.first(where: { $0.type == .fullSizePhoto })
                ?? resources.first(where: { $0.type == .photo }) else {
            throw PhotoLibraryError.originalResourceUnavailable
        }

        let fileExtension = URL(fileURLWithPath: resource.originalFilename).pathExtension
        let fileName = fileExtension.isEmpty
            ? UUID().uuidString
            : "\(UUID().uuidString).\(fileExtension)"
        let destination = FileManager.default.temporaryDirectory.appendingPathComponent(fileName)
        let options = PHAssetResourceRequestOptions()
        options.isNetworkAccessAllowed = true

        return try await withCheckedThrowingContinuation { continuation in
            PHAssetResourceManager.default().writeData(
                for: resource,
                toFile: destination,
                options: options
            ) { error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: destination)
                }
            }
        }
    }

    nonisolated func photoLibraryDidChange(_ changeInstance: PHChange) {
        Task { @MainActor [weak self] in
            self?.applyChanges(changeInstance)
        }
    }

    private func fetchAssets() -> PHFetchResult<PHAsset> {
        let options = PHFetchOptions()
        options.sortDescriptors = [NSSortDescriptor(key: "creationDate", ascending: false)]
        return PHAsset.fetchAssets(with: .image, options: options)
    }

    private func fetchAsset(with localIdentifier: String) -> PHAsset? {
        PHAsset.fetchAssets(withLocalIdentifiers: [localIdentifier], options: nil).firstObject
    }

    private func localPhotos(from assets: PHFetchResult<PHAsset>) -> [LocalPhoto] {
        var photos: [LocalPhoto] = []
        photos.reserveCapacity(assets.count)
        assets.enumerateObjects { asset, _, _ in
            photos.append(LocalPhoto(asset: asset))
        }
        return photos
    }

    private func applyChanges(_ change: PHChange) {
        guard let observedAssets,
              let details = change.changeDetails(for: observedAssets) else { return }

        // PhotoKit's post-change fetch result is the authoritative visible set in limited mode.
        self.observedAssets = details.fetchResultAfterChanges
        let changedAssets = details.insertedObjects + details.changedObjects
        guard !changedAssets.isEmpty else { return }
        changeHandler?(changedAssets.map(LocalPhoto.init(asset:)))
    }
}
