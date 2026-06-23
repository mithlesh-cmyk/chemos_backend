package chemos.chem_os.auth.config;

import chemos.chem_os.auth.model.Role;
import chemos.chem_os.auth.model.User;
import chemos.chem_os.auth.repository.RoleRepository;
import chemos.chem_os.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:changeme123}")
    private String adminPassword;

    @Value("${app.admin.name:Administrator}")
    private String adminName;

    @Value("${app.admin.email:admin@chemos.com}")
    private String adminEmail;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setId("admin");
                        role.setName("ADMIN");
                        role.setDisplayName("Administrator");
                        return roleRepository.save(role);
                    });

            String username = adminUsername.isBlank() ? "admin" : adminUsername;
            String password = adminPassword.isBlank() ? "changeme123" : adminPassword;
            String name     = adminName.isBlank()     ? "Administrator"    : adminName;
            String email    = adminEmail.isBlank()    ? "admin@chemos.com" : adminEmail;

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
}
