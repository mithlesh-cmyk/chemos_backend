package chemos.chem_os.auth.dto;

public record PermissionOverrideResponse(
        String username,
        String permissionCode,
        String displayName,
        String effect,
        String reason
) {
}
