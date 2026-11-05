import SwiftUI

struct ContentView: View {
    var body: some View {
        Text(AppStrings.libraryPlaceholder)
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .navigationTitle(AppStrings.library)
    }
}
