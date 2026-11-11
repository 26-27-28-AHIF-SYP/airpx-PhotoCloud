package at.airpx;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.matchesPattern;

@QuarkusTest
class AuthResourceTest {

    @Test
    void loginReturnsBearerToken() {
        given()
                .contentType("application/json")
                .body("""
                        {"email":"TEST@example.com","password":"correct horse battery staple"}
                        """)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("tokenType", org.hamcrest.Matchers.equalTo("Bearer"))
                .body("accessToken", matchesPattern("[A-Za-z0-9_-]+"))
                .body("expiresAt", org.hamcrest.Matchers.notNullValue());
    }

    @Test
    void loginRejectsInvalidPassword() {
        given()
                .contentType("application/json")
                .body("""
                        {"email":"test@example.com","password":"wrong password"}
                        """)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(401);
    }
}
