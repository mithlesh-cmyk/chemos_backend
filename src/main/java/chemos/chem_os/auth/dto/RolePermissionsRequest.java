package chemos.chem_os.auth.dto;

import chemos.chem_os.auth.model.Role.InheritanceMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record RolePermissionsRequest(
        String parentRoleId,
        @NotNull InheritanceMode inheritanceMode,
        @NotNull Set<@NotBlank String> permissions,
        @NotNull Set<@NotBlank String> selectedInheritedPermissions
) {}