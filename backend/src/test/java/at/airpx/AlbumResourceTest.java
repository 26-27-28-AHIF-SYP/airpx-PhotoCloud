package at.airpx;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(BackendTestProfile.class)
class AlbumResourceTest {
    @Test
    void testAlbumEndpoint() {
        given().when().get("/api/albums").then().statusCode(200);
    }
}
