import BackgroundTasks
import Foundation
import Observation

enum BackendConfig {
    static let baseURL = "https://airpx.example.invalid"
    static let uploadPath = "/api/photos/upload" // TODO: Replace when the backend upload contract is defined.
    static let uploadSessionIdentifier = "com.example.airpix.photo-upload"
    static let backgroundTaskIdentifier = "com.example.airpix.photo-sync"
    static let autoBackupDefaultsKey = "airpx.autoBackup.enabled"

    static func photoUploadURL() throws -> URL {
        guard let baseURL = URL(string: baseURL),
              let url = URL(string: uploadPath, relativeTo: baseURL)?.absoluteURL else {
            throw UploadQueueError.invalidEndpoint
        }
        return url
    }
}

@MainActor
enum BackgroundSessionEvents {
    static var completionHandler: (() -> Void)?
}

struct UploadContext: Codable, Sendable {
    let localIdentifier: String
    let modificationDate: Date?
    var temporaryFilePath: String?
}

private final class UploadSessionDelegate: NSObject, URLSessionTaskDelegate, @unchecked Sendable {
    private let completion: @Sendable (String?, Int?, Error?) -> Void

    init(completion: @escaping @Sendable (String?, Int?, Error?) -> Void) {
        self.completion = completion
    }

    func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        let responseCode = (task.response as? HTTPURLResponse)?.statusCode
        completion(task.taskDescription, responseCode, error)
    }

    func urlSessionDidFinishEvents(forBackgroundURLSession session: URLSession) {
        Task { @MainActor in
            let completionHandler = BackgroundSessionEvents.completionHandler
            BackgroundSessionEvents.completionHandler = nil
            completionHandler?()
        }
    }
}

@Observable
@MainActor
final class UploadQueue {
    private(set) var lastError: String?

    @ObservationIgnored private let store: SyncStore
    @ObservationIgnored private let photoLibrary: PhotoLibraryService
    @ObservationIgnored private var delegate: UploadSessionDelegate?
    @ObservationIgnored private var session: URLSession?

    init(store: SyncStore, photoLibrary: PhotoLibraryService) {
        self.store = store
        self.photoLibrary = photoLibrary

        let delegate = UploadSessionDelegate { [weak self] taskDescription, statusCode, error in
            Task { @MainActor [weak self] in
                await self?.completeUpload(
                    taskDescription: taskDescription,
                    statusCode: statusCode,
                    error: error
                )
            }
        }
        self.delegate = delegate
        let configuration = URLSessionConfiguration.background(
            withIdentifier: BackendConfig.uploadSessionIdentifier
        )
        configuration.sessionSendsLaunchEvents = true
        configuration.isDiscretionary = false
        session = URLSession(configuration: configuration, delegate: delegate, delegateQueue: nil)
    }

    func registerBackgroundProcessingTask() {
        let registered = BGTaskScheduler.shared.register(
            forTaskWithIdentifier: BackendConfig.backgroundTaskIdentifier,
            using: nil
        ) { [weak self] task in
            guard let task = task as? BGProcessingTask else {
                task.setTaskCompleted(success: false)
                return
            }
            Task { @MainActor [weak self] in
                await self?.handleBackgroundTask(task)
            }
        }
        if !registered {
            lastError = "Could not register the background sync task."
        }
    }

    func syncNow(manual: Bool = false) async throws {
        guard manual || UserDefaults.standard.bool(forKey: BackendConfig.autoBackupDefaultsKey) else {
            return
        }
        guard photoLibrary.authorizationStatus == .authorized
                || photoLibrary.authorizationStatus == .limited else {
            throw UploadQueueError.photoAccessRequired
        }

        try store.load()
        for photo in photoLibrary.fetchVisiblePhotos() {
            try store.enqueue(photo)
        }
        try await startPendingUploads()
        try scheduleBackgroundProcessing()
    }

    func enqueueChangedPhotos(_ photos: [LocalPhoto]) async {
        do {
            try store.load()
            for photo in photos {
                try store.enqueue(photo)
            }
            try await startPendingUploads()
            try scheduleBackgroundProcessing()
        } catch {
            lastError = error.localizedDescription
        }
    }

    func pauseAutoBackup() {
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: BackendConfig.backgroundTaskIdentifier)
        session?.getAllTasks { tasks in
            tasks.forEach { $0.cancel() }
        }
    }

    private func startPendingUploads() async throws {
        guard let session else { throw UploadQueueError.sessionUnavailable }

        for record in store.dueRecords() {
            let context = UploadContext(
                localIdentifier: record.localIdentifier,
                modificationDate: record.modificationDate,
                temporaryFilePath: nil
            )
            try store.markUploading(context)

            var temporaryFileURL: URL?
            do {
                let fileURL = try await photoLibrary.writeOriginalResource(
                    for: record.localIdentifier
                )
                temporaryFileURL = fileURL
                var request = URLRequest(url: try BackendConfig.photoUploadURL())
                request.httpMethod = "POST"
                request.setValue("application/octet-stream", forHTTPHeaderField: "Content-Type")
                let task = session.uploadTask(with: request, fromFile: fileURL)
                let uploadContext = UploadContext(
                    localIdentifier: context.localIdentifier,
                    modificationDate: context.modificationDate,
                    temporaryFilePath: fileURL.path
                )
                task.taskDescription = try JSONEncoder().encode(uploadContext).base64EncodedString()
                task.resume()
            } catch let uploadError {
                var cleanupError: Error?
                if let temporaryFileURL, FileManager.default.fileExists(atPath: temporaryFileURL.path) {
                    do {
                        try FileManager.default.removeItem(at: temporaryFileURL)
                    } catch {
                        cleanupError = error
                    }
                }
                do {
                    try store.markFailed(context, error: uploadError)
                } catch {
                    lastError = error.localizedDescription
                    continue
                }
                lastError = (cleanupError ?? uploadError).localizedDescription
            }
        }
    }

    private func completeUpload(
        taskDescription: String?,
        statusCode: Int?,
        error: Error?
    ) async {
        guard let taskDescription,
              let contextData = Data(base64Encoded: taskDescription) else {
            lastError = "An upload completed without valid photo metadata."
            return
        }

        let context: UploadContext
        do {
            context = try JSONDecoder().decode(UploadContext.self, from: contextData)
        } catch {
            lastError = error.localizedDescription
            return
        }

        do {
            if let error {
                try store.markFailed(context, error: error)
                lastError = error.localizedDescription
            } else if let statusCode, (200..<300).contains(statusCode) {
                // The server response schema is intentionally not assumed; wire serverPhotoId once defined.
                try store.markDone(context, serverPhotoId: nil)
                lastError = nil
            } else {
                let failure = UploadQueueError.httpFailure(statusCode)
                try store.markFailed(context, error: failure)
                lastError = failure.localizedDescription
            }
            try scheduleBackgroundProcessing()
        } catch {
            lastError = error.localizedDescription
        }

        if let path = context.temporaryFilePath, FileManager.default.fileExists(atPath: path) {
            do {
                try FileManager.default.removeItem(atPath: path)
            } catch {
                lastError = error.localizedDescription
            }
        }
    }

    private func scheduleBackgroundProcessing() throws {
        guard UserDefaults.standard.bool(forKey: BackendConfig.autoBackupDefaultsKey) else {
            BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: BackendConfig.backgroundTaskIdentifier)
            return
        }

        let request = BGProcessingTaskRequest(identifier: BackendConfig.backgroundTaskIdentifier)
        request.requiresNetworkConnectivity = true
        request.earliestBeginDate = max(
            Date.now.addingTimeInterval(15 * 60),
            store.nextRetryDate ?? .distantPast
        )
        try BGTaskScheduler.shared.submit(request)
    }

    private func handleBackgroundTask(_ task: BGProcessingTask) async {
        guard UserDefaults.standard.bool(forKey: BackendConfig.autoBackupDefaultsKey) else {
            task.setTaskCompleted(success: true)
            return
        }

        let operation = Task { try await syncNow() }
        task.expirationHandler = { operation.cancel() }
        do {
            try await operation.value
            task.setTaskCompleted(success: true)
        } catch {
            lastError = error.localizedDescription
            task.setTaskCompleted(success: false)
        }
    }
}

private enum UploadQueueError: LocalizedError {
    case photoAccessRequired
    case sessionUnavailable
    case invalidEndpoint
    case httpFailure(Int?)

    var errorDescription: String? {
        switch self {
        case .photoAccessRequired:
            "Photo library access is required before syncing."
        case .sessionUnavailable:
            "The background upload session is unavailable."
        case .invalidEndpoint:
            "The configured photo upload URL is invalid."
        case .httpFailure(let statusCode):
            if let statusCode {
                "The photo upload failed with HTTP status \(statusCode)."
            } else {
                "The photo upload returned no HTTP response."
            }
        }
    }
}
