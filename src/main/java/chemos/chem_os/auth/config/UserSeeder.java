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
                    .orElseThrow(() -> new IllegalStateException(
                            "ADMIN role not found in DB. Make sure the roles table has a row with name='ADMIN'."));

            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setIsActive(true);
            admin.setRole(adminRole);

            userRepository.save(admin);
            log.warn("=== Created default admin user: '{}' — change the password immediately! ===", adminUsername);
        }
    }
}
