package chemos.chem_os.auth.dto;

import java.util.List;

public record UserPermissionsResponse(
        String username,
        RoleResponse role,
        List<PermissionOverrideResponse> overrides,
        List<String> effectivePermissions
) {}