import CryptoKit
import Foundation
import PhotoCloudSync

struct CryptoKitPhotoHasher: PhotoFileHasher {
    func sha256(fileURL: URL) async throws -> String {
        try await Task.detached(priority: .utility) {
            let file = try FileHandle(forReadingFrom: fileURL)
            defer { try? file.close() }
            var digest = SHA256()
            while let chunk = try file.read(upToCount: 1024 * 1024), !chunk.isEmpty {
                digest.update(data: chunk)
            }
            return digest.finalize().map { String(format: "%02x", $0) }.joined()
        }.value
    }
}
