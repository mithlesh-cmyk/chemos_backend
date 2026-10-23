package chemos.chem_os.auth.service;

import chemos.chem_os.auth.model.Permission;
import chemos.chem_os.auth.model.Role;
import chemos.chem_os.auth.model.Role.InheritanceMode;
import chemos.chem_os.auth.model.User;
import chemos.chem_os.auth.model.UserPermissionRestriction;
import chemos.chem_os.auth.model.UserPermissionRestriction.OverrideEffect;
import chemos.chem_os.auth.repository.PermissionRepository;
import chemos.chem_os.auth.repository.UserPermissionRestrictionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionResolverServiceTest {
    @Mock PermissionRepository permissions;
    @Mock UserPermissionRestrictionRepository restrictions;
    PermissionResolverService resolver;
    User user;
    Role child;
    Role parent;

    @BeforeEach
    void setUp() {
        resolver = new PermissionResolverService(permissions, restrictions);
        parent = new Role();
        child = new Role();
        child.setParentRole(parent);
        user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(child);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PURCHASE", "SALE"})
    void parentDoesNotGrantAnythingByDefault(String module) {
        child.getPermissions().add(permission(module + "_VIEW"));
        parent.getPermissions().addAll(Set.of(permission(module + "_VIEW"), permission(module + "_EDIT"),
                permission(module + "_APPROVE")));
        assertThat(resolver.resolve(user)).containsExactly(module + "_VIEW");
    }

    @Test
    void allInheritsOnlyDirectParentPermissions() {
        Role grandparent = new Role();
        grandparent.getPermissions().add(permission("USER_MANAGEMENT"));
        parent.setParentRole(grandparent);
        parent.setInheritanceMode(InheritanceMode.ALL);
        parent.getPermissions().add(permission("PURCHASE_EDIT"));
        child.setInheritanceMode(InheritanceMode.ALL);
        child.getPermissions().add(permission("PURCHASE_CREATE"));
        assertThat(resolver.resolve(user)).containsExactlyInAnyOrder("PURCHASE_CREATE", "PURCHASE_EDIT");
    }

    @Test
    void selectedInheritsOnlyPermissionsStillGrantedDirectlyToParent() {
        child.setInheritanceMode(InheritanceMode.SELECTED);
        child.getInheritedPermissions().addAll(Set.of(permission("PURCHASE_VIEW"), permission("PURCHASE_EDIT")));
        parent.getPermissions().addAll(Set.of(permission("PURCHASE_VIEW"), permission("PURCHASE_APPROVE")));
        assertThat(resolver.resolve(user)).containsExactly("PURCHASE_VIEW");
    }

    @Test
    void superParentCannotLeakPermissions() {
        parent.setSuperRole(true);
        parent.getPermissions().add(permission("ROLE_MANAGEMENT"));
        child.setInheritanceMode(InheritanceMode.ALL);
        assertThat(resolver.resolve(user)).isEmpty();
    }

    @Test
    void overridesAreUserSpecificAndDenyWins() {
        child.getPermissions().add(permission("PURCHASE_VIEW"));
        when(restrictions.findByUserId(user.getId())).thenReturn(List.of(
                override("PURCHASE_EDIT", OverrideEffect.ALLOW),
                override("PURCHASE_VIEW", OverrideEffect.DENY),
                override("PURCHASE_EDIT", OverrideEffect.DENY)));
        assertThat(resolver.resolve(user)).isEmpty();

        User other = new User();
        other.setId(UUID.randomUUID());
        other.setRole(child);
        assertThat(resolver.resolve(other)).containsExactly("PURCHASE_VIEW");
    }

    @Test
    void allowCanAddOnePermissionWithoutChangingRole() {
        when(restrictions.findByUserId(user.getId()))
                .thenReturn(List.of(override("SALE_EDIT", OverrideEffect.ALLOW)));
        assertThat(resolver.resolve(user)).containsExactly("SALE_EDIT");
        assertThat(child.getPermissions()).isEmpty();
    }

    @Test
    void superRolesKeepAllPermissionsWithoutOverrides() {
        child.setSuperRole(true);
        when(permissions.findAll()).thenReturn(List.of(permission("ROLE_MANAGEMENT"), permission("PURCHASE_EDIT")));
        assertThat(resolver.resolve(user)).containsExactlyInAnyOrder("ROLE_MANAGEMENT", "PURCHASE_EDIT");
        verifyNoInteractions(restrictions);
    }

    @Test
    void denyRemovesInheritedPermissionAndRemovalRestoresRoleDefault() {
        child.setInheritanceMode(InheritanceMode.ALL);
        parent.getPermissions().add(permission("PURCHASE_EDIT"));
        when(restrictions.findByUserId(user.getId())).thenReturn(
                List.of(override("PURCHASE_EDIT", OverrideEffect.DENY)), List.of());
        assertThat(resolver.resolve(user)).isEmpty();
        assertThat(resolver.resolve(user)).containsExactly("PURCHASE_EDIT");
    }

    private Permission permission(String code) {
        return new Permission(code, code, code, "TEST");
    }

    private UserPermissionRestriction override(String code, OverrideEffect effect) {
        UserPermissionRestriction override = new UserPermissionRestriction();
        override.setPermission(permission(code));
        override.setEffect(effect);
        return override;
    }
}