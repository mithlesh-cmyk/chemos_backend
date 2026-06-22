package chemos.chem_os.auth.service;

import chemos.chem_os.auth.dto.CreateUserRequest;
import chemos.chem_os.auth.dto.LoginRequest;
import chemos.chem_os.auth.dto.LoginResponse;
import chemos.chem_os.auth.dto.UserResponse;
import chemos.chem_os.auth.model.Role;
import chemos.chem_os.auth.model.User;
import chemos.chem_os.auth.repository.RoleRepository;
import chemos.chem_os.auth.repository.UserRepository;
import chemos.chem_os.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!user.getIsActive()) {
            throw new RuntimeException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
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

    public UserResponse toggleUserActive(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsActive(!user.getIsActive());
        return toResponse(userRepository.save(user));
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
