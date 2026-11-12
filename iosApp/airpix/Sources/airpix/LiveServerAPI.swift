import Foundation

struct LiveServerAPI: ServerAPI {
    private let session = URLSession.shared

    func serverInfo(baseURL: URL) async throws -> ServerInfo {
        try await request(baseURL: baseURL, path: "/api/v1/health", method: "GET")
    }

    func login(baseURL: URL, request login: LoginRequest) async throws -> LoginResponse {
        let body = try JSONEncoder.serverAPI.encode(login)
        do {
            return try await request(baseURL: baseURL, path: "/api/v1/auth/login", method: "POST", body: body)
        } catch ServerAPIError.http(status: 401, _) {
            throw ServerAPIError.invalidCredentials
        }
    }

    func findMissingPhotos(baseURL: URL, token: String, request: MissingPhotosRequest) async throws -> MissingPhotosResponse {
        throw ServerAPIError.notImplemented
    }

    func uploadPhoto(baseURL: URL, token: String, fileURL: URL, metadata: UploadMetadata) async throws -> ServerPhoto { throw ServerAPIError.notImplemented }
    func listPhotos(baseURL: URL, token: String, cursor: String?, limit: Int?) async throws -> PhotoPage { throw ServerAPIError.notImplemented }
    func downloadThumbnail(baseURL: URL, token: String, photoID: String) async throws -> Data { throw ServerAPIError.notImplemented }
    func downloadOriginal(baseURL: URL, token: String, photoID: String) async throws -> Data { throw ServerAPIError.notImplemented }
    func deletePhoto(baseURL: URL, token: String, photoID: String) async throws { throw ServerAPIError.notImplemented }

    private func request<T: Decodable & Sendable>(baseURL: URL, path: String, method: String, body: Data? = nil, token: String? = nil) async throws -> T {
        guard let url = URL(string: path, relativeTo: baseURL)?.absoluteURL else { throw ServerAPIError.invalidURL }
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.httpBody = body
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if body != nil { request.setValue("application/json", forHTTPHeaderField: "Content-Type") }
        if let token { request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization") }
        let data: Data
        let response: URLResponse
        do { (data, response) = try await session.data(for: request) }
        catch { throw ServerAPIError.unreachable }
        guard let response = response as? HTTPURLResponse else { throw ServerAPIError.invalidResponse }
        guard (200..<300).contains(response.statusCode) else {
            let apiError = try? JSONDecoder.serverAPI.decode(ApiError.self, from: data)
            if token != nil && response.statusCode == 401 { throw ServerAPIError.unauthorized }
            throw ServerAPIError.http(status: response.statusCode, apiError: apiError)
        }
        do { return try JSONDecoder.serverAPI.decode(T.self, from: data) }
        catch { throw ServerAPIError.invalidResponse }
    }
}
