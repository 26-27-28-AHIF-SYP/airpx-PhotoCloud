import SwiftUI

struct RootView: View {
    let store: SyncStore
    let photoLibrary: PhotoLibraryService
    let uploadQueue: UploadQueue
    let serverSession: ServerSession

    @State private var isSettingsPresented = false

    var body: some View {
        TabView {
            Tab(AppStrings.library, systemImage: "photo.on.rectangle.angled") {
                NavigationStack {
                    LibraryView(photoLibrary: photoLibrary)
                        .toolbar {
                            settingsButton
                        }
                }
            }

            Tab(AppStrings.collections, systemImage: "square.stack") {
                NavigationStack {
                    CollectionsView(photoLibrary: photoLibrary)
                        .toolbar {
                            settingsButton
                        }
                }
            }

            Tab(AppStrings.search, systemImage: "magnifyingglass", role: .search) {
                NavigationStack {
                    SearchView(photoLibrary: photoLibrary)
                        .toolbar {
                            settingsButton
                        }
                }
            }
        }
        .sheet(isPresented: $isSettingsPresented) {
            SettingsSheetView(
                store: store,
                photoLibrary: photoLibrary,
                uploadQueue: uploadQueue,
                serverSession: serverSession
            )
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
            .interactiveDismissDisabled(false)
        }
    }

    private var settingsButton: some ToolbarContent {
        ToolbarItem(placement: .topBarTrailing) {
            Button {
                isSettingsPresented = true
            } label: {
                Image(systemName: "gearshape")
            }
            .accessibilityLabel(AppStrings.settings)
        }
    }
}

private struct SettingsSheetView: View {
    let store: SyncStore
    let photoLibrary: PhotoLibraryService
    let uploadQueue: UploadQueue
    let serverSession: ServerSession

    @Environment(\.dismiss) private var dismiss
    @State private var versionTapCount = 0
    @State private var showDeveloper = false

    var body: some View {
        NavigationStack {
            List {
                Section {
                    NavigationLink(AppStrings.serverConnection) {
                        ServerConnectionView(session: serverSession)
                    }
                    NavigationLink("Auto-Backup") {
                        AutoBackupView(store: store, photoLibrary: photoLibrary, uploadQueue: uploadQueue)
                    }
                }

                #if DEBUG
                if showDeveloper {
                    Section(AppStrings.developer) {
                        Picker(AppStrings.serverImplementation, selection: Binding(
                            get: { serverSession.selectedImplementation },
                            set: { serverSession.setImplementation($0) }
                        )) {
                            Text(AppStrings.live).tag("live")
                            Text(AppStrings.mock).tag("mock")
                        }
                        if serverSession.selectedImplementation == "mock" {
                            Picker(AppStrings.simulate, selection: Binding(
                                get: { serverSession.mockFailureMode },
                                set: { serverSession.setMockFailureMode($0) }
                            )) {
                                ForEach(MockFailureMode.allCases) { mode in
                                    Text(mode.title).tag(mode)
                                }
                            }
                            Text(AppStrings.mockCredentials)
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
                #endif

                Section {
                    #if DEBUG
                    Button(AppStrings.appVersion) {
                        versionTapCount += 1
                        if versionTapCount >= 5 { showDeveloper = true }
                    }
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity)
                    .buttonStyle(.plain)
                    #else
                    Text(AppStrings.appVersion)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity)
                    #endif
                }
            }
            .navigationTitle(AppStrings.settings)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(AppStrings.done) {
                        dismiss()
                    }
                }
            }
        }
    }
}
