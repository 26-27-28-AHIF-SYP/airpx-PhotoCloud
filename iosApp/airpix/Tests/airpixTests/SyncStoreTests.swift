import Foundation
import XCTest
@testable import airpix

final class SyncStoreTests: XCTestCase {
    @MainActor
    func testCachedHashRequiresMatchingModificationDate() async throws {
        let modificationDate = Date(timeIntervalSince1970: 100)
        let (directory, fileURL) = try makeStateFile(records: [
            makeRecord(modificationDate: modificationDate, sha256: "cached-hash"),
        ])
        defer { try? FileManager.default.removeItem(at: directory) }
        let store = SyncStore(fileURL: fileURL)
        try await store.load()

        let matchingHash = await store.cachedHash(localIdentifier: "photo", modificationDate: modificationDate)
        let changedHash = await store.cachedHash(
            localIdentifier: "photo",
            modificationDate: modificationDate.addingTimeInterval(1)
        )

        XCTAssertEqual(matchingHash, "cached-hash")
        XCTAssertNil(changedHash)
    }

    @MainActor
    func testFailedRecordRespectsRetryDelayAndPersists() async throws {
        let (directory, fileURL) = try makeStateFile(records: [
            makeRecord(modificationDate: Date(timeIntervalSince1970: 100)),
        ])
        defer { try? FileManager.default.removeItem(at: directory) }
        let store = SyncStore(fileURL: fileURL)
        try await store.load()
        let now = Date()

        let context = UploadContext(
            localIdentifier: "photo",
            modificationDate: Date(timeIntervalSince1970: 100),
            temporaryFilePath: nil,
            multipartFilePath: nil
        )
        try store.markFailed(context, error: TestFailure.upload)

        let failedRecord = try XCTUnwrap(store.records["photo"])
        let firstRetryAfter = try XCTUnwrap(failedRecord.retryAfter)
        XCTAssertEqual(failedRecord.status, .failed)
        XCTAssertEqual(failedRecord.retryCount, 1)
        XCTAssertTrue(store.dueRecords(at: now.addingTimeInterval(30)).isEmpty)
        XCTAssertEqual(store.dueRecords(at: firstRetryAfter).map(\.localIdentifier), ["photo"])

        let secondFailureAt = Date()
        try store.markFailed(context, error: TestFailure.upload)
        let retriedRecord = try XCTUnwrap(store.records["photo"])
        let retryAfter = try XCTUnwrap(retriedRecord.retryAfter)
        let retryDelay = retryAfter.timeIntervalSince(secondFailureAt)
        XCTAssertEqual(retriedRecord.retryCount, 2)
        XCTAssertGreaterThanOrEqual(retryDelay, 119)
        XCTAssertLessThan(retryDelay, 121)
        XCTAssertTrue(store.dueRecords(at: retryAfter.addingTimeInterval(-1)).isEmpty)
        XCTAssertEqual(store.dueRecords(at: retryAfter).map(\.localIdentifier), ["photo"])
        XCTAssertEqual(store.nextRetryDate, retryAfter)

        let reloadedStore = SyncStore(fileURL: fileURL)
        try await reloadedStore.load()
        XCTAssertEqual(reloadedStore.records["photo"]?.status, .failed)
        XCTAssertEqual(reloadedStore.records["photo"]?.retryCount, 2)
    }

    @MainActor
    func testStaleUploadContextCannotCompleteChangedPhoto() async throws {
        let currentDate = Date(timeIntervalSince1970: 200)
        let (directory, fileURL) = try makeStateFile(records: [
            makeRecord(modificationDate: currentDate),
        ])
        defer { try? FileManager.default.removeItem(at: directory) }
        let store = SyncStore(fileURL: fileURL)
        try await store.load()

        try store.markDone(
            UploadContext(
                localIdentifier: "photo",
                modificationDate: currentDate.addingTimeInterval(-1),
                temporaryFilePath: nil,
                multipartFilePath: nil
            ),
            serverPhotoId: "stale-server-id"
        )

        XCTAssertEqual(store.records["photo"]?.status, .pending)
        XCTAssertEqual(store.records["photo"]?.modificationDate, currentDate)
        XCTAssertNil(store.records["photo"]?.serverPhotoId)
    }
}

private enum TestFailure: Error {
    case upload
}

private func makeRecord(
    modificationDate: Date,
    sha256: String? = nil,
    status: UploadStatus = .pending,
    retryCount: Int = 0,
    retryAfter: Date? = nil
) -> SyncRecord {
    SyncRecord(
        localIdentifier: "photo",
        modificationDate: modificationDate,
        uploadedAt: nil,
        serverPhotoId: nil,
        sha256: sha256,
        status: status,
        retryCount: retryCount,
        retryAfter: retryAfter,
        failureMessage: nil
    )
}

private func makeStateFile(records: [SyncRecord]) throws -> (directory: URL, fileURL: URL) {
    let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
    try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
    let fileURL = directory.appendingPathComponent("sync-state.json")
    try JSONEncoder().encode(records).write(to: fileURL)
    return (directory, fileURL)
}
