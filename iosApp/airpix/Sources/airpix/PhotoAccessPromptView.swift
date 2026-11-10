import SwiftUI
import UIKit

struct PhotoAccessPromptView: View {
    let photoLibrary: PhotoLibraryService

    @Environment(\.openURL) private var openURL

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "photo.on.rectangle.angled")
                .font(.system(size: 44))
                .foregroundStyle(.secondary)
            Text(AppStrings.photoAccessExplanation)
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
            if photoLibrary.authorizationStatus == .notDetermined {
                Button(AppStrings.allowPhotoAccess) {
                    Task { await photoLibrary.requestAuthorization() }
                }
                .buttonStyle(.borderedProminent)
            } else {
                Text(AppStrings.photoAccessDenied)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.secondary)
                Button(AppStrings.openSystemSettings) {
                    if let url = URL(string: UIApplication.openSettingsURLString) {
                        openURL(url)
                    }
                }
                .buttonStyle(.bordered)
            }
        }
        .padding(32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
