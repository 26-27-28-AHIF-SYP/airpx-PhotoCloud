package at.airpx.model.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "email is required") @Email(message = "email must be in a correct format") String email,

        @NotBlank(message = "password is required") String password) {}
