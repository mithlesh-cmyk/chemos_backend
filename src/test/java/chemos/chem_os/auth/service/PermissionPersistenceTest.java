package chemos.chem_os.auth.service;

import chemos.chem_os.auth.model.Permission;
import chemos.chem_os.auth.model.Role;
import chemos.chem_os.auth.model.User;
import chemos.chem_os.auth.model.UserPermissionRestriction;
import chemos.chem_os.auth.repository.PermissionRepository;
import chemos.chem_os.auth.repository.UserPermissionRestrictionRepository;
import chemos.chem_os.auth.repository.UserRepository;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PermissionPersistenceTest {
    @Test
    void selectedInheritanceAndUserOverridesSurviveSeparateDatabaseSessions() throws Exception {
        String userQuery = UserRepository.class.getMethod("findByUsernameWithPermissions", String.class)
                .getAnnotation(Query.class).value();
        // Explicit in-memory Hibernate setup: no application properties, runners or production datasource.
        try (var factory = new Configuration()
                .addAnnotatedClass(Role.class).addAnnotatedClass(Permission.class)
                .addAnnotatedClass(User.class).addAnnotatedClass(UserPermissionRestriction.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:permissions;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .buildSessionFactory()) {
            factory.inTransaction(session -> {
                Permission view = new Permission("pur_view", "PURCHASE_VIEW", "View", "PURCHASES");
                Permission edit = new Permission("pur_edit", "PURCHASE_EDIT", "Edit", "PURCHASES");
                session.persist(view);
                session.persist(edit);
                Role parent = new Role();
                parent.setId("pur_man");
                parent.setName("PURCHASE_MANAGER");
                parent.getPermissions().add(view);
                parent.getPermissions().add(edit);
                session.persist(parent);
                Role child = new Role();
                child.setId("pur_exe");
                child.setName("PURCHASE_EXECUTIVE");
                child.setParentRole(parent);
                child.setInheritanceMode(Role.InheritanceMode.SELECTED);
                child.getInheritedPermissions().add(view);
                session.persist(child);

                User user = new User();
                user.setUsername("daisy10");
                user.setPassword("test-only");
                user.setRole(child);
                session.persist(user);

                UserPermissionRestriction deny = new UserPermissionRestriction();
                deny.setUser(user);
                deny.setPermission(view);
                deny.setEffect(UserPermissionRestriction.OverrideEffect.DENY);
                session.persist(deny);
                UserPermissionRestriction allow = new UserPermissionRestriction();
                allow.setUser(user);
                allow.setPermission(edit);
                allow.setEffect(UserPermissionRestriction.OverrideEffect.ALLOW);
                session.persist(allow);
            });
            factory.inTransaction(session -> {
                Role child = session.find(Role.class, "pur_exe");
                assertThat(child.getInheritanceMode()).isEqualTo(Role.InheritanceMode.SELECTED);
                assertThat(child.getPermissions()).isEmpty();
                assertThat(child.getInheritedPermissions()).extracting(Permission::getPermissionCode)
                        .containsExactly("PURCHASE_VIEW");
                assertThat(child.getParentRole().getId()).isEqualTo("pur_man");

                User user = session.createQuery(userQuery, User.class)
                        .setParameter("username", "daisy10").getSingleResult();
                var restrictions = mock(UserPermissionRestrictionRepository.class);
                when(restrictions.findByUserId(user.getId())).thenReturn(session.createQuery(
                                "from UserPermissionRestriction r join fetch r.permission where r.user.id = :id",
                                UserPermissionRestriction.class).setParameter("id", user.getId()).getResultList());
                var resolver = new PermissionResolverService(mock(PermissionRepository.class), restrictions);
                assertThat(resolver.resolveRole(user.getRole())).containsExactly("PURCHASE_VIEW");
                assertThat(resolver.resolve(user)).containsExactly("PURCHASE_EDIT");
            });
        }
    }
}