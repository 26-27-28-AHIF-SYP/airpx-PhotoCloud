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
    private(set) var calls = 0
    func sha256(fileURL: URL) async throws -> String { calls += 1; return fileURL.lastPathComponent }
}

private actor FakeAPI: PhotoSyncAPIClient {
    private(set) var checked: [[String]] = []
    private(set) var uploads: [PhotoUploadMetadata] = []
    let missing: Set<String>
    init(missing: Set<String>) { self.missing = missing }
    func missingHashes(baseURL: URL, token: String, hashes: [String]) async throws -> Set<String> {
        checked.append(hashes)
        return Set(hashes).intersection(missing)
    }
    func uploadPhoto(baseURL: URL, token: String, asset: PhotoSyncAsset, fileURL: URL, metadata: PhotoUploadMetadata) async throws {
        uploads.append(metadata)
    }
    func listPhotos(baseURL: URL, token: String) async throws -> [PhotoServerRecord] {
        [PhotoServerRecord(id: 1, sha256: "known", filename: "known.jpg", capturedAt: nil, size: 1, mimeType: "image/jpeg")]
    }
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
}
