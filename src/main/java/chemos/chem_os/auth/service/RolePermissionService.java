package chemos.chem_os.auth.service;

import chemos.chem_os.auth.dto.PermissionResponse;
import chemos.chem_os.auth.dto.RolePermissionsRequest;
import chemos.chem_os.auth.dto.RoleResponse;
import chemos.chem_os.auth.model.Permission;
import chemos.chem_os.auth.model.Role;
import chemos.chem_os.auth.model.Role.InheritanceMode;
import chemos.chem_os.auth.repository.PermissionRepository;
import chemos.chem_os.auth.repository.RoleRepository;
import chemos.chem_os.services.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PermissionResolverService permissionResolverService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        return permissionRepository.findAll().stream()
                .sorted(Comparator.comparing(Permission::getModule).thenComparing(Permission::getPermissionCode))
                .map(p -> new PermissionResponse(p.getId(), p.getPermissionCode(), p.getDisplayName(), p.getModule()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        return roleRepository.findAll().stream()
                .sorted(Comparator.comparing(Role::getId))
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse getPermissions(String roleId) {
        return toResponse(loadRole(roleId));
    }

    @Transactional
    public RoleResponse updatePermissions(String roleId, RolePermissionsRequest request) {
        Role role = loadRole(roleId);
        if (role.isSuperRole()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Super-role permissions cannot be changed");
        }
        Role parent = request.parentRoleId() == null ? null : loadRole(request.parentRoleId());
        validateHierarchy(role, parent);
        if (request.inheritanceMode() != InheritanceMode.NONE && (parent == null || parent.isSuperRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Inheritance requires a non-super parent role");
        }
        if (request.inheritanceMode() != InheritanceMode.SELECTED
                && !request.selectedInheritedPermissions().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "selectedInheritedPermissions must be empty unless inheritanceMode is SELECTED");
        }
        Set<Permission> direct = loadPermissions(request.permissions());
        Set<Permission> selected = loadPermissions(request.selectedInheritedPermissions());
        if (!selected.isEmpty() && (parent == null || !parent.getPermissions().containsAll(selected))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Selected permissions must belong to the parent's direct permissions");
        }

        RoleResponse before = toResponse(role);
        role.setParentRole(parent);
        role.setInheritanceMode(request.inheritanceMode());
        role.setPermissions(direct);
        role.setInheritedPermissions(selected);
        RoleResponse after = toResponse(roleRepository.save(role));
        auditLogService.log("ROLE_PERMISSIONS_UPDATED", "ROLE", roleId, before, after);
        return after;
    }

    public RoleResponse toResponse(Role role) {
        return new RoleResponse(role.getId(), role.getName(), role.getDisplayName(), role.isSuperRole(),
                role.getParentRole() == null ? null : role.getParentRole().getId(),
                codes(role.getPermissions()), role.getInheritanceMode(), codes(role.getInheritedPermissions()),
                permissionResolverService.resolveInherited(role).stream().sorted().toList(),
                permissionResolverService.resolveRole(role).stream().sorted().toList());
    }

    private List<String> codes(Set<Permission> permissions) {
        return permissions.stream().map(Permission::getPermissionCode).sorted().toList();
    }

    private Role loadRole(String roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found: " + roleId));
    }

    private Set<Permission> loadPermissions(Set<String> codes) {
        Set<Permission> permissions = permissionRepository.findAll().stream()
                .filter(p -> codes.contains(p.getPermissionCode())).collect(Collectors.toSet());
        Set<String> unknown = new HashSet<>(codes);
        permissions.forEach(p -> unknown.remove(p.getPermissionCode()));
        if (!unknown.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown permission codes: " + unknown.stream().sorted().toList());
        }
        return permissions;
    }

    private void validateHierarchy(Role role, Role parent) {
        Set<String> visited = new HashSet<>();
        visited.add(role.getId());
        for (Role current = parent; current != null; current = current.getParentRole()) {
            if (!visited.add(current.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role hierarchy cannot contain a cycle");
            }
        }
    }
}