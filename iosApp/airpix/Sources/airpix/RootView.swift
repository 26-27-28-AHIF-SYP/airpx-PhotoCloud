import SwiftUI

private enum TabLabel {
    static let menu = "Menü"
    static let settings = "Einstellungen"
    static let dashboard = "Dashboard"
}

struct RootView: View {
    let store: SyncStore
    let photoLibrary: PhotoLibraryService
    let uploadQueue: UploadQueue

    var body: some View {
        TabView {
            ContentView()
                .tabItem {
                    Label(TabLabel.menu, systemImage: "photo.on.rectangle.angled")
                }

            NavigationStack {
                AutoBackupView(
                    store: store,
                    photoLibrary: photoLibrary,
                    uploadQueue: uploadQueue
                )
            }
            .tabItem {
                Label(TabLabel.settings, systemImage: "gearshape")
            }

            DashboardView()
                .tabItem {
                    Label(TabLabel.dashboard, systemImage: "square.grid.2x2")
                }
        }
    }
}
