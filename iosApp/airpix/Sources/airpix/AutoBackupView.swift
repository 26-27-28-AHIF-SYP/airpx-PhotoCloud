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
                .accessibilityHint("Deine Fotos werden erst gesichert, wenn du Auto-Backup aktivierst.")
            } footer: {
                Text("Deine Fotos werden nur hochgeladen, wenn Auto-Backup aktiviert ist.")
            }

            Section("Fotozugriff") {
                Text(permissionDescription)
                if photoLibrary.authorizationStatus == .notDetermined {
                    Text(AppStrings.photoAccessRequiredForBackup)
                        .foregroundStyle(.secondary)
                }
                if photoLibrary.authorizationStatus == .limited {
                    Button("Weitere Fotos auswählen …") {
                        isLimitedPickerPresented = true
                    }
                } else if photoLibrary.authorizationStatus == .denied
                            || photoLibrary.authorizationStatus == .restricted {
                    Text("Erlaube AirPx in den Einstellungen den Zugriff auf Fotos.")
                        .foregroundStyle(.secondary)
                    Button("Einstellungen öffnen") {
                        if let settingsURL = URL(string: UIApplication.openSettingsURLString) {
                            openURL(settingsURL)
                        }
                    }
                }
            }

            Section("Synchronisierungsstatus") {
                LabeledContent("Ausstehend", value: "\(store.pendingCount)")
                LabeledContent("Hochgeladen", value: "\(store.uploadedCount)")
                LabeledContent("Fehlgeschlagen", value: "\(store.failedCount)")
                LabeledContent("Fotos auf dem Server", value: "\(uploadQueue.serverPhotoCount)")
                Button {
                    Task { await syncNow() }
                } label: {
                    if isSyncing {
                        ProgressView("Synchronisierung läuft …")
                    } else {
                        Text("Jetzt synchronisieren")
                    }
                }
                .disabled(isSyncing)
            }

            if let errorMessage {
                Section(AppStrings.error) {
                    Text(errorMessage)
                        .foregroundStyle(.red)
                }
            } else if let queueError = uploadQueue.lastError {
                Section(AppStrings.error) {
                    Text(queueError)
                        .foregroundStyle(.red)
                }
            }
        }
        .navigationTitle("Auto-Backup")
        .background {
            if isLimitedPickerPresented {
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
        }
        .task {
            do {
                try await store.load()
                photoLibrary.refreshAuthorizationStatus()
                if isEnabled {
                    guard photoLibrary.authorizationStatus == .authorized
                            || photoLibrary.authorizationStatus == .limited else {
                        await setAutoBackupEnabled(false)
                        errorMessage = AppStrings.photoAccessRequiredForBackup
                        return
                    }
                    startObservingChanges()
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
            "Voller Zugriff auf die Fotomediathek"
        case .limited:
            "Eingeschränkter Zugriff – nur ausgewählte Fotos werden gesichert"
        case .denied:
            "Zugriff verweigert"
        case .restricted:
            "Zugriff eingeschränkt"
        case .notDetermined:
            "Zugriff wurde noch nicht angefragt"
        @unknown default:
            "Unbekannter Berechtigungsstatus"
        }
    }

    @MainActor
    private func setAutoBackupEnabled(_ enabled: Bool) async {
        guard enabled else {
            isEnabled = false
            UserDefaults.standard.set(false, forKey: BackendConfig.autoBackupDefaultsKey)
            photoLibrary.stopObservingChanges()
            uploadQueue.pauseAutoBackup()
            return
        }

        photoLibrary.refreshAuthorizationStatus()
        guard photoLibrary.authorizationStatus == .authorized
                || photoLibrary.authorizationStatus == .limited else {
            isEnabled = false
            errorMessage = AppStrings.photoAccessRequiredForBackup
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
            photoLibrary.refreshAuthorizationStatus()
            guard photoLibrary.authorizationStatus == .authorized
                    || photoLibrary.authorizationStatus == .limited else {
                errorMessage = AppStrings.photoAccessRequiredForBackup
                return
            }
            try await uploadQueue.syncNow(manual: true)
            errorMessage = nil
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

struct LimitedLibraryPickerBridge: UIViewControllerRepresentable {
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
