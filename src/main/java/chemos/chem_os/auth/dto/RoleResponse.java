package chemos.chem_os.auth.dto;

import java.util.List;

public record RoleResponse(
        String id,
        String name,
        String displayName,
        boolean isSuperRole,
        String parentRoleId,       // role this one inherits from (1 level), null if none
        List<String> permissions   // this role's own permission codes, from role_permissions — excludes inherited ones
) {}
