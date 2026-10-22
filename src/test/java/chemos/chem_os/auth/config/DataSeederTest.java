package chemos.chem_os.auth.config;

import chemos.chem_os.auth.model.Permission;
import chemos.chem_os.auth.model.Role;
import chemos.chem_os.auth.model.User;
import chemos.chem_os.auth.repository.PermissionRepository;
import chemos.chem_os.auth.repository.RoleRepository;
import chemos.chem_os.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {
    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock PermissionRepository permissions;
    @Mock PasswordEncoder passwords;
    DataSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new DataSeeder(users, roles, permissions, passwords);
        ReflectionTestUtils.setField(seeder, "adminUsername", "admin");
        when(users.findByUsername("admin")).thenReturn(Optional.of(new User()));
        Role admin = new Role();
        admin.setSuperRole(true);
        when(roles.findById("admin")).thenReturn(Optional.of(admin));
        when(permissions.findById(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return Optional.of(new Permission(id, id, id, "TEST"));
        });
    }

    @Test
    void existingRolesAreNeverGrantedRemovedPermissionsOnRestart() {
        Role existing = new Role();
        when(roles.findByName(anyString())).thenReturn(Optional.of(existing));
        seeder.run();
        assertThat(existing.getPermissions()).isEmpty();
        verify(roles, never()).save(any());
    }

    @Test
    void newManagerRolesReceiveInitialDefaultsOnly() {
        seeder.run();
        verify(roles).save(argThat(role -> role.getId().equals("pur_man")
                && role.getPermissions().stream().anyMatch(p -> p.getId().equals("pur_edit"))));
        verify(roles).save(argThat(role -> role.getId().equals("sal_man")
                && role.getPermissions().stream().anyMatch(p -> p.getId().equals("sale_edit"))));
    }
}