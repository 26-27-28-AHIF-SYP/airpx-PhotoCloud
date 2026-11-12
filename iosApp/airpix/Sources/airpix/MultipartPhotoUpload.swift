import Foundation
import PhotoCloudSync

enum MultipartPhotoUpload {
    static func makeBody(fileURL: URL, metadata: PhotoUploadMetadata, boundary: String) throws -> URL {
        let outputURL = FileManager.default.temporaryDirectory.appendingPathComponent("airpx-\(UUID().uuidString).multipart")
        guard FileManager.default.createFile(atPath: outputURL.path, contents: nil) else {
            throw ServerAPIError.invalidResponse
        }
        do {
            let output = try FileHandle(forWritingTo: outputURL)
            defer { try? output.close() }
            func field(_ name: String, _ value: String) throws {
                try output.write(contentsOf: Data("--\(boundary)\r\nContent-Disposition: form-data; name=\"\(name)\"\r\n\r\n\(safe(value))\r\n".utf8))
            }
            try field("filename", metadata.filename)
            if let capturedAt = metadata.capturedAt {
                let formatter = ISO8601DateFormatter()
                formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
                try field("capturedAt", formatter.string(from: capturedAt))
            }
            try field("sha256", metadata.sha256)
            try field("size", String(metadata.size))
            try field("mimeType", metadata.mimeType)
            if let albumId = metadata.albumId { try field("albumId", albumId) }
            try output.write(contentsOf: Data("--\(boundary)\r\nContent-Disposition: form-data; name=\"file\"; filename=\"photo\"\r\nContent-Type: \(metadata.mimeType)\r\n\r\n".utf8))
            let input = try FileHandle(forReadingFrom: fileURL)
            defer { try? input.close() }
            while let chunk = try input.read(upToCount: 1024 * 1024), !chunk.isEmpty {
                try output.write(contentsOf: chunk)
            }
            try output.write(contentsOf: Data("\r\n--\(boundary)--\r\n".utf8))
            return outputURL
        } catch {
            try? FileManager.default.removeItem(at: outputURL)
            throw error
        }
    }

    private static func safe(_ value: String) -> String {
        value.replacingOccurrences(of: "\r", with: "_").replacingOccurrences(of: "\n", with: "_")
    }
}
