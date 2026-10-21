package chemos.chem_os.auth.dto;

import java.util.List;

// backupCodes is populated ONLY on this response — the only point in the whole flow where
// plaintext backup codes exist outside the user's authenticator/password manager.
public record EnrollConfirmResponse(
        String token,
        String username,
        String role,
        List<String> backupCodes
) {
}
