package chemos.chem_os.auth.controller;

import chemos.chem_os.auth.dto.CreateUserRequest;
import chemos.chem_os.auth.dto.EnrollConfirmRequest;
import chemos.chem_os.auth.dto.EnrollConfirmResponse;
import chemos.chem_os.auth.dto.EnrollInitResponse;
import chemos.chem_os.auth.dto.LoginRequest;
import chemos.chem_os.auth.dto.LoginResponse;
import chemos.chem_os.auth.dto.PreAuthChallengeResponse;
import chemos.chem_os.auth.dto.RoleResponse;
import chemos.chem_os.auth.dto.TwoFactorStatusResponse;
import chemos.chem_os.auth.dto.UpdateUserRequest;
import chemos.chem_os.auth.dto.UserConfigResponse;
import chemos.chem_os.auth.dto.UserResponse;
import chemos.chem_os.auth.dto.VerifyLoginRequest;
import chemos.chem_os.auth.service.AuthService;
import chemos.chem_os.auth.service.TwoFactorAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
        private final TwoFactorAuthService twoFactorAuthService;

    // Step 1 of login: password check only. Returns a pre-auth token — never a full
    // access token. Caller must complete one of the /2fa/** endpoints below next.
    @PostMapping("/login")
    public ResponseEntity<PreAuthChallengeResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // First-time 2FA setup — generates a TOTP secret for the user identified by the
    // pre-auth token and returns it (plus an otpauth:// URI) so the frontend can render a QR code.
    @PostMapping("/2fa/enroll/init")
    public ResponseEntity<EnrollInitResponse> enrollInit(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(twoFactorAuthService.initEnrollment(authHeader));
    }

    // Confirms first-time setup with a code from the authenticator app. On success, activates
    // 2FA, issues one-time backup codes, and returns a full access token.
    @PostMapping("/2fa/enroll/confirm")
    public ResponseEntity<EnrollConfirmResponse> enrollConfirm(@RequestHeader("Authorization") String authHeader,
                                                                @RequestBody EnrollConfirmRequest request) {
        return ResponseEntity.ok(twoFactorAuthService.confirmEnrollment(authHeader, request.code()));
    }

    // Step 2 of login for an already-enrolled user: verifies a TOTP or backup code and
    // returns a full access token.
    @PostMapping("/2fa/login/verify")
    public ResponseEntity<LoginResponse> verifyLogin(@RequestHeader("Authorization") String authHeader,
                                                      @RequestBody VerifyLoginRequest request) {
        return ResponseEntity.ok(twoFactorAuthService.verifyLogin(authHeader, request.code()));
    }

    // Admin-driven recovery for a lost device: clears the target user's 2FA state so they
    // re-enroll on their next login.
    @PreAuthorize("hasAuthority('USER_MANAGEMENT')")
    @PatchMapping("/users/{username}/2fa/reset")
    public ResponseEntity<TwoFactorStatusResponse> resetTwoFactor(@PathVariable String username) {
        return ResponseEntity.ok(twoFactorAuthService.resetTwoFactor(username));
    }

    // Admin endpoint to unlock a user's 2FA if they're locked out due to too many failed attempts
    @PreAuthorize("hasAuthority('USER_MANAGEMENT')")
    @PatchMapping("/users/{username}/2fa/unlock")
    public ResponseEntity<TwoFactorStatusResponse> unlockTwoFactor(@PathVariable String username) {
        return ResponseEntity.ok(twoFactorAuthService.unlockTwoFactor(username));
    }

    // Admin endpoint to check a user's 2FA status (enabled, backup codes remaining, lockout status)
    @PreAuthorize("hasAuthority('USER_MANAGEMENT')")
    @GetMapping("/users/{username}/2fa/status")
    public ResponseEntity<TwoFactorStatusResponse> getTwoFactorStatus(@PathVariable String username) {
        return ResponseEntity.ok(twoFactorAuthService.getTwoFactorStatus(username));
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
    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(authService.getUserById(id));
    }

    @PreAuthorize("hasAuthority('USER_MANAGEMENT')")
    @PatchMapping("/users/{username}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable String username,
                                                    @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(authService.updateUser(username, request));
    }

    @PreAuthorize("hasAuthority('USER_MANAGEMENT')")
    @PatchMapping("/users/{username}/toggle")
    public ResponseEntity<UserResponse> toggleActive(@PathVariable String username) {
        return ResponseEntity.ok(authService.toggleUserActive(username));
    }

    @PreAuthorize("hasAuthority('USER_MANAGEMENT')")
    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponse>> listRoles() {
        return ResponseEntity.ok(authService.listRoles());
    }
}
