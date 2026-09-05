package chemos.chem_os.auth.dto;

public record TwoFactorStatusResponse(
        String username,
        boolean totpEnabled,
        int backupCodesRemaining
) {
}
