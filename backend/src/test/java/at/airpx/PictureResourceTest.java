package at.airpx;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import at.airpx.workloads.account.Account;
import at.airpx.workloads.account.AccountRepository;
import at.airpx.workloads.album.Album;
import at.airpx.workloads.album.AlbumPermission;
import at.airpx.workloads.album.AlbumRepository;
import at.airpx.workloads.picture.Picture;
import at.airpx.workloads.picture.PictureRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(BackendTestProfile.class)
class PictureResourceTest {
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final String PNG_CONTENT_TYPE = "image/png";
    private static final byte[] CONTENT = png("photo test bytes");
    private static final String HASH = hash(CONTENT);

    @Inject
    AlbumRepository albumRepository;

    @Inject
    AccountRepository accountRepository;

    @Inject
    PictureRepository pictureRepository;

    @Inject
    EntityManager entityManager;

    private static byte[] png(String payload) {
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        byte[] content = new byte[PNG_SIGNATURE.length + payloadBytes.length];
        System.arraycopy(PNG_SIGNATURE, 0, content, 0, PNG_SIGNATURE.length);
        System.arraycopy(payloadBytes, 0, content, PNG_SIGNATURE.length, payloadBytes.length);
        return content;
    }

    private static String hash(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
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

    private TestUser registerUser() {
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
        return new TestUser(email, token(email, "securePassword123"));
    }

    private RequestSpecification uploadRequest(String token, byte[] bytes, String filename) {
        return given().auth()
                .oauth2(token)
                .multiPart("file", filename, bytes, PNG_CONTENT_TYPE)
                .multiPart("filename", filename)
                .multiPart("sha256", hash(bytes))
                .multiPart("size", String.valueOf(bytes.length))
                .multiPart("mimeType", PNG_CONTENT_TYPE);
    }

    private long upload(String token, byte[] bytes) {
        return uploadRequest(token, bytes, "photo.png")
                .post("/api/pictures")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");
    }

    private static byte[] randomPng() {
        return png(UUID.randomUUID().toString());
    }

    @Test
    void apiHealthResourceIsRegisteredAtApiRoot() {
        given().get("/api").then().statusCode(200).body("status", equalTo("up"));
    }

    @Test
    void pictureEndpointsRequireValidBearerToken() {
        given().get("/api/pictures").then().statusCode(401).body("message", notNullValue());
        given().header("Authorization", "Bearer invalid")
                .get("/api/pictures")
                .then()
                .statusCode(401);
        given().multiPart("file", "photo.png", randomPng(), PNG_CONTENT_TYPE)
                .post("/api/pictures")
                .then()
                .statusCode(401);
        given().auth().oauth2("invalid").get("/api/pictures/1").then().statusCode(401);
    }

    @Test
    void uploadRejectsFilenameWithoutBaseName() {
        String owner = token("test@example.com", "correct horse battery staple");
        byte[] content = randomPng();
        given().auth()
                .oauth2(owner)
                .multiPart("file", "photo.png", content, PNG_CONTENT_TYPE)
                .multiPart("filename", "/")
                .multiPart("capturedAt", "2024-01-02T03:04:05Z")
                .multiPart("sha256", hash(content))
                .multiPart("size", String.valueOf(content.length))
                .multiPart("mimeType", PNG_CONTENT_TYPE)
                .post("/api/pictures")
                .then()
                .statusCode(400);
    }

    @Test
    void uploadAcceptsOnlyFileFieldAndReturnsLocation() {
        String owner = token("test@example.com", "correct horse battery staple");
        byte[] content = randomPng();
        Response uploaded = given().auth()
                .oauth2(owner)
                .multiPart("file", "only.png", content, PNG_CONTENT_TYPE)
                .post("/api/pictures");
        uploaded.then()
                .statusCode(201)
                .body("filename", equalTo("only.png"))
                .body("mimeType", equalTo(PNG_CONTENT_TYPE))
                .body("size", equalTo(content.length));
        long id = uploaded.jsonPath().getLong("id");
        assertThat(uploaded.header("Location"), endsWith("/api/pictures/" + id));
    }

    @Test
    void uploadRejectsUnsupportedMimeType() {
        String owner = token("test@example.com", "correct horse battery staple");
        byte[] gif = "GIF89a".getBytes(StandardCharsets.US_ASCII);
        given().auth()
                .oauth2(owner)
                .multiPart("file", "photo.gif", gif, "image/gif")
                .multiPart("filename", "photo.gif")
                .multiPart("sha256", hash(gif))
                .multiPart("size", String.valueOf(gif.length))
                .multiPart("mimeType", "image/gif")
                .post("/api/pictures")
                .then()
                .statusCode(415);
    }

    @Test
    void uploadRejectsMimeTypeThatDoesNotMatchContent() {
        String owner = token("test@example.com", "correct horse battery staple");
        byte[] content = randomPng();
        given().auth()
                .oauth2(owner)
                .multiPart("file", "photo.jpg", content, "image/jpeg")
                .multiPart("filename", "photo.jpg")
                .multiPart("sha256", hash(content))
                .multiPart("size", String.valueOf(content.length))
                .multiPart("mimeType", "image/jpeg")
                .post("/api/pictures")
                .then()
                .statusCode(415);
    }

    @Test
    void uploadRejectsTooLargePicture() {
        String owner = token("test@example.com", "correct horse battery staple");
        byte[] content = png("x".repeat(4096));
        given().auth()
                .oauth2(owner)
                .multiPart("file", "large.png", content, PNG_CONTENT_TYPE)
                .multiPart("filename", "large.png")
                .multiPart("sha256", hash(content))
                .multiPart("size", String.valueOf(content.length))
                .multiPart("mimeType", PNG_CONTENT_TYPE)
                .post("/api/pictures")
                .then()
                .statusCode(413)
                .body("message", notNullValue());
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
                .multiPart("file", "photo.png", CONTENT, PNG_CONTENT_TYPE)
                .multiPart("filename", "photo.png")
                .multiPart("capturedAt", "2024-01-02T03:04:05Z")
                .multiPart("sha256", HASH)
                .multiPart("size", String.valueOf(CONTENT.length))
                .multiPart("mimeType", PNG_CONTENT_TYPE)
                .post("/api/pictures");
        uploaded.then().statusCode(201).body("sha256", equalTo(HASH)).body("filename", equalTo("photo.png"));
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

        TestUser other = registerUser();
        given().auth()
                .oauth2(other.token())
                .get("/api/pictures")
                .then()
                .statusCode(200)
                .body("size()", is(0));
        given().auth()
                .oauth2(other.token())
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
        byte[] bytes = randomPng();
        String hash = hash(bytes);
        given().auth()
                .oauth2(token)
                .multiPart("file", "ignored.png", bytes, PNG_CONTENT_TYPE)
                .multiPart("filename", "../../outside.png")
                .multiPart("sha256", hash)
                .multiPart("size", String.valueOf(bytes.length))
                .multiPart("mimeType", PNG_CONTENT_TYPE)
                .post("/api/pictures")
                .then()
                .statusCode(201)
                .body("filename", equalTo("outside.png"));

        var saved =
                pictureRepository
                        .findByOwnerId(
                                accountRepository
                                        .findByEmail("test@example.com")
                                        .orElseThrow()
                                        .getId(),
                                0,
                                100)
                        .stream()
                        .filter(item -> item.getSha256().equals(hash))
                        .findFirst()
                        .orElseThrow();
        assertThat(saved.getStorageName(), matchesPattern("[0-9a-f-]{36}"));
        assertThat(Files.isRegularFile(Path.of("target/test-photos").resolve(saved.getStorageName())), is(true));

        byte[] wrongHashBytes = randomPng();
        given().auth()
                .oauth2(token)
                .multiPart("file", "wrong.png", wrongHashBytes, PNG_CONTENT_TYPE)
                .multiPart("filename", "wrong.png")
                .multiPart("sha256", "0".repeat(64))
                .multiPart("size", String.valueOf(wrongHashBytes.length))
                .multiPart("mimeType", PNG_CONTENT_TYPE)
                .post("/api/pictures")
                .then()
                .statusCode(400);

        byte[] wrongMimeBytes = randomPng();
        given().auth()
                .oauth2(token)
                .multiPart("file", "wrong.bin", wrongMimeBytes, "application/octet-stream")
                .multiPart("filename", "wrong.bin")
                .multiPart("sha256", hash(wrongMimeBytes))
                .multiPart("size", String.valueOf(wrongMimeBytes.length))
                .multiPart("mimeType", "application/octet-stream")
                .post("/api/pictures")
                .then()
                .statusCode(415);
    }

    @Test
    void uploadAcceptsOnlyAlbumsOwnedByAuthenticatedUser() {
        String ownerToken = token("test@example.com", "correct horse battery staple");
        TestUser other = registerUser();
        var owner = accountRepository.findByEmail("test@example.com").orElseThrow();
        var otherAccount = accountRepository.findByEmail(other.email()).orElseThrow();
        Album ownAlbum = createAlbum(owner, "owned by test user");
        Album otherAlbum = createAlbum(otherAccount, "owned by other user");

        uploadRequest(ownerToken, randomPng(), "own.png")
                .multiPart("albumId", ownAlbum.getId().toString())
                .post("/api/pictures")
                .then()
                .statusCode(201);
        uploadRequest(ownerToken, randomPng(), "foreign.png")
                .multiPart("albumId", otherAlbum.getId().toString())
                .post("/api/pictures")
                .then()
                .statusCode(400);
        uploadRequest(ownerToken, randomPng(), "missing.png")
                .multiPart("albumId", "999999999")
                .post("/api/pictures")
                .then()
                .statusCode(400);
    }

    @Test
    void listPaginatesWithStableOrderAndCapsPageSize() {
        TestUser user = registerUser();
        List<Long> uploadedIds = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            Response uploaded = uploadRequest(user.token(), png("pagination-" + index), "page" + index + ".png")
                    .multiPart("capturedAt", "2024-01-0" + (index + 1) + "T00:00:00Z")
                    .post("/api/pictures");
            uploaded.then().statusCode(201);
            uploadedIds.add(uploaded.jsonPath().getLong("id"));
        }
        List<Long> newestFirst = new ArrayList<>(uploadedIds);
        Collections.reverse(newestFirst);

        Response firstPage = given().auth()
                .oauth2(user.token())
                .queryParam("page", 1)
                .queryParam("size", 2)
                .get("/api/pictures");
        firstPage
                .then()
                .statusCode(200)
                .header("X-Total-Count", "6")
                .header("X-Page", "1")
                .header("X-Page-Size", "2")
                .body("size()", is(2));
        assertThat(firstPage.jsonPath().getList("id", Long.class), contains(newestFirst.get(0), newestFirst.get(1)));

        Response repeatedPage = given().auth()
                .oauth2(user.token())
                .queryParam("page", 1)
                .queryParam("size", 2)
                .get("/api/pictures");
        assertThat(repeatedPage.jsonPath().getList("id", Long.class), contains(newestFirst.get(0), newestFirst.get(1)));

        Response thirdPage = given().auth()
                .oauth2(user.token())
                .queryParam("page", 3)
                .queryParam("size", 2)
                .get("/api/pictures");
        thirdPage.then().statusCode(200).body("size()", is(2));
        assertThat(thirdPage.jsonPath().getList("id", Long.class), contains(newestFirst.get(4), newestFirst.get(5)));

        given().auth()
                .oauth2(user.token())
                .queryParam("page", 1)
                .queryParam("size", 100)
                .get("/api/pictures")
                .then()
                .statusCode(200)
                .header("X-Page-Size", "5")
                .body("size()", is(5));
    }

    @Test
    void listRejectsNonPositivePageParameters() {
        String token = token("test@example.com", "correct horse battery staple");
        given().auth()
                .oauth2(token)
                .queryParam("page", 0)
                .get("/api/pictures")
                .then()
                .statusCode(400);
        given().auth()
                .oauth2(token)
                .queryParam("size", 0)
                .get("/api/pictures")
                .then()
                .statusCode(400);
    }

    @Test
    void loadReturnsPictureFileForOwner() {
        String token = token("test@example.com", "correct horse battery staple");
        byte[] content = randomPng();
        long pictureId = upload(token, content);

        Response file = given().auth().oauth2(token).get("/api/pictures/" + pictureId);
        file.then().statusCode(200).contentType(PNG_CONTENT_TYPE);
        assertThat(file.asByteArray(), equalTo(content));
    }

    @Test
    void loadReturnsNotFoundForUnknownPicture() {
        String token = token("test@example.com", "correct horse battery staple");
        given().auth().oauth2(token).get("/api/pictures/999999999").then().statusCode(404);
    }

    @Test
    void loadReturnsNotFoundForForeignPicture() {
        String ownerToken = token("test@example.com", "correct horse battery staple");
        long pictureId = upload(ownerToken, randomPng());
        TestUser other = registerUser();

        given().auth()
                .oauth2(other.token())
                .get("/api/pictures/" + pictureId)
                .then()
                .statusCode(404);
    }

    @Test
    void loadAllowsReadAccessThroughAlbumPermission() {
        String ownerToken = token("test@example.com", "correct horse battery staple");
        long pictureId = upload(ownerToken, randomPng());
        TestUser collaborator = registerUser();
        Album album =
                createAlbum(accountRepository.findByEmail("test@example.com").orElseThrow(), "shared album");
        grantAlbumPermission(album, pictureId, collaborator.email(), true);

        given().auth()
                .oauth2(collaborator.token())
                .get("/api/pictures/" + pictureId)
                .then()
                .statusCode(200)
                .contentType(PNG_CONTENT_TYPE);
    }

    @Test
    void loadRejectsAlbumPermissionWithoutReadAccess() {
        String ownerToken = token("test@example.com", "correct horse battery staple");
        long pictureId = upload(ownerToken, randomPng());
        TestUser collaborator = registerUser();
        Album album =
                createAlbum(accountRepository.findByEmail("test@example.com").orElseThrow(), "restricted album");
        grantAlbumPermission(album, pictureId, collaborator.email(), false);

        given().auth()
                .oauth2(collaborator.token())
                .get("/api/pictures/" + pictureId)
                .then()
                .statusCode(404);
    }

    private Album createAlbum(Account owner, String name) {
        Album album = new Album();
        album.setName(name);
        album.setCreatedAt(LocalDateTime.now());
        album.setOwner(owner);
        albumRepository.persist(album);
        return album;
    }

    private void grantAlbumPermission(Album album, long pictureId, String collaboratorEmail, boolean canRead) {
        long collaboratorId =
                accountRepository.findByEmail(collaboratorEmail).orElseThrow().getId();
        QuarkusTransaction.requiringNew().run(() -> {
            Album managedAlbum = entityManager.find(Album.class, album.getId());
            Picture managedPicture = entityManager.find(Picture.class, pictureId);
            Account collaborator = entityManager.find(Account.class, collaboratorId);
            managedAlbum.addPicture(managedPicture);
            entityManager.persist(new AlbumPermission(collaborator, managedAlbum, canRead, false, false, false));
        });
    }

    private record TestUser(String email, String token) {}
}
