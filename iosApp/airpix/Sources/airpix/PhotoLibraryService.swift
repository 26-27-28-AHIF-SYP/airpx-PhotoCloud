import Foundation
import Observation
@preconcurrency import Photos

struct PhotoLocation: Codable, Equatable, Sendable {
    let latitude: Double
    let longitude: Double
}

struct LocalPhoto: Equatable, Identifiable, Sendable {
    var id: String { localIdentifier }

    let localIdentifier: String
    let creationDate: Date?
    let modificationDate: Date?
    let location: PhotoLocation?
    let pixelWidth: Int
    let pixelHeight: Int

    init(asset: PHAsset) {
        localIdentifier = asset.localIdentifier
        creationDate = asset.creationDate
        modificationDate = asset.modificationDate
        if let coordinate = asset.location?.coordinate {
            location = PhotoLocation(latitude: coordinate.latitude, longitude: coordinate.longitude)
        } else {
            location = nil
        }
        pixelWidth = asset.pixelWidth
        pixelHeight = asset.pixelHeight
    }
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
