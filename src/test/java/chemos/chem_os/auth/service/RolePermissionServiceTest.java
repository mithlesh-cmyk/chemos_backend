package chemos.chem_os.auth.service;

import chemos.chem_os.auth.dto.RolePermissionsRequest;
import chemos.chem_os.auth.dto.RoleResponse;
import chemos.chem_os.auth.model.Permission;
import chemos.chem_os.auth.model.Role;
import chemos.chem_os.auth.model.Role.InheritanceMode;
import chemos.chem_os.auth.repository.PermissionRepository;
import chemos.chem_os.auth.repository.RoleRepository;
import chemos.chem_os.auth.repository.UserPermissionRestrictionRepository;
import chemos.chem_os.services.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolePermissionServiceTest {
    @Mock RoleRepository roles;
    @Mock PermissionRepository permissions;
    @Mock UserPermissionRestrictionRepository restrictions;
    @Mock AuditLogService audit;
    RolePermissionService service;
    Role child;
    Role parent;
    Permission view = new Permission("pur_view", "PURCHASE_VIEW", "View", "PURCHASES");
    Permission edit = new Permission("pur_edit", "PURCHASE_EDIT", "Edit", "PURCHASES");

    @BeforeEach
    void setUp() {
        service = new RolePermissionService(roles, permissions,
                new PermissionResolverService(permissions, restrictions), audit);
        child = role("pur_exe");
        parent = role("pur_man");
        parent.getPermissions().addAll(Set.of(view, edit));
        when(roles.findById(child.getId())).thenReturn(Optional.of(child));
    }

    @Test
    void savesSelectedInheritanceAndAuditsBeforeAndAfter() {
        when(roles.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(permissions.findAll()).thenReturn(List.of(view, edit));
        when(roles.save(child)).thenReturn(child);
        RoleResponse result = service.updatePermissions(child.getId(), new RolePermissionsRequest(
                parent.getId(), InheritanceMode.SELECTED, Set.of("PURCHASE_VIEW"), Set.of("PURCHASE_EDIT")));
        assertThat(result.permissions()).containsExactly("PURCHASE_VIEW");
        assertThat(result.selectedInheritedPermissions()).containsExactly("PURCHASE_EDIT");
        assertThat(result.effectivePermissions()).containsExactly("PURCHASE_EDIT", "PURCHASE_VIEW");
        verify(audit).log(eq("ROLE_PERMISSIONS_UPDATED"), eq("ROLE"), eq(child.getId()),
                any(RoleResponse.class), eq(result));
    }

    @Test
    void noneReplacesDirectGrantsAndClearsInheritanceWithoutRemovingParent() {
        child.setParentRole(parent);
        child.setInheritanceMode(InheritanceMode.ALL);
        child.getPermissions().add(edit);
        when(roles.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(permissions.findAll()).thenReturn(List.of(view, edit));
        when(roles.save(child)).thenReturn(child);
        RoleResponse result = service.updatePermissions(child.getId(), new RolePermissionsRequest(
                parent.getId(), InheritanceMode.NONE, Set.of("PURCHASE_VIEW"), Set.of()));
        assertThat(result.effectivePermissions()).containsExactly("PURCHASE_VIEW");
        assertThat(child.getParentRole()).isSameAs(parent);
    }

    @Test
    void rejectsUnknownPermissionWithoutSaving() {
        when(permissions.findAll()).thenReturn(List.of(view));
        assertBadRequest(new RolePermissionsRequest(null, InheritanceMode.NONE, Set.of("TYPO"), Set.of()));
    }

    @Test
    void rejectsSelectionNotOwnedByParent() {
        parent.getPermissions().remove(edit);
        when(roles.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(permissions.findAll()).thenReturn(List.of(view, edit));
        assertBadRequest(new RolePermissionsRequest(parent.getId(), InheritanceMode.SELECTED,
                Set.of(), Set.of("PURCHASE_EDIT")));
    }

    @Test
    void rejectsSelectionsOutsideSelectedMode() {
        assertBadRequest(new RolePermissionsRequest(null, InheritanceMode.NONE, Set.of(), Set.of("PURCHASE_EDIT")));
    }

    @Test
    void rejectsInheritanceWithoutParent() {
        assertBadRequest(new RolePermissionsRequest(null, InheritanceMode.ALL, Set.of(), Set.of()));
    }

    @Test
    void rejectsSuperParentInheritance() {
        parent.setSuperRole(true);
        when(roles.findById(parent.getId())).thenReturn(Optional.of(parent));
        assertBadRequest(new RolePermissionsRequest(parent.getId(), InheritanceMode.ALL, Set.of(), Set.of()));
    }

    @Test
    void rejectsSelfParent() {
        assertBadRequest(new RolePermissionsRequest(child.getId(), InheritanceMode.NONE, Set.of(), Set.of()));
    }

    @Test
    void rejectsHierarchyCycle() {
        parent.setParentRole(child);
        when(roles.findById(parent.getId())).thenReturn(Optional.of(parent));
        assertBadRequest(new RolePermissionsRequest(parent.getId(), InheritanceMode.NONE, Set.of(), Set.of()));
    }

    @Test
    void rejectsEditingSuperRole() {
        child.setSuperRole(true);
        assertBadRequest(new RolePermissionsRequest(null, InheritanceMode.NONE, Set.of(), Set.of()));
    }

    @Test
    void missingRoleIsNotFound() {
        when(roles.findById(child.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getPermissions(child.getId()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
    }

    private void assertBadRequest(RolePermissionsRequest request) {
        assertThatThrownBy(() -> service.updatePermissions(child.getId(), request))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
        verify(roles, never()).save(any());
        verifyNoInteractions(audit);
    }

    private Role role(String id) {
        Role role = new Role();
        role.setId(id);
        role.setName(id);
        return role;
    }
}