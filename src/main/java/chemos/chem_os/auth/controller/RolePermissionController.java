package chemos.chem_os.auth.controller;

import chemos.chem_os.auth.dto.PermissionResponse;
import chemos.chem_os.auth.dto.RolePermissionsRequest;
import chemos.chem_os.auth.dto.RoleResponse;
import chemos.chem_os.auth.dto.UserPermissionsResponse;
import chemos.chem_os.auth.service.PermissionOverrideService;
import chemos.chem_os.auth.service.RolePermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@PreAuthorize("hasAuthority('ROLE_MANAGEMENT')")
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;
    private final PermissionOverrideService permissionOverrideService;

    @GetMapping("/permissions")
    public List<PermissionResponse> listPermissions() {
        return rolePermissionService.listPermissions();
    }

    @GetMapping("/roles/{roleId}/permissions")
    public RoleResponse getRolePermissions(@PathVariable String roleId) {
        return rolePermissionService.getPermissions(roleId);
    }

    @PutMapping("/roles/{roleId}/permissions")
    public RoleResponse updateRolePermissions(@PathVariable String roleId,
                                              @Valid @RequestBody RolePermissionsRequest request) {
        return rolePermissionService.updatePermissions(roleId, request);
    }

    @GetMapping("/users/{username}/permissions")
    public UserPermissionsResponse getUserPermissions(@PathVariable String username) {
        return permissionOverrideService.getPermissions(username);
    }
}