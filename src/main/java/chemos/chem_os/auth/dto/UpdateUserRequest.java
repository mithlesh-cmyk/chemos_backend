package chemos.chem_os.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank String roleId,
        @NotBlank String name,
        @NotBlank @Email String email,
        // Optional: only set when the caller wants to reset the user's password.
        // Left null/blank to leave the existing password untouched.
        @Size(min = 6, message = "Password must be at least 6 characters")
        String newPassword
) {
}
