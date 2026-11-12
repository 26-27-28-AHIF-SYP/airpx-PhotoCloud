import BackgroundTasks
import Foundation
import Observation
import PhotoCloudSync

enum BackendConfig {
    static let uploadSessionIdentifier = "com.example.airpix.photo-upload"
    static let backgroundTaskIdentifier = "com.example.airpix.photo-sync"
    static let autoBackupDefaultsKey = "airpx.autoBackup.enabled"
}

@MainActor
enum BackgroundSessionEvents {
    static var completionHandler: (() -> Void)?
}

struct UploadContext: Codable, Sendable {
    let localIdentifier: String
    let modificationDate: Date?
    let temporaryFilePath: String?
    let multipartFilePath: String?
}

private final class UploadSessionDelegate: NSObject, URLSessionTaskDelegate, @unchecked Sendable {
    private let completion: @Sendable (String?, Int?, Error?) -> Void

    init(completion: @escaping @Sendable (String?, Int?, Error?) -> Void) { self.completion = completion }

    func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        completion(task.taskDescription, (task.response as? HTTPURLResponse)?.statusCode, error)
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
final class UploadQueue: PhotoSyncAPIClient {
    private(set) var lastError: String?
    private(set) var serverPhotoCount = 0

    @ObservationIgnored private let store: SyncStore
    @ObservationIgnored private let photoLibrary: PhotoLibraryService
    @ObservationIgnored private let serverSession: ServerSession
    @ObservationIgnored private var delegate: UploadSessionDelegate?
    @ObservationIgnored private var session: URLSession?
    @ObservationIgnored private var uploadContinuations: [String: CheckedContinuation<Void, Error>] = [:]

    init(store: SyncStore, photoLibrary: PhotoLibraryService, serverSession: ServerSession) {
        self.store = store
        self.photoLibrary = photoLibrary
        self.serverSession = serverSession
        let delegate = UploadSessionDelegate { [weak self] taskDescription, statusCode, error in
            Task { @MainActor [weak self] in
                await self?.completeUpload(taskDescription: taskDescription, statusCode: statusCode, error: error)
            }
        }
        self.delegate = delegate
        let configuration = URLSessionConfiguration.background(withIdentifier: BackendConfig.uploadSessionIdentifier)
        configuration.sessionSendsLaunchEvents = true
        configuration.isDiscretionary = false
        session = URLSession(configuration: configuration, delegate: delegate, delegateQueue: nil)
    }

    func registerBackgroundProcessingTask() {
        let registered = BGTaskScheduler.shared.register(forTaskWithIdentifier: BackendConfig.backgroundTaskIdentifier, using: nil) { [weak self] task in
            guard let task = task as? BGProcessingTask else {
                task.setTaskCompleted(success: false)
                return
            }
            Task { @MainActor [weak self] in await self?.handleBackgroundTask(task) }
        }
        if !registered { lastError = "Der Hintergrund-Sync konnte nicht registriert werden." }
    }

    func syncNow(manual: Bool = false) async throws {
        guard manual || UserDefaults.standard.bool(forKey: BackendConfig.autoBackupDefaultsKey) else { return }
        guard photoLibrary.authorizationStatus == .authorized || photoLibrary.authorizationStatus == .limited else {
            throw UploadQueueError.photoAccessRequired
        }
        let credentials = try serverSession.syncCredentials()
        try await store.load()
        for photo in photoLibrary.fetchVisiblePhotos() { try store.enqueue(photo) }

        let api: any PhotoSyncAPIClient = serverSession.isUsingMockAPI ? serverSession.serverAPIClient() : self
        let useCase = PhotoSyncUseCase(client: api, hasher: CryptoKitPhotoHasher(), cache: store)
        let summary = try await useCase.synchronize(
            assets: photoLibrary.fetchSyncAssets(),
            baseURL: credentials.baseURL,
            token: credentials.token,
            originalFile: { asset in try await photoLibrary.writeOriginalResource(for: asset.localIdentifier) },
            onUpdate: { [store] asset, state, _ in
                await MainActor.run {
                    let context = UploadContext(localIdentifier: asset.localIdentifier, modificationDate: asset.modificationDate,
                        temporaryFilePath: nil, multipartFilePath: nil)
                    do {
                        switch state {
                        case .uploading: try store.markUploading(context)
                        case .alreadyOnServer, .uploaded: try store.markDone(context, serverPhotoId: nil)
                        case .failed(let message): try store.markFailed(context, error: PhotoSyncFailure(message: message))
                        }
                    } catch { }
                }
            }
        )
        serverPhotoCount = summary.serverPhotoCount
        lastError = summary.failed == 0 ? nil : "\(summary.failed) Fotos konnten nicht synchronisiert werden."
        try scheduleBackgroundProcessing()
    }

    func enqueueChangedPhotos(_ photos: [LocalPhoto]) async {
        guard !photos.isEmpty else { return }
        do { try await syncNow() }
        catch { lastError = error.localizedDescription }
    }

    func pauseAutoBackup() {
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: BackendConfig.backgroundTaskIdentifier)
        session?.getAllTasks { $0.forEach { $0.cancel() } }
    }

    func missingHashes(baseURL: URL, token: String, hashes: [String]) async throws -> Set<String> {
        try await serverSession.serverAPIClient().missingHashes(baseURL: baseURL, token: token, hashes: hashes)
    }

    func listPhotos(baseURL: URL, token: String) async throws -> [PhotoServerRecord] {
        try await serverSession.serverAPIClient().listPhotos(baseURL: baseURL, token: token)
    }

    func uploadPhoto(baseURL: URL, token: String, asset: PhotoSyncAsset, fileURL: URL, metadata: PhotoUploadMetadata) async throws {
        guard let session else { throw UploadQueueError.sessionUnavailable }
        let boundary = "AirPx-\(UUID().uuidString)"
        let bodyURL = try MultipartPhotoUpload.makeBody(fileURL: fileURL, metadata: metadata, boundary: boundary)
        guard let url = URL(string: "/api/photos", relativeTo: baseURL)?.absoluteURL else {
            try? FileManager.default.removeItem(at: bodyURL)
            throw UploadQueueError.invalidEndpoint
        }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        let requestID = UUID().uuidString
        let context = UploadContext(localIdentifier: asset.localIdentifier, modificationDate: asset.modificationDate,
            temporaryFilePath: fileURL.path, multipartFilePath: bodyURL.path)
        let encodedContext = try JSONEncoder().encode(context).base64EncodedString()
        let task = session.uploadTask(with: request, fromFile: bodyURL)
        task.taskDescription = "\(requestID)|\(encodedContext)"
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            uploadContinuations[requestID] = continuation
            task.resume()
        }
    }

    private func completeUpload(taskDescription: String?, statusCode: Int?, error: Error?) async {
        guard let taskDescription, let separator = taskDescription.firstIndex(of: "|") else {
            lastError = "Ein Upload wurde ohne gültigen Kontext abgeschlossen."
            return
        }
        let requestID = String(taskDescription[..<separator])
        let encodedContext = String(taskDescription[taskDescription.index(after: separator)...])
        guard let data = Data(base64Encoded: encodedContext), let context = try? JSONDecoder().decode(UploadContext.self, from: data) else {
            uploadContinuations.removeValue(forKey: requestID)?.resume(throwing: UploadQueueError.invalidContext)
            lastError = "Upload-Metadaten konnten nicht gelesen werden."
            return
        }

        let hasWaiter = uploadContinuations[requestID] != nil
        let failure: Error?
        if let error { failure = error }
        else if let statusCode, (200..<300).contains(statusCode) { failure = nil }
        else { failure = UploadQueueError.httpFailure(statusCode) }
        do {
            if let failure {
                if !hasWaiter { try store.markFailed(context, error: failure) }
                lastError = failure.localizedDescription
            } else {
                if !hasWaiter { try store.markDone(context, serverPhotoId: nil) }
                lastError = nil
            }
        } catch { lastError = error.localizedDescription }
        if let path = context.multipartFilePath { try? FileManager.default.removeItem(atPath: path) }
        if let path = context.temporaryFilePath { try? FileManager.default.removeItem(atPath: path) }
        if let continuation = uploadContinuations.removeValue(forKey: requestID) {
            if let failure { continuation.resume(throwing: failure) }
            else { continuation.resume() }
        }
        try? scheduleBackgroundProcessing()
    }

    private func scheduleBackgroundProcessing() throws {
        guard UserDefaults.standard.bool(forKey: BackendConfig.autoBackupDefaultsKey) else {
            BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: BackendConfig.backgroundTaskIdentifier)
            return
        }
        let request = BGProcessingTaskRequest(identifier: BackendConfig.backgroundTaskIdentifier)
        request.requiresNetworkConnectivity = true
        request.earliestBeginDate = max(Date.now.addingTimeInterval(15 * 60), store.nextRetryDate ?? .distantPast)
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

private struct PhotoSyncFailure: Error, LocalizedError {
    let message: String
    init(message: String) { self.message = message }
    var errorDescription: String? { message }
}

private enum UploadQueueError: LocalizedError {
    case photoAccessRequired
    case sessionUnavailable
    case invalidEndpoint
    case invalidContext
    case httpFailure(Int?)

    var errorDescription: String? {
        switch self {
        case .photoAccessRequired: AppStrings.photoAccessRequiredForBackup
        case .sessionUnavailable: "Der Hintergrund-Upload ist nicht verfügbar."
        case .invalidEndpoint: AppStrings.invalidServerURL
        case .invalidContext: "Upload-Metadaten konnten nicht gelesen werden."
        case .httpFailure(let status): "Upload fehlgeschlagen (HTTP \(status.map { String($0) } ?? "keine Antwort"))."
        }
    }
}
