package chemos.chem_os.auth.service;

import chemos.chem_os.auth.dto.PermissionOverrideRequest;
import chemos.chem_os.auth.model.Permission;
import chemos.chem_os.auth.model.Role;
import chemos.chem_os.auth.model.User;
import chemos.chem_os.auth.model.UserPermissionRestriction;
import chemos.chem_os.auth.model.UserPermissionRestriction.OverrideEffect;
import chemos.chem_os.auth.repository.PermissionRepository;
import chemos.chem_os.auth.repository.UserPermissionRestrictionRepository;
import chemos.chem_os.auth.repository.UserRepository;
import chemos.chem_os.services.AuditLogService;
import chemos.chem_os.services.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionOverrideServiceTest {
    @Mock UserRepository users;
    @Mock PermissionRepository permissions;
    @Mock UserPermissionRestrictionRepository overrides;
    @Mock PermissionResolverService resolver;
    @Mock AuditLogService audit;
    @Mock CurrentUserService currentUser;
    @Mock RolePermissionService roleService;
    PermissionOverrideService service;
    User target;
    Permission edit = new Permission("pur_edit", "PURCHASE_EDIT", "Edit", "PURCHASES");

    @BeforeEach
    void setUp() {
        service = new PermissionOverrideService(users, permissions, overrides, resolver, audit, currentUser, roleService);
        target = new User();
        target.setId(UUID.randomUUID());
        target.setUsername("daisy10");
        target.setRole(new Role());
        when(users.findByUsername(target.getUsername())).thenReturn(Optional.of(target));
    }

    @Test
    void setsAndReplacesOverrideForOnlyTargetUser() {
        User admin = new User();
        when(currentUser.getUsername()).thenReturn("admin");
        when(users.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(permissions.findByPermissionCode("PURCHASE_EDIT")).thenReturn(Optional.of(edit));
        UserPermissionRestriction existing = new UserPermissionRestriction();
        existing.setId(1L);
        existing.setEffect(OverrideEffect.ALLOW);
        when(overrides.findByUserIdAndPermissionId(target.getId(), edit.getId())).thenReturn(Optional.of(existing));
        when(overrides.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.setOverride("daisy10", "PURCHASE_EDIT",
                new PermissionOverrideRequest(OverrideEffect.DENY, "No editing"));
        assertThat(response.effect()).isEqualTo("DENY");
        assertThat(existing.getUser()).isSameAs(target);
        assertThat(existing.getRestrictedBy()).isSameAs(admin);
        assertThat(existing.getPermission()).isSameAs(edit);
        verify(audit).log(eq("PERMISSION_OVERRIDE_SET"), eq("USER_PERMISSION_OVERRIDE"), anyString(), any(), any());
    }

    @Test
    void removingOverrideDeletesOnlyThatUserPermission() {
        when(permissions.findByPermissionCode("PURCHASE_EDIT")).thenReturn(Optional.of(edit));
        UserPermissionRestriction existing = new UserPermissionRestriction();
        when(overrides.findByUserIdAndPermissionId(target.getId(), edit.getId())).thenReturn(Optional.of(existing));
        service.removeOverride("daisy10", "PURCHASE_EDIT");
        verify(overrides).delete(existing);
        verify(audit).log(eq("PERMISSION_OVERRIDE_REMOVED"), anyString(), anyString(), any(), isNull());
    }

    @Test
    void listsAllOverridesNotJustPermissionsHeldByCaller() {
        UserPermissionRestriction existing = new UserPermissionRestriction();
        existing.setPermission(edit);
        existing.setEffect(OverrideEffect.DENY);
        when(overrides.findByUserId(target.getId())).thenReturn(List.of(existing));
        assertThat(service.listOverrides("daisy10")).extracting("permissionCode").containsExactly("PURCHASE_EDIT");
        verifyNoInteractions(currentUser, resolver);
    }

    @Test
    void rejectsMeaninglessSuperRoleOverride() {
        target.getRole().setSuperRole(true);
        assertThatThrownBy(() -> service.setOverride("daisy10", "PURCHASE_EDIT",
                new PermissionOverrideRequest(OverrideEffect.DENY, null)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
        verifyNoInteractions(overrides, audit);
    }
}