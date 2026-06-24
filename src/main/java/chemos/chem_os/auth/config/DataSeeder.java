package chemos.chem_os.auth.config;

import chemos.chem_os.auth.model.Permission;
import chemos.chem_os.auth.model.Role;
import chemos.chem_os.auth.model.User;
import chemos.chem_os.auth.repository.PermissionRepository;
import chemos.chem_os.auth.repository.RoleRepository;
import chemos.chem_os.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:changeme123}")
    private String adminPassword;

    @Value("${app.admin.name:Administrator}")
    private String adminName;

    @Value("${app.admin.email:admin@chemos.com}")
    private String adminEmail;

    // Permission codes are code-owned: tied to @PreAuthorize strings in controllers.
    // Any new @PreAuthorize code must have a matching entry here.
    private static final List<Object[]> ALL_PERMISSIONS = List.of(
            new Object[]{"pur_view",    "PURCHASE_VIEW",    "View Purchases",    "PURCHASES"},
            new Object[]{"pur_create",  "PURCHASE_CREATE",  "Create Purchases",  "PURCHASES"},
            new Object[]{"pur_edit",    "PURCHASE_EDIT",    "Edit Purchases",    "PURCHASES"},
            new Object[]{"pur_approve", "PURCHASE_APPROVE", "Approve Purchases", "PURCHASES"},
            new Object[]{"sale_view",   "SALE_VIEW",        "View Sales",        "SALES"},
            new Object[]{"sale_create", "SALE_CREATE",      "Create Sales",      "SALES"},
            new Object[]{"sale_edit",   "SALE_EDIT",        "Edit Sales",        "SALES"},
            new Object[]{"sale_approve","SALE_APPROVE",     "Approve Sales",     "SALES"},
            new Object[]{"comp_view",   "COMPANY_VIEW",     "View Company",      "COMPANY"},
            new Object[]{"comp_create", "COMPANY_CREATE",   "Create Company",    "COMPANY"},
            new Object[]{"comp_edit",   "COMPANY_EDIT",     "Edit Company",      "COMPANY"},
            new Object[]{"prod_view",   "PRODUCT_VIEW",     "View Products",     "PRODUCTS"},
            new Object[]{"prod_create", "PRODUCT_CREATE",   "Create Products",   "PRODUCTS"},
            new Object[]{"prod_edit",   "PRODUCT_EDIT",     "Edit Products",     "PRODUCTS"},
            new Object[]{"usr_mgmt",    "USER_MANAGEMENT",  "User Management",   "ADMIN"},
            new Object[]{"role_mgmt",   "ROLE_MANAGEMENT",  "Role Management",   "ADMIN"}
    );

    @Override
    @Transactional
    public void run(String... args) {
        seedPermissions();
        seedAdminRole();
        seedAdminUser();
    }

    private void seedPermissions() {
        int created = 0;
        for (Object[] row : ALL_PERMISSIONS) {
            String id   = (String) row[0];
            String code = (String) row[1];
            String name = (String) row[2];
            String mod  = (String) row[3];

            if (permissionRepository.findById(id).isEmpty()) {
                permissionRepository.save(new Permission(id, code, name, mod));
                created++;
            }
        }
        if (created > 0) {
            log.info("Seeded {} new permission(s).", created);
        }
    }

    private void seedAdminRole() {
        if (roleRepository.findById("admin").isEmpty()) {
            Role admin = new Role();
            admin.setId("admin");
            admin.setName("ADMIN");
            admin.setDisplayName("Administrator");
            admin.setSuperRole(true);   // bypasses role_permissions — always resolves to all codes
            admin.setParentRole(null);
            roleRepository.save(admin);
            log.info("Seeded admin role.");
        } else {
            // Ensure existing admin role has superRole=true (migration guard).
            roleRepository.findById("admin").ifPresent(role -> {
                if (!role.isSuperRole()) {
                    role.setSuperRole(true);
                    roleRepository.save(role);
                    log.info("Updated admin role: set superRole=true.");
                }
            });
        }
    }

    private void seedAdminUser() {
        String username = adminUsername.isBlank() ? "admin" : adminUsername;

        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }

        Role adminRole = roleRepository.findById("admin")
                .orElseThrow(() -> new IllegalStateException("Admin role not found — seedAdminRole must run first"));

        String password = adminPassword.isBlank() ? "changeme123" : adminPassword;
        String name     = adminName.isBlank()     ? "Administrator"    : adminName;
        String email    = adminEmail.isBlank()     ? "admin@chemos.com" : adminEmail;

        User admin = new User();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setIsActive(true);
        admin.setName(name);
        admin.setEmail(email);
        admin.setRole(adminRole);

        userRepository.save(admin);
        log.warn("=== Created default admin user: '{}' — change the password immediately! ===", username);
    }
}
