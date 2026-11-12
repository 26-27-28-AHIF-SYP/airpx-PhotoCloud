package at.airpx;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class AlbumResourceTest {
    @Test
    void testAlbumEndpoint() {
        given()
          .when().get("/api/albums")
          .then()
             .statusCode(200);
    }

}