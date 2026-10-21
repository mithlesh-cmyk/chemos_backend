package chemos.chem_os.auth.service;

import chemos.chem_os.auth.model.Permission;
import chemos.chem_os.auth.model.Role;
import chemos.chem_os.auth.model.User;
import chemos.chem_os.auth.model.UserPermissionRestriction;
import chemos.chem_os.auth.model.UserPermissionRestriction.OverrideEffect;
import chemos.chem_os.auth.repository.PermissionRepository;
import chemos.chem_os.auth.repository.UserPermissionRestrictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
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

        // Apply user-level overrides on top of the role: ALLOW first (grants beyond the role,
        // e.g. a manager delegating one permission to one executive), then DENY (always wins,
        // even over an ALLOW — shouldn't co-occur for the same code given the unique constraint,
        // but the ordering is a defensive invariant either way).
        List<UserPermissionRestriction> overrides = restrictionRepository.findByUserId(user.getId());
        overrides.stream()
                .filter(o -> o.getEffect() == OverrideEffect.ALLOW)
                .forEach(o -> effective.add(o.getPermission().getPermissionCode()));
        overrides.stream()
                .filter(o -> o.getEffect() == OverrideEffect.DENY)
                .forEach(o -> effective.remove(o.getPermission().getPermissionCode()));

        return effective;
    }
}
