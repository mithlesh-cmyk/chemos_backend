package chemos.chem_os.auth.controller;

import chemos.chem_os.auth.dto.CreateUserRequest;
import chemos.chem_os.auth.dto.LoginRequest;
import chemos.chem_os.auth.dto.LoginResponse;
import chemos.chem_os.auth.dto.UserResponse;
import chemos.chem_os.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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



    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(authService.createUser(request));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(authService.listUsers());
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PatchMapping("/users/{username}/toggle")
    public ResponseEntity<UserResponse> toggleActive(@PathVariable String username) {
        return ResponseEntity.ok(authService.toggleUserActive(username));
    }
}
