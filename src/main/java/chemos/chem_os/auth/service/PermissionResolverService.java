package chemos.chem_os.auth.service;

import chemos.chem_os.auth.model.Permission;
import chemos.chem_os.auth.model.Role;
import chemos.chem_os.auth.model.User;
import chemos.chem_os.auth.repository.PermissionRepository;
import chemos.chem_os.auth.repository.UserPermissionRestrictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionResolverService {

    private final PermissionRepository permissionRepository;
    private final UserPermissionRestrictionRepository restrictionRepository;

    // Resolves the effective permission codes for a user.
    //
    // Super roles (e.g., ADMIN): all permission codes are returned, enabling
    // uniform audit logging — every admin action maps to a real permission code
    // in the security context.
    //
    // Regular roles: own permissions ∪ direct parent's permissions (1 level up).
    // Super role parents are skipped — inheritance stops before reaching admin.
    // User-level restrictions (deny-list) are then subtracted from the result.
    @Transactional(readOnly = true)
    public Set<String> resolve(User user) {
        Role role = user.getRole();

        if (role.isSuperRole()) {
            return permissionRepository.findAll().stream()
                    .map(Permission::getPermissionCode)
                    .collect(Collectors.toSet());
        }

        Set<String> effective = new HashSet<>();

        role.getPermissions().forEach(p -> effective.add(p.getPermissionCode()));

        // 1 level up: include parent's permissions, but never traverse into a super role.
        // This prevents children of admin from inheriting all permissions.
        Role parent = role.getParentRole();
        if (parent != null && !parent.isSuperRole()) {
            parent.getPermissions().forEach(p -> effective.add(p.getPermissionCode()));
        }

        // Subtract user-level restrictions (deny-list).
        restrictionRepository.findByUserId(user.getId())
                .forEach(r -> effective.remove(r.getPermission().getPermissionCode()));

        return effective;
    }
}
