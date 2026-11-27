import Foundation
import XCTest
@testable import PhotoCloudSync

private actor FakeCache: PhotoHashCache {
    private var values: [String: String]
    private(set) var saves = 0

    init(values: [String: String] = [:]) { self.values = values }
    func cachedHash(localIdentifier: String, modificationDate: Date?) async -> String? { values[localIdentifier] }
    func saveHash(_ hash: String, localIdentifier: String, modificationDate: Date?) async throws {
        values[localIdentifier] = hash
        saves += 1
    }
}

private actor FakeHasher: PhotoFileHasher {
    private let failingFiles: Set<String>
    private(set) var calls = 0

    init(failingFiles: Set<String> = []) { self.failingFiles = failingFiles }

    func sha256(fileURL: URL) async throws -> String {
        calls += 1
        if failingFiles.contains(fileURL.lastPathComponent) { throw FakeError.hashing }
        return fileURL.lastPathComponent
    }
}

private actor FakeAPI: PhotoSyncAPIClient {
    private(set) var checked: [[String]] = []
    private(set) var uploads: [PhotoUploadMetadata] = []
    private(set) var uploadAttempts: [String] = []
    let missing: Set<String>
    let failingUploads: Set<String>
    let failMissingHashes: Bool

    init(missing: Set<String>, failingUploads: Set<String> = [], failMissingHashes: Bool = false) {
        self.missing = missing
        self.failingUploads = failingUploads
        self.failMissingHashes = failMissingHashes
    }

    func missingHashes(baseURL: URL, token: String, hashes: [String]) async throws -> Set<String> {
        checked.append(hashes)
        if failMissingHashes { throw FakeError.missingHashes }
        return Set(hashes).intersection(missing)
    }

    func uploadPhoto(baseURL: URL, token: String, asset: PhotoSyncAsset, fileURL: URL, metadata: PhotoUploadMetadata) async throws {
        uploadAttempts.append(metadata.sha256)
        if failingUploads.contains(metadata.sha256) { throw FakeError.upload }
        uploads.append(metadata)
    }

    func listPhotos(baseURL: URL, token: String) async throws -> [PhotoServerRecord] {
        [PhotoServerRecord(id: 1, sha256: "known", filename: "known.jpg", capturedAt: nil, size: 1, mimeType: "image/jpeg")]
    }
}

private enum FakeError: Error, Equatable {
    case hashing
    case upload
    case missingHashes
}

final class PhotoSyncUseCaseTests: XCTestCase {
    func testHashesUncachedAssetChecksMissingHashesAndUploadsOnlyMissing() async throws {
        let cached = FakeCache(values: ["unchanged": "already-stored", "duplicate": "new-content"])
        let hasher = FakeHasher()
        let api = FakeAPI(missing: ["new-content"])
        let useCase = PhotoSyncUseCase(client: api, hasher: hasher, cache: cached)
        let assets = [
            PhotoSyncAsset(localIdentifier: "unchanged", modificationDate: nil, capturedAt: nil, originalFilename: "old.jpg", mimeType: "image/jpeg"),
            PhotoSyncAsset(localIdentifier: "changed", modificationDate: Date(timeIntervalSince1970: 5), capturedAt: nil, originalFilename: "new.jpg", mimeType: "image/jpeg"),
            PhotoSyncAsset(localIdentifier: "duplicate", modificationDate: nil, capturedAt: nil, originalFilename: "copy.jpg", mimeType: "image/jpeg"),
        ]
        let newFile = FileManager.default.temporaryDirectory.appendingPathComponent("new-content")
        try Data("photo".utf8).write(to: newFile)
        defer { try? FileManager.default.removeItem(at: newFile) }
        let summary = try await useCase.synchronize(assets: assets, baseURL: URL(string: "https://photos.example")!, token: "token") { _ in newFile }
        let hashCalls = await hasher.calls
        let hashSaves = await cached.saves
        let checkedHashes = await api.checked.flatMap { $0 }.sorted()
        let uploadedFiles = await api.uploads.map(\.filename)
        XCTAssertEqual(hashCalls, 1)
        XCTAssertEqual(hashSaves, 1)
        XCTAssertEqual(checkedHashes, ["already-stored", "new-content"])
        XCTAssertEqual(uploadedFiles, ["new.jpg"])
        XCTAssertEqual(summary.uploaded, 1)
        XCTAssertEqual(summary.failed, 0)
        XCTAssertEqual(summary.serverPhotoCount, 1)
    }

    func testHashFailureDoesNotPreventOtherAssetsFromSyncing() async throws {
        let cached = FakeCache()
        let hasher = FakeHasher(failingFiles: ["broken.jpg"])
        let api = FakeAPI(missing: ["good.jpg"])
        let useCase = PhotoSyncUseCase(client: api, hasher: hasher, cache: cached)
        let assets = [makeAsset("broken"), makeAsset("good")]
        let directory = try makeTemporaryDirectory()
        defer { try? FileManager.default.removeItem(at: directory) }
        for asset in assets {
            try Data(asset.localIdentifier.utf8).write(to: directory.appendingPathComponent("\(asset.localIdentifier).jpg"))
        }

        let summary = try await useCase.synchronize(
            assets: assets,
            baseURL: URL(string: "https://photos.example")!,
            token: "token"
        ) { asset in
            directory.appendingPathComponent("\(asset.localIdentifier).jpg")
        }

        let uploads = await api.uploads.map(\.filename)
        XCTAssertEqual(uploads, ["good.jpg"])
        XCTAssertEqual(summary.uploaded, 1)
        XCTAssertEqual(summary.failed, 1)
    }

    func testUploadFailureDoesNotPreventOtherAssetsFromUploading() async throws {
        let cached = FakeCache(values: ["first": "first-hash", "second": "second-hash"])
        let api = FakeAPI(missing: ["first-hash", "second-hash"], failingUploads: ["first-hash"])
        let useCase = PhotoSyncUseCase(client: api, hasher: FakeHasher(), cache: cached)
        let assets = [makeAsset("first"), makeAsset("second")]
        let directory = try makeTemporaryDirectory()
        defer { try? FileManager.default.removeItem(at: directory) }
        for asset in assets {
            try Data(asset.localIdentifier.utf8).write(to: directory.appendingPathComponent(asset.localIdentifier))
        }

        let summary = try await useCase.synchronize(
            assets: assets,
            baseURL: URL(string: "https://photos.example")!,
            token: "token"
        ) { asset in
            directory.appendingPathComponent(asset.localIdentifier)
        }

        let attempts = await api.uploadAttempts.sorted()
        let uploads = await api.uploads.map(\.filename)
        XCTAssertEqual(attempts, ["first-hash", "second-hash"])
        XCTAssertEqual(uploads, ["second.jpg"])
        XCTAssertEqual(summary.uploaded, 1)
        XCTAssertEqual(summary.failed, 1)
    }

    func testMissingHashesAreRequestedInBatchesOf500() async throws {
        let assets = (0..<501).map { makeAsset("photo-\($0)") }
        let cache = FakeCache(values: Dictionary(uniqueKeysWithValues: assets.map { ($0.localIdentifier, $0.localIdentifier) }))
        let api = FakeAPI(missing: [])
        let hasher = FakeHasher()
        let useCase = PhotoSyncUseCase(client: api, hasher: hasher, cache: cache)

        _ = try await useCase.synchronize(
            assets: assets,
            baseURL: URL(string: "https://photos.example")!,
            token: "token"
        ) { asset in
            FileManager.default.temporaryDirectory.appendingPathComponent(asset.localIdentifier)
        }

        let batchSizes = await api.checked.map(\.count).sorted()
        let hashCalls = await hasher.calls
        XCTAssertEqual(batchSizes, [1, 500])
        XCTAssertEqual(hashCalls, 0)
    }

    func testMissingHashesAPIErrorIsPropagated() async throws {
        let cache = FakeCache(values: ["photo": "photo-hash"])
        let api = FakeAPI(missing: [], failMissingHashes: true)
        let useCase = PhotoSyncUseCase(client: api, hasher: FakeHasher(), cache: cache)

        do {
            _ = try await useCase.synchronize(
                assets: [makeAsset("photo")],
                baseURL: URL(string: "https://photos.example")!,
                token: "token"
            ) { asset in
                FileManager.default.temporaryDirectory.appendingPathComponent(asset.localIdentifier)
            }
            XCTFail("Expected the missing-hashes API error to propagate")
        } catch {
            XCTAssertEqual(error as? FakeError, .missingHashes)
        }
    }
}

private func makeAsset(_ identifier: String) -> PhotoSyncAsset {
    PhotoSyncAsset(
        localIdentifier: identifier,
        modificationDate: nil,
        capturedAt: nil,
        originalFilename: "\(identifier).jpg",
        mimeType: "image/jpeg"
    )
}

private func makeTemporaryDirectory() throws -> URL {
    let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
    try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
    return directory
}
