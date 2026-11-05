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
                uploadQueue: uploadQueue
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

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            AutoBackupView(
                store: store,
                photoLibrary: photoLibrary,
                uploadQueue: uploadQueue
            )
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
