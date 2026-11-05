import Foundation
import Observation

enum UploadStatus: String, Codable, Sendable {
    case pending
    case uploading
    case done
    case failed
}

struct SyncRecord: Codable, Identifiable, Sendable {
    var id: String { localIdentifier }

    let localIdentifier: String
    var modificationDate: Date?
    var uploadedAt: Date?
    var serverPhotoId: String?
    var status: UploadStatus
    var retryCount: Int
    var retryAfter: Date?
    var failureMessage: String?
}

@Observable
@MainActor
final class SyncStore {
    private(set) var records: [String: SyncRecord] = [:]
    @ObservationIgnored private let fileURL: URL
    @ObservationIgnored private var isLoaded = false
    @ObservationIgnored private var loadingTask: Task<[String: SyncRecord], Error>?

    var pendingCount: Int {
        records.values.filter { $0.status != .done }.count
    }

    var uploadedCount: Int {
        records.values.filter { $0.status == .done }.count
    }

    init(fileURL: URL = URL.applicationSupportDirectory.appending(path: "AirPx/sync-state.json")) {
        self.fileURL = fileURL
    }

    func load() async throws {
        guard !isLoaded else { return }

        let task: Task<[String: SyncRecord], Error>
        if let loadingTask {
            task = loadingTask
        } else {
            let fileURL = fileURL
            task = Task.detached(priority: .utility) {
                guard FileManager.default.fileExists(atPath: fileURL.path) else { return [:] }
                let data = try Data(contentsOf: fileURL)
                let savedRecords = try JSONDecoder().decode([SyncRecord].self, from: data)
                return savedRecords.reduce(into: [:]) { result, record in
                    result[record.localIdentifier] = record
                }
            }
            loadingTask = task
        }

        do {
            records = try await task.value
            isLoaded = true
            loadingTask = nil
        } catch {
            loadingTask = nil
            throw error
        }
    }

    @discardableResult
    func enqueue(_ photo: LocalPhoto) throws -> Bool {
        if let existing = records[photo.localIdentifier],
           existing.modificationDate == photo.modificationDate {
            return false
        }

        let existing = records[photo.localIdentifier]
        records[photo.localIdentifier] = SyncRecord(
            localIdentifier: photo.localIdentifier,
            modificationDate: photo.modificationDate,
            uploadedAt: nil,
            serverPhotoId: existing?.serverPhotoId,
            status: .pending,
            retryCount: 0,
            retryAfter: nil,
            failureMessage: nil
        )
        try persist()
        return true
    }

    func dueRecords(at date: Date = .now) -> [SyncRecord] {
        records.values
            .filter { record in
                record.status == .pending
                    || (record.status == .failed && (record.retryAfter ?? .distantPast) <= date)
            }
            .sorted { $0.localIdentifier < $1.localIdentifier }
    }

    var nextRetryDate: Date? {
        records.values
            .filter { $0.status == .failed }
            .compactMap(\.retryAfter)
            .min()
    }

    func markUploading(_ context: UploadContext) throws {
        guard var record = records[context.localIdentifier],
              record.modificationDate == context.modificationDate else { return }
        record.status = .uploading
        record.failureMessage = nil
        records[context.localIdentifier] = record
        try persist()
    }

    func markDone(_ context: UploadContext, serverPhotoId: String?) throws {
        guard var record = records[context.localIdentifier],
              record.modificationDate == context.modificationDate else { return }
        record.status = .done
        record.uploadedAt = .now
        record.serverPhotoId = serverPhotoId ?? record.serverPhotoId
        record.retryAfter = nil
        record.failureMessage = nil
        records[context.localIdentifier] = record
        try persist()
    }

    func markFailed(_ context: UploadContext, error: Error) throws {
        guard var record = records[context.localIdentifier],
              record.modificationDate == context.modificationDate else { return }
        record.status = .failed
        record.retryCount += 1
        let delay = min(60 * pow(2, Double(record.retryCount - 1)), 86_400)
        record.retryAfter = .now.addingTimeInterval(delay)
        record.failureMessage = error.localizedDescription
        records[context.localIdentifier] = record
        try persist()
    }

    private func persist() throws {
        let directory = fileURL.deletingLastPathComponent()
        try FileManager.default.createDirectory(
            at: directory,
            withIntermediateDirectories: true
        )
        let data = try JSONEncoder().encode(Array(records.values))
        try data.write(to: fileURL, options: .atomic)
    }
}
