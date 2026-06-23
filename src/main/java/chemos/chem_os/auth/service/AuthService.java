package chemos.chem_os.auth.service;

import chemos.chem_os.auth.dto.CreateUserRequest;
import chemos.chem_os.auth.dto.LoginRequest;
import chemos.chem_os.auth.dto.LoginResponse;
import chemos.chem_os.auth.dto.RoleResponse;
import chemos.chem_os.auth.dto.UserConfigResponse;
import chemos.chem_os.auth.dto.UserConfigResponse.ModuleAccess;
import chemos.chem_os.auth.dto.UserConfigResponse.ModulesConfig;
import chemos.chem_os.auth.dto.UserConfigResponse.UserInfo;
import chemos.chem_os.auth.dto.UserResponse;
import chemos.chem_os.auth.model.Role;
import chemos.chem_os.auth.model.User;
import chemos.chem_os.auth.repository.RoleRepository;
import chemos.chem_os.auth.repository.UserRepository;
import chemos.chem_os.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final PermissionResolverService permissionResolverService;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (!user.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is disabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }


        String token = jwtService.generateToken(user.getUsername(), user.getRole().getName());

        return new LoginResponse(token, user.getUsername(), user.getRole().getName());
    }

    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new RuntimeException("Role not found: " + request.roleId()));

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setIsActive(true);
        user.setRole(role);
        user.setName(request.name());
        user.setEmail(request.email());

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<RoleResponse> listRoles() {
        return roleRepository.findAll().stream()
                .map(r -> new RoleResponse(r.getId(), r.getName(), r.getDisplayName()))
                .toList();
    }

    public UserResponse toggleUserActive(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsActive(!user.getIsActive());
        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserConfigResponse getUserConfig(String username) {
        User user = userRepository.findByUsernameWithPermissions(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Set<String> perms = permissionResolverService.resolve(user);

        UserInfo userInfo = new UserInfo(
                user.getUsername(),
                user.getName(),
                user.getRole().getName(),
                user.getRole().getDisplayName()
        );

        ModulesConfig modules = new ModulesConfig(
                new ModuleAccess(
                        perms.contains("SALE_VIEW"),
                        perms.contains("SALE_CREATE"),
                        perms.contains("SALE_EDIT"),
                        perms.contains("SALE_APPROVE")
                ),
                new ModuleAccess(
                        perms.contains("PURCHASE_VIEW"),
                        perms.contains("PURCHASE_CREATE"),
                        perms.contains("PURCHASE_EDIT"),
                        perms.contains("PURCHASE_APPROVE")
                ),
                new ModuleAccess(
                        perms.contains("COMPANY_VIEW"),
                        perms.contains("COMPANY_CREATE"),
                        perms.contains("COMPANY_EDIT"),
                        false
                ),
                new ModuleAccess(
                        perms.contains("PRODUCT_VIEW"),
                        perms.contains("PRODUCT_CREATE"),
                        perms.contains("PRODUCT_EDIT"),
                        false
                )
        );

        return new UserConfigResponse(userInfo, new ArrayList<>(perms), modules);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getIsActive(),
                user.getRole().getName(),
                user.getRole().getDisplayName()
        );
    }
}
