package at.airpx;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import at.airpx.workloads.account.Account;
import at.airpx.workloads.account.AccountRepository;
import at.airpx.workloads.album.Album;
import at.airpx.workloads.album.AlbumRepository;
import at.airpx.workloads.picture.PictureRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(BackendTestProfile.class)
class PictureResourceTest {
    private static final byte[] CONTENT = "photo test bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    private static final String HASH = hash(CONTENT);

    @Inject
    AlbumRepository albumRepository;

    @Inject
    AccountRepository accountRepository;

    @Inject
    PictureRepository pictureRepository;

    private static String hash(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String token(String email, String password) {
        return given().contentType("application/json")
                .body("""
                {"email":"%s","password":"%s"}
                """.formatted(email, password))
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("accessToken");
    }

    @Test
    void apiHealthResourceIsRegisteredAtApiRoot() {
        given().get("/api").then().statusCode(200).body("status", equalTo("up"));
    }

    @Test
    void pictureEndpointsRequireValidBearerToken() {
        given().get("/api/pictures").then().statusCode(401);
        given().header("Authorization", "Bearer invalid")
                .get("/api/pictures")
                .then()
                .statusCode(401);
    }

    @Test
    void uploadDedupCheckAndListAreScopedToAuthenticatedUser() {
        String owner = token("test@example.com", "correct horse battery staple");
        given().auth()
                .oauth2(owner)
                .contentType("application/json")
                .body("{\"hashes\":[\"" + HASH + "\"]}")
                .post("/api/pictures/missing")
                .then()
                .statusCode(200)
                .body("missingHashes", hasItem(HASH));

        Response uploaded = given().auth()
                .oauth2(owner)
                .multiPart("file", "photo.jpg", CONTENT, "image/jpeg")
                .multiPart("filename", "photo.jpg")
                .multiPart("capturedAt", "2024-01-02T03:04:05Z")
                .multiPart("sha256", HASH)
                .multiPart("size", String.valueOf(CONTENT.length))
                .multiPart("mimeType", "image/jpeg")
                .post("/api/pictures");
        uploaded.then().statusCode(201).body("sha256", equalTo(HASH)).body("filename", equalTo("photo.jpg"));
        given().auth()
                .oauth2(owner)
                .contentType("application/json")
                .body("{\"hashes\":[\"" + HASH + "\"]}")
                .post("/api/pictures/missing")
                .then()
                .statusCode(200)
                .body("missingHashes", empty());
        given().auth()
                .oauth2(owner)
                .get("/api/pictures")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("find { it.sha256 == '%s' }.id".formatted(HASH), notNullValue());

        String username =
                "photo" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String email = username + "@example.com";
        given().contentType("application/json")
                .body("""
                {"username":"%s","email":"%s","password":"securePassword123"}
                """.formatted(username, email))
                .post("/api/auth/register")
                .then()
                .statusCode(201);
        String otherUser = token(email, "securePassword123");
        given().auth()
                .oauth2(otherUser)
                .get("/api/pictures")
                .then()
                .statusCode(200)
                .body("size()", is(0));
        given().auth()
                .oauth2(otherUser)
                .contentType("application/json")
                .body("{\"hashes\":[\"" + HASH + "\"]}")
                .post("/api/pictures/missing")
                .then()
                .statusCode(200)
                .body("missingHashes", hasItem(HASH));
    }

    @Test
    void uploadUsesGeneratedStorageNameAndRejectsUnsafeMetadata() throws Exception {
        String token = token("test@example.com", "correct horse battery staple");
        byte[] bytes = UUID.randomUUID().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String hash = hash(bytes);
        given().auth()
                .oauth2(token)
                .multiPart("file", "ignored.jpg", bytes, "image/jpeg")
                .multiPart("filename", "../../outside.jpg")
                .multiPart("sha256", hash)
                .multiPart("size", String.valueOf(bytes.length))
                .multiPart("mimeType", "image/jpeg")
                .post("/api/pictures")
                .then()
                .statusCode(201)
                .body("filename", equalTo("outside.jpg"));

        var saved =
                pictureRepository
                        .findAll(accountRepository
                                .findByEmail("test@example.com")
                                .orElseThrow()
                                .getId())
                        .stream()
                        .filter(item -> item.getSha256().equals(hash))
                        .findFirst()
                        .orElseThrow();
        assertThat(saved.getStorageName(), matchesPattern("[0-9a-f-]{36}"));
        assertThat(Files.isRegularFile(Path.of("target/test-photos").resolve(saved.getStorageName())), is(true));

        byte[] wrongHashBytes = UUID.randomUUID().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        given().auth()
                .oauth2(token)
                .multiPart("file", "wrong.jpg", wrongHashBytes, "image/jpeg")
                .multiPart("filename", "wrong.jpg")
                .multiPart("sha256", "0".repeat(64))
                .multiPart("size", String.valueOf(wrongHashBytes.length))
                .multiPart("mimeType", "image/jpeg")
                .post("/api/pictures")
                .then()
                .statusCode(400);

        byte[] wrongMimeBytes = UUID.randomUUID().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        given().auth()
                .oauth2(token)
                .multiPart("file", "wrong.bin", wrongMimeBytes, "application/octet-stream")
                .multiPart("filename", "wrong.bin")
                .multiPart("sha256", hash(wrongMimeBytes))
                .multiPart("size", String.valueOf(wrongMimeBytes.length))
                .multiPart("mimeType", "application/octet-stream")
                .post("/api/pictures")
                .then()
                .statusCode(400);
    }

    @Test
    void uploadAcceptsOnlyAlbumsOwnedByAuthenticatedUser() {
        String ownerToken = token("test@example.com", "correct horse battery staple");
        String otherName =
                "album" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String otherEmail = otherName + "@example.com";
        given().contentType("application/json")
                .body("""
                {"username":"%s","email":"%s","password":"securePassword123"}
                """.formatted(otherName, otherEmail))
                .post("/api/auth/register")
                .then()
                .statusCode(201);
        var owner = accountRepository.findByEmail("test@example.com").orElseThrow();
        var other = accountRepository.findByEmail(otherEmail).orElseThrow();
        Album ownAlbum = createAlbum(owner, "owned by test user");
        Album otherAlbum = createAlbum(other, "owned by other user");
        byte[] bytes = UUID.randomUUID().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

        upload(ownerToken, bytes, ownAlbum.getId().toString()).then().statusCode(201);
        upload(
                        ownerToken,
                        UUID.randomUUID().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        otherAlbum.getId().toString())
                .then()
                .statusCode(400);
        upload(ownerToken, UUID.randomUUID().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8), "999999999")
                .then()
                .statusCode(400);
    }

    private io.restassured.response.Response upload(String token, byte[] bytes, String albumId) {
        return given().auth()
                .oauth2(token)
                .multiPart("file", "photo.jpg", bytes, "image/jpeg")
                .multiPart("filename", "photo.jpg")
                .multiPart("sha256", hash(bytes))
                .multiPart("size", String.valueOf(bytes.length))
                .multiPart("mimeType", "image/jpeg")
                .multiPart("albumId", albumId)
                .post("/api/pictures");
    }

    private Album createAlbum(Account owner, String name) {
        Album album = new Album();
        album.setName(name);
        album.setCreatedAt(LocalDateTime.now());
        album.setOwner(owner);
        albumRepository.persist(album);
        return album;
    }
}
