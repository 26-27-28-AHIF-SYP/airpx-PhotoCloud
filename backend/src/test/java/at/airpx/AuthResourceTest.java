package at.airpx;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(BackendTestProfile.class)
class AuthResourceTest {

    @Test
    void loginReturnsBearerToken() {
        given().contentType("application/json")
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
        given().contentType("application/json")
                .body("""
                        {"email":"test@example.com","password":"wrong password"}
                        """)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(401);
    }

    @Test
    void registerCreatesUser() {
        given().contentType("application/json")
                .body("""
                        {
                        "username": "newuser",
                        "email": "newuser@example.com",
                        "password": "secretPassword123!"}
                        """)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(201)
                .body("username", equalTo("newuser"))
                .body("email", equalTo("newuser@example.com"))
                .body("password", nullValue());
    }

    @Test
    void registeredUserCanLogin() {
        given().contentType("application/json")
                .body("""
                        {
                        "username": "loginuser",
                        "email": "loginuser@example.com",
                        "password": "securePassword123"}
                        """)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(201);

        given().contentType("application/json")
                .body("""
                        {
                        "email": "loginuser@example.com",
                        "password": "securePassword123"}
                        """)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("tokenType", equalTo("Bearer"));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        given().contentType("application/json")
                .body("""
                        {
                        "username": "duplicateuser",
                        "email": "duplicateuser@example.com",
                        "password": "secretPassword123"}
                        """)
                .when()
                .post("api/auth/register")
                .then()
                .statusCode(201);

        given().contentType("application/json")
                .body("""
                        {
                        "username": "anotheruser",
                        "email": "duplicateuser@example.com",
                        "password": "secretPassword123"}
                        """)
                .when()
                .post("api/auth/register")
                .then()
                .statusCode(409);
    }

    @Test
    void registerRejectsInvalidRequest() {
        given().contentType("application/json")
                .body("""
                        {
                        "username": "",
                        "email": "correctemail@example.com",
                        "password": "secretPassword123"
                        }
                        """)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(400);

        given().contentType("application/json")
                .body("""
                        {
                        "username": "invalidrequestuser",
                        "email": "not-an-email",
                        "password": "secretPassword123"
                        }
                        """)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(400);

        given().contentType("application/json")
                .body("""
                        {
                        "username": "invalidrequestuser",
                        "email": "correctemail@example.com",
                        "password": "short"
                        }
                        """)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(400);
    }
}
