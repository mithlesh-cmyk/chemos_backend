package chemos.chem_os.auth.dto;

import chemos.chem_os.auth.model.Role.InheritanceMode;
import java.util.List;

public record RoleResponse(
        String id,
        String name,
        String displayName,
        boolean isSuperRole,
        String parentRoleId,
        List<String> permissions,
        InheritanceMode inheritanceMode,
        List<String> selectedInheritedPermissions,
        List<String> inheritedPermissions,
        List<String> effectivePermissions
) {}
