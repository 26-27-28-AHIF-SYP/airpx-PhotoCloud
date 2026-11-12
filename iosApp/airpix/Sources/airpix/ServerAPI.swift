import Foundation

struct ServerInfo: Codable, Sendable {
    let name: String
    let apiVersion: String
    let status: String
}

struct LoginRequest: Codable, Sendable {
    let email: String
    let password: String
}

struct LoginResponse: Codable, Sendable {
    let accessToken: String
    let tokenType: String
    let expiresAt: Date
}

struct ApiError: Codable, Sendable {
    let type: String
    let title: String
    let status: Int
    let detail: String
    let instance: String?
    let code: String
}

typealias ContentHash = String

struct MissingPhotosRequest: Codable, Sendable {
    let hashes: [ContentHash]
}

struct MissingPhotosResponse: Codable, Sendable {
    let missingHashes: [ContentHash]
}

struct ServerPhotoLocation: Codable, Sendable {
    let latitude: Double
    let longitude: Double
}

enum PhotoMediaType: String, Codable, Sendable {
    case photo
    case video
}

struct UploadMetadata: Codable, Sendable {
    let capturedAt: Date?
    let location: ServerPhotoLocation?
    let originalFilename: String
    let contentHash: ContentHash
    let mediaType: PhotoMediaType
    let width: Int
    let height: Int
    let livePhotoPairId: String?

    private enum CodingKeys: String, CodingKey {
        case capturedAt, location, originalFilename, contentHash, mediaType, width, height, livePhotoPairId
    }

    init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        capturedAt = try values.decode(Date?.self, forKey: .capturedAt)
        location = try values.decodeIfPresent(ServerPhotoLocation.self, forKey: .location)
        originalFilename = try values.decode(String.self, forKey: .originalFilename)
        contentHash = try values.decode(ContentHash.self, forKey: .contentHash)
        mediaType = try values.decode(PhotoMediaType.self, forKey: .mediaType)
        width = try values.decode(Int.self, forKey: .width)
        height = try values.decode(Int.self, forKey: .height)
        livePhotoPairId = try values.decodeIfPresent(String.self, forKey: .livePhotoPairId)
    }

    func encode(to encoder: Encoder) throws {
        var values = encoder.container(keyedBy: CodingKeys.self)
        try values.encode(capturedAt, forKey: .capturedAt)
        try values.encodeIfPresent(location, forKey: .location)
        try values.encode(originalFilename, forKey: .originalFilename)
        try values.encode(contentHash, forKey: .contentHash)
        try values.encode(mediaType, forKey: .mediaType)
        try values.encode(width, forKey: .width)
        try values.encode(height, forKey: .height)
        try values.encodeIfPresent(livePhotoPairId, forKey: .livePhotoPairId)
    }
}

struct PhotoMetadata: Codable, Sendable {
    let capturedAt: Date?
    let location: ServerPhotoLocation?
    let originalFilename: String
    let mediaType: PhotoMediaType
    let width: Int
    let height: Int
    let livePhotoPairId: String?

    private enum CodingKeys: String, CodingKey {
        case capturedAt, location, originalFilename, mediaType, width, height, livePhotoPairId
    }

    init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        capturedAt = try values.decode(Date?.self, forKey: .capturedAt)
        location = try values.decodeIfPresent(ServerPhotoLocation.self, forKey: .location)
        originalFilename = try values.decode(String.self, forKey: .originalFilename)
        mediaType = try values.decode(PhotoMediaType.self, forKey: .mediaType)
        width = try values.decode(Int.self, forKey: .width)
        height = try values.decode(Int.self, forKey: .height)
        livePhotoPairId = try values.decodeIfPresent(String.self, forKey: .livePhotoPairId)
    }

    func encode(to encoder: Encoder) throws {
        var values = encoder.container(keyedBy: CodingKeys.self)
        try values.encode(capturedAt, forKey: .capturedAt)
        try values.encodeIfPresent(location, forKey: .location)
        try values.encode(originalFilename, forKey: .originalFilename)
        try values.encode(mediaType, forKey: .mediaType)
        try values.encode(width, forKey: .width)
        try values.encode(height, forKey: .height)
        try values.encodeIfPresent(livePhotoPairId, forKey: .livePhotoPairId)
    }
}

struct ServerPhoto: Codable, Sendable {
    let id: String
    let contentHash: ContentHash
    let uploadedAt: Date
    let metadata: PhotoMetadata
    let thumbnailUrl: URL
    let originalUrl: URL
}

struct PhotoPage: Codable, Sendable {
    let items: [ServerPhoto]
    let nextCursor: String?
}

enum ServerAPIError: Error, LocalizedError, Sendable {
    case invalidURL
    case unreachable
    case invalidResponse
    case invalidCredentials
    case unauthorized
    case http(status: Int, apiError: ApiError?)
    case notImplemented
    case credentialStorage

    var errorDescription: String? {
        switch self {
        case .invalidURL: AppStrings.invalidServerURL
        case .unreachable: AppStrings.serverUnreachable
        case .invalidResponse: AppStrings.invalidServerResponse
        case .invalidCredentials: AppStrings.invalidCredentials
        case .unauthorized: AppStrings.loginAgain
        case let .http(_, apiError): apiError?.detail ?? AppStrings.requestFailed
        case .notImplemented: AppStrings.apiNotImplemented
        case .credentialStorage: AppStrings.credentialStorageError
        }
    }
}

protocol ServerAPI: Sendable {
    func serverInfo(baseURL: URL) async throws -> ServerInfo
    func login(baseURL: URL, request: LoginRequest) async throws -> LoginResponse
    func findMissingPhotos(baseURL: URL, token: String, request: MissingPhotosRequest) async throws -> MissingPhotosResponse
    func uploadPhoto(baseURL: URL, token: String, fileURL: URL, metadata: UploadMetadata) async throws -> ServerPhoto
    func listPhotos(baseURL: URL, token: String, cursor: String?, limit: Int?) async throws -> PhotoPage
    func downloadThumbnail(baseURL: URL, token: String, photoID: String) async throws -> Data
    func downloadOriginal(baseURL: URL, token: String, photoID: String) async throws -> Data
    func deletePhoto(baseURL: URL, token: String, photoID: String) async throws
}

extension JSONDecoder {
    static var serverAPI: JSONDecoder {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return decoder
    }
}

extension JSONEncoder {
    static var serverAPI: JSONEncoder {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        return encoder
    }
}
