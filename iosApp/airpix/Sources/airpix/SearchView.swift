import SwiftUI

struct SearchView: View {
    let photoLibrary: PhotoLibraryService

    @Environment(\.scenePhase) private var scenePhase
    @State private var photos: [LocalPhoto] = []
    @State private var searchText = ""
    @State private var isDateRangeEnabled = false
    @State private var startDate = Date.now.addingTimeInterval(-30 * 24 * 60 * 60)
    @State private var endDate = Date.now
    @State private var isLimitedPickerPresented = false

    var body: some View {
        VStack(spacing: 0) {
            if hasPhotoAccess {
                VStack(alignment: .leading, spacing: 8) {
                    Toggle(AppStrings.dateRange, isOn: $isDateRangeEnabled)
                    if isDateRangeEnabled {
                        HStack {
                            DatePicker(AppStrings.fromDate, selection: $startDate, displayedComponents: .date)
                                .datePickerStyle(.compact)
                            DatePicker(AppStrings.toDate, selection: $endDate, displayedComponents: .date)
                                .datePickerStyle(.compact)
                        }
                    }
                    Label(AppStrings.aiSearchComingSoon, systemImage: "sparkles")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .padding(.horizontal)
                .padding(.vertical, 8)

                PhotoGridView(
                    photos: filteredPhotos,
                    photoLibrary: photoLibrary,
                    onDelete: { deleted in photos.removeAll { $0.id == deleted.id } },
                    onDismiss: loadPhotos
                )
            } else {
                PhotoAccessPromptView(photoLibrary: photoLibrary)
            }
        }
        .navigationTitle(AppStrings.search)
        .searchable(text: $searchText, prompt: AppStrings.searchPrompt)
        .toolbar {
            if photoLibrary.authorizationStatus == .limited {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(AppStrings.chooseMorePhotos) { isLimitedPickerPresented = true }
                }
            }
        }
        .task { loadPhotos() }
        .onChange(of: photoLibrary.authorizationStatus) { _, _ in loadPhotos() }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { loadPhotos() }
        }
        .onChange(of: startDate) { _, newValue in
            if newValue > endDate { endDate = newValue }
        }
        .onChange(of: endDate) { _, newValue in
            if newValue < startDate { startDate = newValue }
        }
        .background {
            if isLimitedPickerPresented {
                LimitedLibraryPickerBridge(isPresented: $isLimitedPickerPresented) {
                    loadPhotos()
                }
                .frame(width: 0, height: 0)
            }
        }
    }

    private var hasPhotoAccess: Bool {
        photoLibrary.authorizationStatus == .authorized
            || photoLibrary.authorizationStatus == .limited
    }

    private var filteredPhotos: [LocalPhoto] {
        photos.filter { photo in
            if isDateRangeEnabled {
                guard let date = photo.creationDate,
                      date >= Calendar.current.startOfDay(for: startDate),
                      date < Calendar.current.date(byAdding: .day, value: 1, to: Calendar.current.startOfDay(for: endDate)) ?? endDate else {
                    return false
                }
            }
            guard !searchText.isEmpty else { return true }
            guard let date = photo.creationDate else { return false }
            return date.formatted(date: .long, time: .omitted)
                .localizedCaseInsensitiveContains(searchText.trimmingCharacters(in: .whitespacesAndNewlines))
        }
    }

    private func loadPhotos() {
        photoLibrary.refreshAuthorizationStatus()
        photos = hasPhotoAccess
            ? photoLibrary.fetchLibraryPhotos(sortedBy: .captureDate, filteredBy: .all)
            : []
    }
}
