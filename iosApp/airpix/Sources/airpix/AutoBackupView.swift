import Photos
import PhotosUI
import SwiftUI
import UIKit

struct AutoBackupView: View {
    let store: SyncStore
    let photoLibrary: PhotoLibraryService
    let uploadQueue: UploadQueue

    @State private var isEnabled = UserDefaults.standard.bool(
        forKey: BackendConfig.autoBackupDefaultsKey
    )
    @State private var isSyncing = false
    @State private var isLimitedPickerPresented = false
    @State private var errorMessage: String?
    @Environment(\.openURL) private var openURL
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        Form {
            Section {
                Toggle("Auto-Backup", isOn: Binding(
                    get: { isEnabled },
                    set: { newValue in
                        Task { await setAutoBackupEnabled(newValue) }
                    }
                ))
                .accessibilityHint("Photo backup is off until you turn it on.")
            } footer: {
                Text("Your camera roll is not uploaded unless you enable Auto-Backup.")
            }

            Section("Photo Library Access") {
                Text(permissionDescription)
                if photoLibrary.authorizationStatus == .limited {
                    Button("Choose More Photos…") {
                        isLimitedPickerPresented = true
                    }
                } else if photoLibrary.authorizationStatus == .denied
                            || photoLibrary.authorizationStatus == .restricted {
                    Text("Allow AirPx to access photos in Settings to use Auto-Backup.")
                        .foregroundStyle(.secondary)
                    Button("Open Settings") {
                        if let settingsURL = URL(string: UIApplication.openSettingsURLString) {
                            openURL(settingsURL)
                        }
                    }
                }
            }

            Section("Sync Status") {
                LabeledContent("Pending", value: "\(store.pendingCount)")
                LabeledContent("Uploaded", value: "\(store.uploadedCount)")
                Button {
                    Task { await syncNow() }
                } label: {
                    if isSyncing {
                        ProgressView("Syncing…")
                    } else {
                        Text("Sync now")
                    }
                }
                .disabled(isSyncing)
            }

            if let errorMessage {
                Section("Issue") {
                    Text(errorMessage)
                        .foregroundStyle(.red)
                }
            } else if let queueError = uploadQueue.lastError {
                Section("Issue") {
                    Text(queueError)
                        .foregroundStyle(.red)
                }
            }
        }
        .navigationTitle("Auto-Backup")
        .background {
            LimitedLibraryPickerBridge(isPresented: $isLimitedPickerPresented) {
                photoLibrary.refreshAuthorizationStatus()
                if isEnabled {
                    Task {
                        do {
                            try await uploadQueue.syncNow()
                            errorMessage = nil
                        } catch {
                            errorMessage = error.localizedDescription
                        }
                    }
                }
            }
            .frame(width: 0, height: 0)
        }
        .task {
            do {
                try store.load()
                photoLibrary.refreshAuthorizationStatus()
                if isEnabled {
                    await setAutoBackupEnabled(true, requestPermission: true)
                }
            } catch {
                errorMessage = error.localizedDescription
            }
        }
        .onChange(of: scenePhase) { _, phase in
            guard phase == .active else { return }
            photoLibrary.refreshAuthorizationStatus()
            guard isEnabled else { return }
            guard photoLibrary.authorizationStatus == .authorized
                    || photoLibrary.authorizationStatus == .limited else {
                Task { await setAutoBackupEnabled(false) }
                return
            }
            startObservingChanges()
            Task {
                do {
                    try await uploadQueue.syncNow()
                } catch {
                    errorMessage = error.localizedDescription
                }
            }
        }
    }

    private var permissionDescription: String {
        switch photoLibrary.authorizationStatus {
        case .authorized:
            "Full photo library access"
        case .limited:
            "Limited access — only selected photos are included"
        case .denied:
            "Access denied"
        case .restricted:
            "Access restricted"
        case .notDetermined:
            "Permission has not been requested"
        @unknown default:
            "Unknown permission status"
        }
    }

    @MainActor
    private func setAutoBackupEnabled(_ enabled: Bool, requestPermission: Bool = false) async {
        guard enabled else {
            isEnabled = false
            UserDefaults.standard.set(false, forKey: BackendConfig.autoBackupDefaultsKey)
            photoLibrary.stopObservingChanges()
            uploadQueue.pauseAutoBackup()
            return
        }

        let status: PHAuthorizationStatus
        if requestPermission || photoLibrary.authorizationStatus == .notDetermined {
            status = await photoLibrary.requestAuthorization()
        } else {
            photoLibrary.refreshAuthorizationStatus()
            status = photoLibrary.authorizationStatus
        }

        guard status == .authorized || status == .limited else {
            isEnabled = false
            UserDefaults.standard.set(false, forKey: BackendConfig.autoBackupDefaultsKey)
            photoLibrary.stopObservingChanges()
            uploadQueue.pauseAutoBackup()
            return
        }

        isEnabled = true
        UserDefaults.standard.set(true, forKey: BackendConfig.autoBackupDefaultsKey)
        startObservingChanges()
        do {
            try await uploadQueue.syncNow()
            errorMessage = nil
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    @MainActor
    private func startObservingChanges() {
        photoLibrary.startObservingChanges { [uploadQueue] changedPhotos in
            Task { await uploadQueue.enqueueChangedPhotos(changedPhotos) }
        }
    }

    @MainActor
    private func syncNow() async {
        isSyncing = true
        defer { isSyncing = false }

        do {
            if photoLibrary.authorizationStatus == .notDetermined {
                _ = await photoLibrary.requestAuthorization()
            } else {
                photoLibrary.refreshAuthorizationStatus()
            }
            try await uploadQueue.syncNow(manual: true)
            errorMessage = nil
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

private struct LimitedLibraryPickerBridge: UIViewControllerRepresentable {
    @Binding var isPresented: Bool
    let onCompletion: () -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func makeUIViewController(context: Context) -> UIViewController {
        UIViewController()
    }

    func updateUIViewController(_ controller: UIViewController, context: Context) {
        guard isPresented, !context.coordinator.isPresenting else { return }
        context.coordinator.isPresenting = true
        PHPhotoLibrary.shared().presentLimitedLibraryPicker(from: controller) { _ in
            Task { @MainActor in
                context.coordinator.isPresenting = false
                isPresented = false
                onCompletion()
            }
        }
    }

    @MainActor
    final class Coordinator {
        var isPresenting = false
    }
}
