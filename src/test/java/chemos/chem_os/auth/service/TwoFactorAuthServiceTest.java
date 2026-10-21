package chemos.chem_os.auth.service;

import chemos.chem_os.auth.dto.EnrollConfirmResponse;
import chemos.chem_os.auth.dto.EnrollInitResponse;
import chemos.chem_os.auth.dto.LoginResponse;
import chemos.chem_os.auth.model.BackupCode;
import chemos.chem_os.auth.model.Role;
import chemos.chem_os.auth.model.TwoFactorCredential;
import chemos.chem_os.auth.model.User;
import chemos.chem_os.auth.repository.BackupCodeRepository;
import chemos.chem_os.auth.repository.TwoFactorCredentialRepository;
import chemos.chem_os.auth.repository.UserRepository;
import chemos.chem_os.auth.security.JwtService;
import chemos.chem_os.auth.security.TotpSecretEncryptor;
import chemos.chem_os.services.AuditLogService;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TwoFactorAuthServiceTest {

    private static final String PRE_AUTH_TOKEN = "Bearer pre-auth-token";
    private static final String VALID_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Mock
    private UserRepository userRepository;
    @Mock
    private TwoFactorCredentialRepository credentialRepository;
    @Mock
    private BackupCodeRepository backupCodeRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuditLogService auditLogService;

    // Real crypto and a real password encoder -- this flow's whole point is cryptographic
    // correctness, which a mock would just assume away.
    private final TotpSecretEncryptor secretEncryptor = new TotpSecretEncryptor();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private TwoFactorAuthService service;
    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(secretEncryptor, "base64Key", VALID_KEY);
        ReflectionTestUtils.invokeMethod(secretEncryptor, "init");

        service = new TwoFactorAuthService(
                userRepository,
                credentialRepository,
                backupCodeRepository,
                secretEncryptor,
                jwtService,
                passwordEncoder,
                auditLogService
        );
        ReflectionTestUtils.setField(service, "issuer", "ChemOS");
        ReflectionTestUtils.setField(service, "maxAttempts", 5);
        ReflectionTestUtils.setField(service, "lockoutDurationMinutes", 15L);

        Role role = new Role();
        role.setId("admin");
        role.setName("ADMIN");

        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("alice");
        user.setIsActive(true);
        user.setRole(role);

        when(jwtService.isValid("pre-auth-token")).thenReturn(true);
        when(jwtService.isPreAuthStage("pre-auth-token")).thenReturn(true);
        when(jwtService.extractUsername("pre-auth-token")).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
    }

    private String currentTotpCode(String secret) {
        try {
            long counter = Instant.now().getEpochSecond() / 30;
            return new DefaultCodeGenerator().generate(secret, counter);
        } catch (CodeGenerationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void rejectsMissingPreAuthToken() {
        assertThatThrownBy(() -> service.initEnrollment(null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Missing pre-auth token");
    }

    @Test
    void rejectsFullStageTokenOnPreAuthEndpoints() {
        when(jwtService.isValid("pre-auth-token")).thenReturn(true);
        when(jwtService.isPreAuthStage("pre-auth-token")).thenReturn(false);

        assertThatThrownBy(() -> service.initEnrollment(PRE_AUTH_TOKEN))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid or expired pre-auth token");
    }

    @Test
    void initEnrollmentGeneratesAndStoresAnEncryptedSecret() {
        when(credentialRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        EnrollInitResponse response = service.initEnrollment(PRE_AUTH_TOKEN);

        assertThat(response.secretBase32()).isNotBlank();
        assertThat(response.otpauthUri()).contains("ChemOS").contains("alice");

        ArgumentCaptor<TwoFactorCredential> captor = ArgumentCaptor.forClass(TwoFactorCredential.class);
        verify(credentialRepository).save(captor.capture());
        assertThat(captor.getValue().isEnabled()).isFalse();
        assertThat(captor.getValue().getEncryptedSecret()).isNotEqualTo(response.secretBase32());
        assertThat(secretEncryptor.decrypt(captor.getValue().getEncryptedSecret())).isEqualTo(response.secretBase32());
    }

    @Test
    void initEnrollmentRejectsUserAlreadyEnrolled() {
        TwoFactorCredential existing = new TwoFactorCredential();
        existing.setEnabled(true);
        when(credentialRepository.findByUserId(user.getId())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.initEnrollment(PRE_AUTH_TOKEN))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already enrolled");
    }

    @Test
    void confirmEnrollmentWithValidCodeActivatesTwoFactorAndIssuesBackupCodes() {
        String secret = "JBSWY3DPEHPK3PXP";
        TwoFactorCredential credential = new TwoFactorCredential();
        credential.setUser(user);
        credential.setEncryptedSecret(secretEncryptor.encrypt(secret));
        when(credentialRepository.findByUserId(user.getId())).thenReturn(Optional.of(credential));
        when(jwtService.generateToken("alice", "ADMIN")).thenReturn("full-access-token");

        EnrollConfirmResponse response = service.confirmEnrollment(PRE_AUTH_TOKEN, currentTotpCode(secret));

        assertThat(response.token()).isEqualTo("full-access-token");
        assertThat(response.backupCodes()).hasSize(10);
        assertThat(response.backupCodes()).doesNotHaveDuplicates();
        assertThat(credential.isEnabled()).isTrue();

        ArgumentCaptor<List<BackupCode>> captor = ArgumentCaptor.forClass(List.class);
        verify(backupCodeRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(10);
        // Stored rows hold only the bcrypt hash, never the plaintext code returned to the caller.
        assertThat(captor.getValue()).allSatisfy(row ->
                assertThat(response.backupCodes()).noneMatch(row.getCodeHash()::equals));
    }

    @Test
    void confirmEnrollmentWithInvalidCodeRecordsFailureAndRejects() {
        String secret = "JBSWY3DPEHPK3PXP";
        TwoFactorCredential credential = new TwoFactorCredential();
        credential.setUser(user);
        credential.setEncryptedSecret(secretEncryptor.encrypt(secret));
        when(credentialRepository.findByUserId(user.getId())).thenReturn(Optional.of(credential));

        assertThatThrownBy(() -> service.confirmEnrollment(PRE_AUTH_TOKEN, "000000"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid verification code");

        assertThat(credential.getFailedAttempts()).isEqualTo(1);
        assertThat(credential.isEnabled()).isFalse();
    }

    @Test
    void verifyLoginWithValidTotpCodeSucceeds() {
        String secret = "JBSWY3DPEHPK3PXP";
        TwoFactorCredential credential = new TwoFactorCredential();
        credential.setUser(user);
        credential.setEnabled(true);
        credential.setEncryptedSecret(secretEncryptor.encrypt(secret));
        when(credentialRepository.findByUserId(user.getId())).thenReturn(Optional.of(credential));
        when(jwtService.generateToken("alice", "ADMIN")).thenReturn("full-access-token");

        LoginResponse response = service.verifyLogin(PRE_AUTH_TOKEN, currentTotpCode(secret));

        assertThat(response.token()).isEqualTo("full-access-token");
        assertThat(response.username()).isEqualTo("alice");
    }

    @Test
    void verifyLoginRejectsTheSameTotpCodeUsedTwiceInARow() {
        String secret = "JBSWY3DPEHPK3PXP";
        String code = currentTotpCode(secret);
        TwoFactorCredential credential = new TwoFactorCredential();
        credential.setUser(user);
        credential.setEnabled(true);
        credential.setEncryptedSecret(secretEncryptor.encrypt(secret));
        credential.setLastUsedCode(code);
        credential.setLastCodeUsedAt(LocalDateTime.now());
        when(credentialRepository.findByUserId(user.getId())).thenReturn(Optional.of(credential));

        assertThatThrownBy(() -> service.verifyLogin(PRE_AUTH_TOKEN, code))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already been used");
    }

    @Test
    void verifyLoginAcceptsAndConsumesAValidBackupCode() {
        String plainBackupCode = "AB12-CD34";
        TwoFactorCredential credential = new TwoFactorCredential();
        credential.setUser(user);
        credential.setEnabled(true);
        credential.setEncryptedSecret(secretEncryptor.encrypt("JBSWY3DPEHPK3PXP"));
        when(credentialRepository.findByUserId(user.getId())).thenReturn(Optional.of(credential));

        BackupCode stored = new BackupCode();
        stored.setUser(user);
        stored.setCodeHash(passwordEncoder.encode(plainBackupCode));
        stored.setUsed(false);
        when(backupCodeRepository.findByUserIdAndUsedFalse(user.getId()))
                .thenReturn(new ArrayList<>(List.of(stored)));
        when(jwtService.generateToken("alice", "ADMIN")).thenReturn("full-access-token");

        LoginResponse response = service.verifyLogin(PRE_AUTH_TOKEN, plainBackupCode.toLowerCase());

        assertThat(response.token()).isEqualTo("full-access-token");
        assertThat(stored.isUsed()).isTrue();
        verify(backupCodeRepository).save(stored);
    }

    @Test
    void verifyLoginRejectsInvalidBackupCode() {
        TwoFactorCredential credential = new TwoFactorCredential();
        credential.setUser(user);
        credential.setEnabled(true);
        credential.setEncryptedSecret(secretEncryptor.encrypt("JBSWY3DPEHPK3PXP"));
        when(credentialRepository.findByUserId(user.getId())).thenReturn(Optional.of(credential));
        when(backupCodeRepository.findByUserIdAndUsedFalse(user.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> service.verifyLogin(PRE_AUTH_TOKEN, "ZZZZ-ZZZZ"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid verification code");
    }

    @Test
    void verifyLoginLocksOutAfterMaxFailedAttempts() {
        ReflectionTestUtils.setField(service, "maxAttempts", 3);
        TwoFactorCredential credential = new TwoFactorCredential();
        credential.setUser(user);
        credential.setEnabled(true);
        credential.setEncryptedSecret(secretEncryptor.encrypt("JBSWY3DPEHPK3PXP"));
        when(credentialRepository.findByUserId(user.getId())).thenReturn(Optional.of(credential));

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.verifyLogin(PRE_AUTH_TOKEN, "000000"))
                    .isInstanceOf(ResponseStatusException.class);
        }

        assertThatThrownBy(() -> service.verifyLogin(PRE_AUTH_TOKEN, "000000"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("locked");
    }

    @Test
    void verifyLoginRejectsWhenTwoFactorNotEnrolled() {
        when(credentialRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyLogin(PRE_AUTH_TOKEN, "123456"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not enrolled");
    }
}
