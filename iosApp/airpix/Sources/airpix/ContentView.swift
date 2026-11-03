import SwiftUI
#if canImport(UIKit)
import UIKit
#elseif canImport(AppKit)
import AppKit
#endif

struct ContentView: View {
    let store: SyncStore
    let photoLibrary: PhotoLibraryService
    let uploadQueue: UploadQueue

    private var logo: Image? {
        guard let url = Bundle.module.url(forResource: "homescreen-logo", withExtension: "jpg") else {
            return nil
        }

        #if canImport(UIKit)
        guard let image = UIImage(contentsOfFile: url.path) else {
            return nil
        }
        return Image(uiImage: image)
        #elseif canImport(AppKit)
        guard let image = NSImage(contentsOf: url) else {
            return nil
        }
        return Image(nsImage: image)
        #else
        return nil
        #endif
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                if let logo {
                    logo
                        .resizable()
                        .scaledToFit()
                        .frame(width: 200, height: 200)
                        .accessibilityLabel("AirPx logo")
                } else {
                    Text("Logo could not be loaded.")
                        .foregroundStyle(.secondary)
                }

                Text("Welcome to airpx")
                    .font(.largeTitle.bold())

                Text("We're glad you and your photos are here.")
                    .font(.body)
                    .foregroundStyle(.secondary)

                NavigationLink("Auto-Backup Settings") {
                    AutoBackupView(
                        store: store,
                        photoLibrary: photoLibrary,
                        uploadQueue: uploadQueue
                    )
                }
            }
            .padding()
        }
    }
}
