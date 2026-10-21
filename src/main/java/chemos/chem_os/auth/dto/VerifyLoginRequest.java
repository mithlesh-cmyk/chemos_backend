package chemos.chem_os.auth.dto;

// code may be a 6-digit TOTP code or a backup code (e.g. "AB12-CD34")
public record VerifyLoginRequest(
        String code
) {
}
