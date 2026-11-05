import SwiftUI

struct RootView: View {
    let store: SyncStore
    let photoLibrary: PhotoLibraryService
    let uploadQueue: UploadQueue

    @State private var isSettingsPresented = false

    var body: some View {
        TabView {
            Tab(AppStrings.library, systemImage: "photo.on.rectangle.angled") {
                NavigationStack {
                    ContentView()
                        .toolbar {
                            settingsButton
                        }
                }
            }

            Tab(AppStrings.collections, systemImage: "square.stack") {
                NavigationStack {
                    Text(AppStrings.collectionsPlaceholder)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .navigationTitle(AppStrings.collections)
                        .toolbar {
                            settingsButton
                        }
                }
            }

            Tab(AppStrings.search, systemImage: "magnifyingglass", role: .search) {
                NavigationStack {
                    Text(AppStrings.searchPlaceholder)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .navigationTitle(AppStrings.search)
                        .toolbar {
                            settingsButton
                        }
                }
            }
        }
        .sheet(isPresented: $isSettingsPresented) {
            NavigationStack {
                AutoBackupView(
                    store: store,
                    photoLibrary: photoLibrary,
                    uploadQueue: uploadQueue
                )
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button(AppStrings.close) {
                            isSettingsPresented = false
                        }
                    }
                }
            }
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
