import SwiftUI
import UIKit

@main
struct airpixApp: App {
    @UIApplicationDelegateAdaptor(AirPxAppDelegate.self) private var appDelegate
    private let store: SyncStore
    private let photoLibrary: PhotoLibraryService
    private let uploadQueue: UploadQueue
    private let serverSession: ServerSession

    init() {
        let store = SyncStore()
        let photoLibrary = PhotoLibraryService()
        let uploadQueue = UploadQueue(store: store, photoLibrary: photoLibrary)
        let serverSession = ServerSession()
        self.store = store
        self.photoLibrary = photoLibrary
        self.uploadQueue = uploadQueue
        self.serverSession = serverSession
        uploadQueue.registerBackgroundProcessingTask()
    }

    var body: some Scene {
        WindowGroup {
            RootView(
                store: store,
                photoLibrary: photoLibrary,
                uploadQueue: uploadQueue,
                serverSession: serverSession
            )
        }
    }
}

@MainActor
final class AirPxAppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        handleEventsForBackgroundURLSession identifier: String,
        completionHandler: @escaping () -> Void
    ) {
        guard identifier == BackendConfig.uploadSessionIdentifier else {
            completionHandler()
            return
        }
        BackgroundSessionEvents.completionHandler = completionHandler
    }
}
