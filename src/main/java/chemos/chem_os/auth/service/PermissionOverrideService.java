package chemos.chem_os.auth.service;

import chemos.chem_os.auth.dto.PermissionOverrideRequest;
import chemos.chem_os.auth.dto.PermissionOverrideResponse;
import chemos.chem_os.auth.model.Permission;
import chemos.chem_os.auth.model.User;
import chemos.chem_os.auth.model.UserPermissionRestriction;
import chemos.chem_os.auth.repository.PermissionRepository;
import chemos.chem_os.auth.repository.UserPermissionRestrictionRepository;
import chemos.chem_os.auth.repository.UserRepository;
import chemos.chem_os.services.AuditLogService;
import chemos.chem_os.services.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

// Lets a caller who already holds a permission delegate it (ALLOW) or revoke it (DENY) for one
// specific other user, on top of whatever that user's role normally grants/denies. See
// PermissionResolverService.resolve() for how overrides combine with role permissions.
@Service
@RequiredArgsConstructor
public class PermissionOverrideService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final UserPermissionRestrictionRepository restrictionRepository;
    private final PermissionResolverService permissionResolverService;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;

    @Transactional
    public PermissionOverrideResponse setOverride(String username, String permissionCode, PermissionOverrideRequest request) {
        User target = loadUser(username);
        Permission permission = loadPermission(permissionCode);

        UserPermissionRestriction override = restrictionRepository
                .findByUserIdAndPermissionId(target.getId(), permission.getId())
                .orElseGet(UserPermissionRestriction::new);

        Map<String, Object> before = override.getId() != null
                ? Map.of("effect", override.getEffect().name(), "reason", String.valueOf(override.getReason()))
                : null;

        override.setUser(target);
        override.setPermission(permission);
        override.setEffect(request.effect());
        override.setReason(request.reason());
        override.setRestrictedBy(loadUser(currentUserService.getUsername()));

        UserPermissionRestriction saved = restrictionRepository.save(override);

        auditLogService.log("PERMISSION_OVERRIDE_SET", "USER_PERMISSION_OVERRIDE",
                target.getId() + ":" + permission.getId(), before,
                Map.of("effect", saved.getEffect().name(), "reason", String.valueOf(saved.getReason())));

        return toResponse(target, saved);
    }

    @Transactional
    public void removeOverride(String username, String permissionCode) {
        User target = loadUser(username);
        Permission permission = loadPermission(permissionCode);

        UserPermissionRestriction override = restrictionRepository
                .findByUserIdAndPermissionId(target.getId(), permission.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No override exists for " + username + "/" + permissionCode));

        restrictionRepository.delete(override);

        auditLogService.log("PERMISSION_OVERRIDE_REMOVED", "USER_PERMISSION_OVERRIDE",
                target.getId() + ":" + permission.getId(),
                Map.of("effect", override.getEffect().name()), null);
    }

    @Transactional(readOnly = true)
    public List<PermissionOverrideResponse> listOverrides(String username) {
        User target = loadUser(username);
        User caller = loadUser(currentUserService.getUsername());
        Set<String> callerPermissions = permissionResolverService.resolve(caller);

        return restrictionRepository.findByUserId(target.getId()).stream()
                .filter(o -> callerPermissions.contains(o.getPermission().getPermissionCode()))
                .map(o -> toResponse(target, o))
                .toList();
    }

    private User loadUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + username));
    }

    private Permission loadPermission(String code) {
        return permissionRepository.findByPermissionCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown permission code: " + code));
    }

    private PermissionOverrideResponse toResponse(User target, UserPermissionRestriction o) {
        return new PermissionOverrideResponse(
                target.getUsername(),
                o.getPermission().getPermissionCode(),
                o.getPermission().getDisplayName(),
                o.getEffect().name(),
                o.getReason()
        );
    }
}
