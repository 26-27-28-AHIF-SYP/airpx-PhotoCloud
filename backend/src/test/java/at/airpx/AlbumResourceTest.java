package at.airpx;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(BackendTestProfile.class)
class AlbumResourceTest {
    private String token(String email, String password) {
        return given().contentType("application/json")
                .body("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("accessToken");
    }

    private String newUserToken() {
        String username =
                "album" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String email = username + "@example.com";
        given().contentType("application/json")
                .body("{\"username\":\"" + username + "\",\"email\":\"" + email
                        + "\",\"password\":\"securePassword123\"}")
                .post("/api/auth/register")
                .then()
                .statusCode(201);
        return token(email, "securePassword123");
    }

    private long createAlbum(String token, String name) {
        return given().auth()
                .oauth2(token)
                .contentType("application/json")
                .body("{\"name\":\"" + name + "\"}")
                .post("/api/albums")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getInt("id");
    }

    private static byte[] png() {
        byte[] suffix = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
        byte[] bytes = new byte[8 + suffix.length];
        byte[] signature = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        System.arraycopy(signature, 0, bytes, 0, signature.length);
        System.arraycopy(suffix, 0, bytes, signature.length, suffix.length);
        return bytes;
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    @Test
    void albumEndpointsRequireAuthenticationAndValidateNames() {
        given().get("/api/albums").then().statusCode(401);
        String owner = token("test@example.com", "correct horse battery staple");
        given().auth()
                .oauth2(owner)
                .contentType("application/json")
                .body("{\"name\":\" \"}")
                .post("/api/albums")
                .then()
                .statusCode(400);
    }

    @Test
    void ownerCanCreateRenameListAndDeleteAlbum() {
        String owner = token("test@example.com", "correct horse battery staple");
        long albumId = createAlbum(owner, "Holiday");
        given().auth()
                .oauth2(owner)
                .get("/api/albums")
                .then()
                .statusCode(200)
                .body("find { it.id == %d }.name".formatted(albumId), equalTo("Holiday"));
        given().auth()
                .oauth2(owner)
                .contentType("application/json")
                .body("{\"name\":\"Summer trip\"}")
                .put("/api/albums/" + albumId)
                .then()
                .statusCode(200)
                .body("name", equalTo("Summer trip"));
        given().auth()
                .oauth2(newUserToken())
                .contentType("application/json")
                .body("{\"name\":\"Stolen\"}")
                .put("/api/albums/" + albumId)
                .then()
                .statusCode(404);
        given().auth().oauth2(owner).delete("/api/albums/" + albumId).then().statusCode(204);
        given().auth()
                .oauth2(owner)
                .get("/api/albums")
                .then()
                .statusCode(200)
                .body("find { it.id == %d }".formatted(albumId), nullValue());
    }

    @Test
    void addingAndRemovingPicturesAndDeletingAlbumPreservesPicture() throws Exception {
        String owner = token("test@example.com", "correct horse battery staple");
        long albumId = createAlbum(owner, "Photos");
        byte[] bytes = png();
        String hash = sha256(bytes);
        long pictureId = given().auth()
                .oauth2(owner)
                .multiPart("file", "photo.png", bytes, "image/png")
                .multiPart("filename", "photo.png")
                .multiPart("sha256", hash)
                .multiPart("size", String.valueOf(bytes.length))
                .multiPart("mimeType", "image/png")
                .post("/api/pictures")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getInt("id");
        given().auth()
                .oauth2(owner)
                .post("/api/albums/%d/pictures/%d".formatted(albumId, pictureId))
                .then()
                .statusCode(204);
        given().auth()
                .oauth2(owner)
                .delete("/api/albums/%d/pictures/%d".formatted(albumId, pictureId))
                .then()
                .statusCode(204);
        given().auth()
                .oauth2(owner)
                .post("/api/albums/%d/pictures/%d".formatted(albumId, pictureId))
                .then()
                .statusCode(204);
        given().auth().oauth2(owner).delete("/api/albums/" + albumId).then().statusCode(204);
        given().auth()
                .oauth2(owner)
                .get("/api/pictures")
                .then()
                .statusCode(200)
                .body("find { it.id == %d }.sha256".formatted(pictureId), equalTo(hash));
    }
}
