package at.airpx.model.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "username is required") String username,

        @NotBlank(message = "username is required") @Email(message = "email must be in a correct format") String email,

        @NotBlank(message = "password is required") @Size(min = 8) // Passwort muss mindestens 8 Zeichen lang sein
        String password) {}
