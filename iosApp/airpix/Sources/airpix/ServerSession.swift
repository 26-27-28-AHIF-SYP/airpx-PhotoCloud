import Foundation
import Observation
import Security

private struct SavedServerCredential: Codable {
    let token: String
    let expiresAt: Date
    let email: String
}

private enum ServerCredentialStore {
    private static let service = "com.example.airpix.server-session"
    private static let account = "access-token"

    static func read() -> SavedServerCredential? {
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword,
                                    kSecAttrService as String: service,
                                    kSecAttrAccount as String: account,
                                    kSecReturnData as String: true,
                                    kSecMatchLimit as String: kSecMatchLimitOne]
        var result: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data else { return nil }
        return try? JSONDecoder().decode(SavedServerCredential.self, from: data)
    }

    static func write(_ credential: SavedServerCredential?) throws {
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword,
                                    kSecAttrService as String: service,
                                    kSecAttrAccount as String: account]
        let deleteStatus = SecItemDelete(query as CFDictionary)
        guard deleteStatus == errSecSuccess || deleteStatus == errSecItemNotFound else { throw ServerAPIError.credentialStorage }
        guard let credential else { return }
        let data = try JSONEncoder().encode(credential)
        var item = query
        item[kSecValueData as String] = data
        item[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        guard SecItemAdd(item as CFDictionary, nil) == errSecSuccess else { throw ServerAPIError.credentialStorage }
    }
}

@Observable
@MainActor
final class ServerSession {
    private(set) var serverURL = UserDefaults.standard.string(forKey: "airpx.server.url") ?? ""
    private(set) var serverInfo: ServerInfo?
    private(set) var accountEmail: String?
    private(set) var expiresAt: Date?
    private(set) var requiresLogin = false
    private(set) var isChecking = false
    private(set) var errorMessage: String?
    private(set) var selectedImplementation: String
    #if DEBUG
    private(set) var mockFailureMode: MockFailureMode = .none
    #endif

    @ObservationIgnored private var token: String?
    @ObservationIgnored private var expirationTask: Task<Void, Never>?
    private let expiryMargin: TimeInterval = 60

    init() {
        #if DEBUG
        selectedImplementation = UserDefaults.standard.string(forKey: "airpx.server.implementation") == "mock" ? "mock" : "live"
        #else
        selectedImplementation = "live"
        #endif

        if let saved = ServerCredentialStore.read() {
            token = saved.token
            expiresAt = saved.expiresAt
            accountEmail = saved.email
            if saved.expiresAt <= .now.addingTimeInterval(expiryMargin) {
                expireSession()
            } else {
                scheduleExpiration(at: saved.expiresAt)
            }
        }
    }

    var isLoggedIn: Bool { token != nil && !requiresLogin }
    var implementationLabel: String { selectedImplementation == "mock" ? "Mock" : "Live" }
    var isUsingMockAPI: Bool { selectedImplementation == "mock" }

    #if DEBUG
    func setImplementation(_ value: String) {
        guard value != selectedImplementation else { return }
        selectedImplementation = value
        UserDefaults.standard.set(value, forKey: "airpx.server.implementation")
        logout()
        serverInfo = nil
        errorMessage = nil
    }

    func setMockFailureMode(_ value: MockFailureMode) {
        if value != mockFailureMode { logout() }
        mockFailureMode = value
    }
    #endif

    func updateServerURL(_ input: String) {
        let trimmed = input.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            if token != nil { logout() }
            serverURL = ""
            UserDefaults.standard.set("", forKey: "airpx.server.url")
            serverInfo = nil
            errorMessage = nil
            return
        }
        let withScheme = trimmed.contains("://") ? trimmed : "https://" + trimmed
        guard let parsed = URL(string: withScheme), let host = parsed.host?.lowercased(),
              let scheme = parsed.scheme?.lowercased(), parsed.user == nil, parsed.password == nil,
              scheme == "https" || (scheme == "http" && Self.developmentHTTPHosts.contains(host)) else {
            if token != nil { logout() }
            serverURL = withScheme
            serverInfo = nil
            errorMessage = AppStrings.httpsRequired
            return
        }
        let normalized = parsed.absoluteString.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        if token != nil { logout() }
        serverURL = normalized
        UserDefaults.standard.set(normalized, forKey: "airpx.server.url")
        serverInfo = nil
        errorMessage = nil
    }

    func validateServer() async {
        if errorMessage == AppStrings.httpsRequired { return }
        guard let url = baseURL else { errorMessage = AppStrings.invalidServerURL; return }
        isChecking = true
        errorMessage = nil
        serverInfo = nil
        defer { isChecking = false }
        do { serverInfo = try await api.serverInfo(baseURL: url) }
        catch { errorMessage = error.localizedDescription }
    }

    func login(email: String, password: String) async {
        guard let url = baseURL else { errorMessage = AppStrings.invalidServerURL; return }
        isChecking = true
        errorMessage = nil
        defer { isChecking = false }
        do {
            let response = try await api.login(baseURL: url, request: LoginRequest(email: email, password: password))
            guard response.tokenType == "Bearer" else { throw ServerAPIError.invalidResponse }
            do {
                try ServerCredentialStore.write(SavedServerCredential(token: response.accessToken, expiresAt: response.expiresAt, email: email))
            } catch {
                throw ServerAPIError.credentialStorage
            }
            accountEmail = email
            expiresAt = response.expiresAt
            if response.expiresAt <= .now.addingTimeInterval(expiryMargin) {
                expireSession()
                return
            }
            token = response.accessToken
            requiresLogin = false
            scheduleExpiration(at: response.expiresAt)
        } catch { errorMessage = error.localizedDescription }
    }

    func logout() {
        expirationTask?.cancel()
        expirationTask = nil
        token = nil
        expiresAt = nil
        accountEmail = nil
        requiresLogin = false
        do { try ServerCredentialStore.write(nil) }
        catch { errorMessage = AppStrings.credentialStorageError }
    }

    func refreshExpirationState() {
        guard let expiresAt, expiresAt <= .now.addingTimeInterval(expiryMargin) else { return }
        expireSession()
    }

    func performAuthenticated<T>(_ operation: () async throws -> T) async throws -> T {
        refreshExpirationState()
        guard token != nil, !requiresLogin else { throw ServerAPIError.unauthorized }
        do { return try await operation() }
        catch ServerAPIError.unauthorized {
            expireSession()
            throw ServerAPIError.unauthorized
        }
    }

    func syncCredentials() throws -> (baseURL: URL, token: String) {
        refreshExpirationState()
        guard let token, !requiresLogin, let baseURL else { throw ServerAPIError.unauthorized }
        return (baseURL, token)
    }

    func serverAPIClient() -> any ServerAPI { api }

    private var baseURL: URL? {
        guard let parsed = URL(string: serverURL), let host = parsed.host?.lowercased(),
              let scheme = parsed.scheme?.lowercased(), parsed.user == nil, parsed.password == nil else { return nil }
        guard scheme == "https" || (scheme == "http" && Self.developmentHTTPHosts.contains(host)) else { return nil }
        return parsed
    }

    private static let developmentHTTPHosts: Set<String> = ["10.0.2.2", "localhost", "127.0.0.1", "photocloud.local"]

    private var api: any ServerAPI {
        #if DEBUG
        if selectedImplementation == "mock" { return MockServerAPI(failureMode: mockFailureMode) }
        #endif
        return LiveServerAPI()
    }

    private func scheduleExpiration(at date: Date) {
        expirationTask?.cancel()
        let delay = max(0, date.timeIntervalSinceNow - expiryMargin)
        expirationTask = Task { [weak self] in
            try? await Task.sleep(for: .seconds(delay))
            guard !Task.isCancelled else { return }
            self?.expireSession()
        }
    }

    private func expireSession() {
        expirationTask?.cancel()
        expirationTask = nil
        token = nil
        requiresLogin = true
        try? ServerCredentialStore.write(nil)
    }
}
