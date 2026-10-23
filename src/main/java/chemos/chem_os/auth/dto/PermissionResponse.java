package chemos.chem_os.auth.dto;

public record PermissionResponse(
        String id,
        String permissionCode,
        String displayName,
        String module
) {}