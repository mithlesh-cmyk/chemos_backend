package chemos.chem_os.auth.controller;

import chemos.chem_os.auth.dto.CreateUserRequest;
import chemos.chem_os.auth.dto.LoginRequest;
import chemos.chem_os.auth.dto.LoginResponse;
import chemos.chem_os.auth.dto.UserConfigResponse;
import chemos.chem_os.auth.dto.UserResponse;
import chemos.chem_os.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Returns the caller's effective permissions and pre-computed module config flags.
    // Frontend uses this after login to drive UI (show/hide routes, buttons, menus).
    // Backend still enforces via @PreAuthorize — this config is a UX guide, not the security layer.
    @GetMapping("/me")
    public ResponseEntity<UserConfigResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.getUserConfig(authentication.getName()));
    }

    @PreAuthorize("hasAuthority('USER_MANAGEMENT')")
    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(authService.createUser(request));
    }

    @PreAuthorize("hasAuthority('USER_MANAGEMENT')")
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(authService.listUsers());
    }

    @PreAuthorize("hasAuthority('USER_MANAGEMENT')")
    @PatchMapping("/users/{username}/toggle")
    public ResponseEntity<UserResponse> toggleActive(@PathVariable String username) {
        return ResponseEntity.ok(authService.toggleUserActive(username));
    }
}
