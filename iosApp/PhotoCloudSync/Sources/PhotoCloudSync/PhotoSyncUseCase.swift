import Foundation

public struct PhotoSyncAsset: Equatable, Sendable {
    public let localIdentifier: String
    public let modificationDate: Date?
    public let capturedAt: Date?
    public let originalFilename: String
    public let mimeType: String

    public init(localIdentifier: String, modificationDate: Date?, capturedAt: Date?, originalFilename: String, mimeType: String) {
        self.localIdentifier = localIdentifier
        self.modificationDate = modificationDate
        self.capturedAt = capturedAt
        self.originalFilename = originalFilename
        self.mimeType = mimeType
    }
}

public struct PhotoUploadMetadata: Codable, Equatable, Sendable {
    public let filename: String
    public let capturedAt: Date?
    public let sha256: String
    public let size: Int64
    public let mimeType: String
    public let albumId: String?

    public init(filename: String, capturedAt: Date?, sha256: String, size: Int64, mimeType: String, albumId: String? = nil) {
        self.filename = filename
        self.capturedAt = capturedAt
        self.sha256 = sha256
        self.size = size
        self.mimeType = mimeType
        self.albumId = albumId
    }
}

public struct PhotoServerRecord: Codable, Equatable, Sendable, Identifiable {
    public let id: Int64
    public let sha256: String
    public let filename: String
    public let capturedAt: Date?
    public let size: Int64
    public let mimeType: String

    public init(id: Int64, sha256: String, filename: String, capturedAt: Date?, size: Int64, mimeType: String) {
        self.id = id
        self.sha256 = sha256
        self.filename = filename
        self.capturedAt = capturedAt
        self.size = size
        self.mimeType = mimeType
    }
}

public protocol PhotoSyncAPIClient: Sendable {
    func missingHashes(baseURL: URL, token: String, hashes: [String]) async throws -> Set<String>
    func uploadPhoto(baseURL: URL, token: String, asset: PhotoSyncAsset, fileURL: URL, metadata: PhotoUploadMetadata) async throws
    func listPhotos(baseURL: URL, token: String) async throws -> [PhotoServerRecord]
}

public protocol PhotoHashCache: Sendable {
    func cachedHash(localIdentifier: String, modificationDate: Date?) async -> String?
    func saveHash(_ hash: String, localIdentifier: String, modificationDate: Date?) async throws
}

public protocol PhotoFileHasher: Sendable {
    func sha256(fileURL: URL) async throws -> String
}

public enum PhotoSyncItemState: Equatable, Sendable {
    case alreadyOnServer
    case uploading
    case uploaded
    case failed(String)
}

public struct PhotoSyncSummary: Equatable, Sendable {
    public let uploaded: Int
    public let pending: Int
    public let failed: Int
    public let serverPhotoCount: Int

    public init(uploaded: Int, pending: Int, failed: Int, serverPhotoCount: Int) {
        self.uploaded = uploaded
        self.pending = pending
        self.failed = failed
        self.serverPhotoCount = serverPhotoCount
    }
}

public struct PhotoSyncUseCase: Sendable {
    private let client: any PhotoSyncAPIClient
    private let hasher: any PhotoFileHasher
    private let cache: any PhotoHashCache

    public init(client: any PhotoSyncAPIClient, hasher: any PhotoFileHasher, cache: any PhotoHashCache) {
        self.client = client
        self.hasher = hasher
        self.cache = cache
    }

    public func synchronize(
        assets: [PhotoSyncAsset],
        baseURL: URL,
        token: String,
        originalFile: @Sendable (PhotoSyncAsset) async throws -> URL,
        onUpdate: @Sendable (PhotoSyncAsset, PhotoSyncItemState, String?) async -> Void = { _, _, _ in }
    ) async throws -> PhotoSyncSummary {
        var hashes: [String: String] = [:]
        var files: [String: URL] = [:]
        var failed = Set<String>()
        defer { files.values.forEach { try? FileManager.default.removeItem(at: $0) } }

        for asset in assets {
            do {
                if let hash = await cache.cachedHash(localIdentifier: asset.localIdentifier, modificationDate: asset.modificationDate) {
                    hashes[asset.localIdentifier] = hash
                } else {
                    let url = try await originalFile(asset)
                    files[asset.localIdentifier] = url
                    let hash = try await hasher.sha256(fileURL: url)
                    try await cache.saveHash(hash, localIdentifier: asset.localIdentifier, modificationDate: asset.modificationDate)
                    hashes[asset.localIdentifier] = hash
                }
            } catch {
                failed.insert(asset.localIdentifier)
                await onUpdate(asset, .failed(error.localizedDescription), nil)
            }
        }

        let allHashes = Array(Set(hashes.values))
        var missing = Set<String>()
        for batch in allHashes.chunked(into: 500) {
            missing.formUnion(try await client.missingHashes(baseURL: baseURL, token: token, hashes: batch))
        }

        var uploaded = 0
        var uploadedHashes = Set<String>()
        for asset in assets where !failed.contains(asset.localIdentifier) {
            guard let hash = hashes[asset.localIdentifier] else { continue }
            guard missing.contains(hash), !uploadedHashes.contains(hash) else {
                await onUpdate(asset, .alreadyOnServer, hash)
                continue
            }
            do {
                await onUpdate(asset, .uploading, hash)
                let file: URL
                if let existingFile = files[asset.localIdentifier] {
                    file = existingFile
                } else {
                    file = try await originalFile(asset)
                    files[asset.localIdentifier] = file
                }
                let sizeValue = try FileManager.default.attributesOfItem(atPath: file.path)[.size] as? NSNumber
                let size = sizeValue?.int64Value ?? 0
                let metadata = PhotoUploadMetadata(filename: asset.originalFilename, capturedAt: asset.capturedAt,
                    sha256: hash, size: size, mimeType: asset.mimeType)
                try await client.uploadPhoto(baseURL: baseURL, token: token, asset: asset, fileURL: file, metadata: metadata)
                uploaded += 1
                uploadedHashes.insert(hash)
                await onUpdate(asset, .uploaded, hash)
            } catch {
                failed.insert(asset.localIdentifier)
                await onUpdate(asset, .failed(error.localizedDescription), hash)
            }
        }

        let serverPhotos = try await client.listPhotos(baseURL: baseURL, token: token)
        return PhotoSyncSummary(uploaded: uploaded, pending: failed.count, failed: failed.count, serverPhotoCount: serverPhotos.count)
    }
}

private extension Array {
    func chunked(into size: Int) -> [[Element]] {
        guard size > 0 else { return [self] }
        return stride(from: 0, to: count, by: size).map { Array(self[$0..<Swift.min($0 + size, count)]) }
    }
}
