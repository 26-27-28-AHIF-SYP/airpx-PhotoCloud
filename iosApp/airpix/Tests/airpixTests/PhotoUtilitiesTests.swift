import Foundation
import XCTest
import PhotoCloudSync
@testable import airpix

final class PhotoUtilitiesTests: XCTestCase {
    func testMultipartBodyContainsMetadataAndPhotoBytes() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let photoURL = directory.appendingPathComponent("photo.jpg")
        let photoBytes = Data("photo-bytes".utf8)
        try photoBytes.write(to: photoURL)
        let boundary = "test-boundary"
        let metadata = PhotoUploadMetadata(
            filename: "photo\r\nname.jpg",
            capturedAt: Date(timeIntervalSince1970: 0),
            sha256: "abc123",
            size: Int64(photoBytes.count),
            mimeType: "image/jpeg",
            albumId: "album-1"
        )

        let bodyURL = try MultipartPhotoUpload.makeBody(fileURL: photoURL, metadata: metadata, boundary: boundary)
        defer { try? FileManager.default.removeItem(at: bodyURL) }
        let body = try Data(contentsOf: bodyURL)
        let text = String(decoding: body, as: UTF8.self)

        XCTAssertTrue(text.contains("name=\"filename\"\r\n\r\nphoto__name.jpg\r\n"))
        XCTAssertTrue(text.contains("name=\"sha256\"\r\n\r\nabc123\r\n"))
        XCTAssertTrue(text.contains("name=\"size\"\r\n\r\n\(photoBytes.count)\r\n"))
        XCTAssertTrue(text.contains("name=\"mimeType\"\r\n\r\nimage/jpeg\r\n"))
        XCTAssertTrue(text.contains("name=\"albumId\"\r\n\r\nalbum-1\r\n"))
        XCTAssertTrue(text.contains("name=\"capturedAt\"\r\n\r\n1970-01-01T00:00:00.000Z\r\n"))
        XCTAssertTrue(text.contains("Content-Type: image/jpeg\r\n\r\nphoto-bytes\r\n--\(boundary)--\r\n"))
    }

    func testPhotoHasherMatchesKnownSHA256Digest() async throws {
        let fileURL = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try Data("abc".utf8).write(to: fileURL)
        defer { try? FileManager.default.removeItem(at: fileURL) }

        let hash = try await CryptoKitPhotoHasher().sha256(fileURL: fileURL)

        XCTAssertEqual(hash, "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
    }
}
