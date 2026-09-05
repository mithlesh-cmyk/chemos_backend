package chemos.chem_os.auth.service;

import chemos.chem_os.auth.dto.LoginRequest;
import chemos.chem_os.auth.dto.PreAuthChallengeResponse;
import chemos.chem_os.auth.model.Role;
import chemos.chem_os.auth.model.TwoFactorCredential;
import chemos.chem_os.auth.model.User;
import chemos.chem_os.auth.repository.RoleRepository;
import chemos.chem_os.auth.repository.TwoFactorCredentialRepository;
import chemos.chem_os.auth.repository.UserRepository;
import chemos.chem_os.auth.security.JwtService;
import chemos.chem_os.auth.security.LoginRateLimiter;
import chemos.chem_os.services.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PermissionResolverService permissionResolverService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private TwoFactorCredentialRepository twoFactorCredentialRepository;

    private LoginRateLimiter loginRateLimiter;
    private AuthService authService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        loginRateLimiter = new LoginRateLimiter();
        ReflectionTestUtils.setField(loginRateLimiter, "maxAttempts", 3);
        ReflectionTestUtils.setField(loginRateLimiter, "lockoutDurationMinutes", 15L);

        authService = new AuthService(
                userRepository,
                roleRepository,
                jwtService,
                passwordEncoder,
                permissionResolverService,
                auditLogService,
                twoFactorCredentialRepository,
                loginRateLimiter
        );

        Role role = new Role();
        role.setId("admin");
        role.setName("ADMIN");
        role.setDisplayName("Administrator");

        activeUser = new User();
        activeUser.setId(UUID.randomUUID());
        activeUser.setUsername("alice");
        activeUser.setPassword("hashed-password");
        activeUser.setIsActive(true);
        activeUser.setRole(role);
    }

    @Test
    void loginWithNoTwoFactorEnrollmentRequiresEnrollment() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("correct-password", "hashed-password")).thenReturn(true);
        when(twoFactorCredentialRepository.findByUserId(activeUser.getId())).thenReturn(Optional.empty());
        when(jwtService.generatePreAuthToken("alice")).thenReturn("pre-auth-token");

        PreAuthChallengeResponse response = authService.login(new LoginRequest("alice", "correct-password"));

        assertThat(response.status()).isEqualTo("ENROLLMENT_REQUIRED");
        assertThat(response.preAuthToken()).isEqualTo("pre-auth-token");
        assertThat(response.username()).isEqualTo("alice");
    }

    @Test
    void loginWithEnabledTwoFactorRequiresVerification() {
        TwoFactorCredential credential = new TwoFactorCredential();
        credential.setEnabled(true);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("correct-password", "hashed-password")).thenReturn(true);
        when(twoFactorCredentialRepository.findByUserId(activeUser.getId())).thenReturn(Optional.of(credential));
        when(jwtService.generatePreAuthToken("alice")).thenReturn("pre-auth-token");

        PreAuthChallengeResponse response = authService.login(new LoginRequest("alice", "correct-password"));

        assertThat(response.status()).isEqualTo("VERIFICATION_REQUIRED");
    }

    @Test
    void loginNeverIssuesAFullAccessToken() {
        // Regression guard: PreAuthChallengeResponse must never carry role/authority
        // information a client could mistake for a usable access token.
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(twoFactorCredentialRepository.findByUserId(activeUser.getId())).thenReturn(Optional.empty());
        when(jwtService.generatePreAuthToken("alice")).thenReturn("pre-auth-token");

        authService.login(new LoginRequest("alice", "correct-password"));

        verify(jwtService).generatePreAuthToken("alice");
        verify(jwtService, org.mockito.Mockito.never()).generateToken(any(), any());
    }

    @Test
    void loginRejectsUnknownUsername() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "whatever")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void loginRejectsWrongPassword() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong-password")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void loginRejectsDisabledAccount() {
        activeUser.setIsActive(false);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "correct-password")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Account is disabled");
    }

    @Test
    void loginLocksOutAfterRepeatedFailedPasswordAttempts() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong-password")))
                    .isInstanceOf(ResponseStatusException.class);
        }

        // 4th attempt is blocked by the rate limiter before the password is even checked,
        // even if the caller now supplies the correct password.
        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "correct-password")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Too many failed login attempts");
    }

    @Test
    void successfulLoginResetsPriorFailureCount() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);
        when(passwordEncoder.matches("correct-password", "hashed-password")).thenReturn(true);
        when(twoFactorCredentialRepository.findByUserId(activeUser.getId())).thenReturn(Optional.empty());
        when(jwtService.generatePreAuthToken("alice")).thenReturn("pre-auth-token");

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong-password")))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong-password")))
                .isInstanceOf(ResponseStatusException.class);

        // A success in between should reset the failure counter.
        authService.login(new LoginRequest("alice", "correct-password"));

        // Two more failures post-reset -- still below maxAttempts=3, so not locked out yet.
        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong-password")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid username or password");
    }
}
