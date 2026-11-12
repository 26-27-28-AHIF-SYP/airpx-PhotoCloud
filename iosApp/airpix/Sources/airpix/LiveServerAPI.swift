import Foundation
import PhotoCloudSync

struct LiveServerAPI: ServerAPI {
    private let session = URLSession.shared

    func serverInfo(baseURL: URL) async throws -> ServerInfo {
        try await request(baseURL: baseURL, path: "/api", method: "GET")
    }

    func login(baseURL: URL, request login: LoginRequest) async throws -> LoginResponse {
        let body = try JSONEncoder.serverAPI.encode(login)
        do {
            return try await request(baseURL: baseURL, path: "/api/auth/login", method: "POST", body: body)
        } catch ServerAPIError.http(status: 401, _) {
            throw ServerAPIError.invalidCredentials
        }
    }

    func missingHashes(baseURL: URL, token: String, hashes: [String]) async throws -> Set<String> {
        let body = try JSONEncoder.serverAPI.encode(MissingPhotosRequest(hashes: hashes))
        let response: MissingPhotosResponse = try await request(
            baseURL: baseURL, path: "/api/photos/missing", method: "POST", body: body, token: token
        )
        return Set(response.missingHashes)
    }

    func uploadPhoto(baseURL: URL, token: String, asset: PhotoSyncAsset, fileURL: URL, metadata: PhotoUploadMetadata) async throws {
        let boundary = "AirPx-\(UUID().uuidString)"
        let bodyURL = try MultipartPhotoUpload.makeBody(fileURL: fileURL, metadata: metadata, boundary: boundary)
        defer { try? FileManager.default.removeItem(at: bodyURL) }
        guard let url = URL(string: "/api/photos", relativeTo: baseURL)?.absoluteURL else { throw ServerAPIError.invalidURL }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        let (data, response) = try await session.upload(for: request, fromFile: bodyURL)
        guard let response = response as? HTTPURLResponse else { throw ServerAPIError.invalidResponse }
        guard (200..<300).contains(response.statusCode) else {
            let apiError = try? JSONDecoder.serverAPI.decode(ApiError.self, from: data)
            if response.statusCode == 401 { throw ServerAPIError.unauthorized }
            throw ServerAPIError.http(status: response.statusCode, apiError: apiError)
        }
    }

    func listPhotos(baseURL: URL, token: String) async throws -> [PhotoServerRecord] {
        try await request(baseURL: baseURL, path: "/api/photos", method: "GET", token: token)
    }

    private func request<T: Decodable & Sendable>(
        baseURL: URL,
        path: String,
        method: String,
        body: Data? = nil,
        token: String? = nil
    ) async throws -> T {
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

private struct MissingPhotosRequest: Encodable {
    let hashes: [String]
}
