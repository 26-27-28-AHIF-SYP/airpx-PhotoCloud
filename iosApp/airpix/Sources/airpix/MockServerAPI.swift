#if DEBUG
import Foundation

enum MockFailureMode: String, CaseIterable, Identifiable, Sendable {
    case none
    case wrongPassword
    case unreachable
    case expiredToken

    var id: Self { self }
    var title: String {
        switch self {
        case .none: AppStrings.noSimulation
        case .wrongPassword: AppStrings.wrongPasswordMode
        case .unreachable: AppStrings.unreachableMode
        case .expiredToken: AppStrings.expiringTokenMode
        }
    }
}

struct MockServerAPI: ServerAPI {
    let failureMode: MockFailureMode

    func serverInfo(baseURL: URL) async throws -> ServerInfo {
        if failureMode == .unreachable { throw ServerAPIError.unreachable }
        return ServerInfo(name: AppStrings.mockServerName, apiVersion: "1", status: "ok")
    }

    func login(baseURL: URL, request: LoginRequest) async throws -> LoginResponse {
        if failureMode == .unreachable { throw ServerAPIError.unreachable }
        guard failureMode != .wrongPassword,
              request.email == "demo@airpx.local", request.password == "airpx-demo" else {
            throw ServerAPIError.invalidCredentials
        }
        let lifetime: TimeInterval = failureMode == .expiredToken ? 90 : 86_400
        return LoginResponse(accessToken: "mock-access-token", tokenType: "Bearer", expiresAt: .now.addingTimeInterval(lifetime))
    }

    func findMissingPhotos(baseURL: URL, token: String, request: MissingPhotosRequest) async throws -> MissingPhotosResponse { throw ServerAPIError.unauthorized }
    func uploadPhoto(baseURL: URL, token: String, fileURL: URL, metadata: UploadMetadata) async throws -> ServerPhoto { throw ServerAPIError.notImplemented }
    func listPhotos(baseURL: URL, token: String, cursor: String?, limit: Int?) async throws -> PhotoPage { throw ServerAPIError.notImplemented }
    func downloadThumbnail(baseURL: URL, token: String, photoID: String) async throws -> Data { throw ServerAPIError.notImplemented }
    func downloadOriginal(baseURL: URL, token: String, photoID: String) async throws -> Data { throw ServerAPIError.notImplemented }
    func deletePhoto(baseURL: URL, token: String, photoID: String) async throws { throw ServerAPIError.notImplemented }
}
#endif
