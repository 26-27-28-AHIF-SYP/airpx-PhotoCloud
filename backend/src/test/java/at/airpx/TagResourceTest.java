package at.airpx;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class TagResourceTest {
    @Test
    void tagEndpointRequiresAuthentication() {
        given()
                .when().get("/api/tags")
                .then()
                .statusCode(401);
    }

    @Test
    void authenticatedAccountCanListTags() {
        String token = given()
                .contentType("application/json")
                .body("""
                        {"email":"test@example.com","password":"correct horse battery staple"}
                        """)
                .when().post("/api/auth/login")
                .then().statusCode(200)
                .extract().path("accessToken");

        given()
                .auth().oauth2(token)
                .when().get("/api/tags")
                .then()
                .statusCode(200);
    }
}
