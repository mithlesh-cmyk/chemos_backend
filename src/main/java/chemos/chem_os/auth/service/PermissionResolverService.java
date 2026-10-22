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
    // Regular roles: direct permissions plus explicitly configured inheritance.
    // User ALLOW overrides add permissions; DENY overrides always win.
    @Transactional(readOnly = true)
    public Set<String> resolve(User user) {
        Set<String> effective = resolveRole(user.getRole());
        if (user.getRole().isSuperRole()) {
            return effective;
        }

        List<UserPermissionRestriction> overrides = restrictionRepository.findByUserId(user.getId());
        overrides.stream()
                .filter(o -> o.getEffect() == OverrideEffect.ALLOW)
                .forEach(o -> effective.add(o.getPermission().getPermissionCode()));
        overrides.stream()
                .filter(o -> o.getEffect() == OverrideEffect.DENY)
                .forEach(o -> effective.remove(o.getPermission().getPermissionCode()));

        return effective;
    }

    @Transactional(readOnly = true)
    public Set<String> resolveRole(Role role) {
        if (role.isSuperRole()) {
            return permissionRepository.findAll().stream()
                    .map(Permission::getPermissionCode)
                    .collect(Collectors.toSet());
        }

        Set<String> effective = new HashSet<>();

        role.getPermissions().forEach(p -> effective.add(p.getPermissionCode()));

        effective.addAll(resolveInherited(role));
        return effective;
    }

    @Transactional(readOnly = true)
    public Set<String> resolveInherited(Role role) {
        Role parent = role.getParentRole();
        if (role.isSuperRole() || parent == null || parent.isSuperRole()
                || role.getInheritanceMode() == Role.InheritanceMode.NONE) {
            return Set.of();
        }

        return parent.getPermissions().stream()
                .filter(p -> role.getInheritanceMode() == Role.InheritanceMode.ALL
                        || role.getInheritedPermissions().contains(p))
                .map(Permission::getPermissionCode)
                .collect(Collectors.toSet());
    }
}
