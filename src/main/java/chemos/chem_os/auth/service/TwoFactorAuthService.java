package chemos.chem_os.auth.service;

import chemos.chem_os.auth.dto.EnrollConfirmResponse;
import chemos.chem_os.auth.dto.EnrollInitResponse;
import chemos.chem_os.auth.dto.LoginResponse;
import chemos.chem_os.auth.dto.TwoFactorStatusResponse;
import chemos.chem_os.auth.model.BackupCode;
import chemos.chem_os.auth.model.TwoFactorCredential;
import chemos.chem_os.auth.model.User;
import chemos.chem_os.auth.repository.BackupCodeRepository;
import chemos.chem_os.auth.repository.TwoFactorCredentialRepository;
import chemos.chem_os.auth.repository.UserRepository;
import chemos.chem_os.auth.security.JwtService;
import chemos.chem_os.auth.security.TotpSecretEncryptor;
import chemos.chem_os.services.AuditLogService;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TwoFactorAuthService {

    private static final String BACKUP_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int BACKUP_CODE_COUNT = 10;

    private final UserRepository userRepository;
    private final TwoFactorCredentialRepository credentialRepository;
    private final BackupCodeRepository backupCodeRepository;
    private final TotpSecretEncryptor secretEncryptor;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final CodeVerifier codeVerifier = buildCodeVerifier();

    @Value("${totp.issuer}")
    private String issuer;

    @Value("${totp.lockout.max-attempts}")
    private int maxAttempts;

    @Value("${totp.lockout.duration-minutes}")
    private long lockoutDurationMinutes;

    private static CodeVerifier buildCodeVerifier() {
        DefaultCodeVerifier verifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
        verifier.setTimePeriod(30);
        verifier.setAllowedTimePeriodDiscrepancy(2); // Allow ±60 seconds for clock drift
        return verifier;
    }

    // Resolves the username carried by a pre-auth token, rejecting anything else
    // (missing header, malformed/expired token, or a FULL-stage token used here by mistake).
    public String resolvePreAuthUsername(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing pre-auth token");
        }
        String token = authHeader.substring(7);
        if (!jwtService.isValid(token) || !jwtService.isPreAuthStage(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired pre-auth token");
        }
        return jwtService.extractUsername(token);
    }

    @Transactional
    public EnrollInitResponse initEnrollment(String authHeader) {
        String username = resolvePreAuthUsername(authHeader);
        User user = loadUser(username);

        TwoFactorCredential credential = credentialRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    TwoFactorCredential c = new TwoFactorCredential();
                    c.setUser(user);
                    return c;
                });

        if (credential.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "2FA is already enrolled for this user");
        }

        String secret = secretGenerator.generate();
        credential.setEncryptedSecret(secretEncryptor.encrypt(secret));
        credentialRepository.save(credential);

        auditLogService.log("2FA_ENROLL_INIT", "USER_2FA", user.getId().toString(), null, null);

        String otpauthUri = new QrData.Builder()
                .label(user.getUsername())
                .secret(secret)
                .issuer(issuer)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build()
                .getUri();

        return new EnrollInitResponse(secret, otpauthUri);
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public EnrollConfirmResponse confirmEnrollment(String authHeader, String code) {
        String username = resolvePreAuthUsername(authHeader);
        User user = loadUser(username);

        TwoFactorCredential credential = credentialRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enrollment was not initiated"));

        if (credential.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "2FA is already enrolled for this user");
        }

        assertNotLocked(credential);

        String secret = secretEncryptor.decrypt(credential.getEncryptedSecret());
        if (!codeVerifier.isValidCode(secret, code)) {
            recordFailure(credential);
            auditLogService.log("2FA_ENROLL_FAILED", "USER_2FA", user.getId().toString(), null, null);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid verification code");
        }

        credential.setEnabled(true);
        credential.setEnrolledAt(LocalDateTime.now());
        recordSuccess(credential, code);

        List<String> backupCodes = issueBackupCodes(user);

        auditLogService.log("2FA_ENROLLED", "USER_2FA", user.getId().toString(), null,
                Map.of("enabled", true, "backupCodesIssued", backupCodes.size()));

        String token = jwtService.generateToken(user.getUsername(), user.getRole().getName());
        return new EnrollConfirmResponse(token, user.getUsername(), user.getRole().getName(), backupCodes);
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public LoginResponse verifyLogin(String authHeader, String code) {
        String username = resolvePreAuthUsername(authHeader);
        User user = loadUser(username);

        TwoFactorCredential credential = credentialRepository.findByUserId(user.getId())
                .filter(TwoFactorCredential::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "2FA is not enrolled for this user"));

        assertNotLocked(credential);

        boolean verified;
        boolean viaBackupCode = false;
        String usedCode = code; // Track the code being used

        if (code != null && code.matches("\\d{6}")) {
            // Check if this code was already used recently (within 90 seconds to cover time window)
            if (credential.getLastUsedCode() != null && 
                credential.getLastUsedCode().equals(code) &&
                credential.getLastCodeUsedAt() != null &&
                credential.getLastCodeUsedAt().isAfter(LocalDateTime.now().minusSeconds(90))) {
                recordFailure(credential);
                auditLogService.log("2FA_LOGIN_FAILED", "USER_2FA", user.getId().toString(), null, 
                        Map.of("reason", "CODE_REUSE", "code", code));
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This code has already been used. Please wait for a new code.");
            }
            
            String secret = secretEncryptor.decrypt(credential.getEncryptedSecret());
            verified = codeVerifier.isValidCode(secret, code);
        } else {
            BackupCode matched = matchBackupCode(user, code);
            verified = matched != null;
            if (matched != null) {
                viaBackupCode = true;
                matched.setUsed(true);
                matched.setUsedAt(LocalDateTime.now());
                backupCodeRepository.save(matched);
            }
        }

        if (!verified) {
            recordFailure(credential);
            auditLogService.log("2FA_LOGIN_FAILED", "USER_2FA", user.getId().toString(), null, null);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid verification code");
        }

        recordSuccess(credential, viaBackupCode ? null : usedCode);

        if (viaBackupCode) {
            auditLogService.log("2FA_BACKUP_CODE_USED", "USER_2FA", user.getId().toString(), null, null);
            long remaining = backupCodeRepository.countByUserIdAndUsedFalse(user.getId());
            if (remaining == 0) {
                auditLogService.log("2FA_BACKUP_CODES_EXHAUSTED", "USER_2FA", user.getId().toString(), null, null);
            }
        }

        auditLogService.log("2FA_LOGIN_VERIFIED", "USER_2FA", user.getId().toString(), null, null);

        String token = jwtService.generateToken(user.getUsername(), user.getRole().getName());
        return new LoginResponse(token, user.getUsername(), user.getRole().getName());
    }

    @Transactional
    public TwoFactorStatusResponse resetTwoFactor(String username) {
        User user = loadUser(username);

        credentialRepository.deleteByUserId(user.getId());
        backupCodeRepository.deleteByUserId(user.getId());

        auditLogService.log("2FA_RESET", "USER_2FA", user.getId().toString(), null, null);

        return new TwoFactorStatusResponse(user.getUsername(), false, 0);
    }

    @Transactional
    public TwoFactorStatusResponse unlockTwoFactor(String username) {
        User user = loadUser(username);
        
        TwoFactorCredential credential = credentialRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User has no 2FA credential"));
        
        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);
        credentialRepository.save(credential);
        
        auditLogService.log("2FA_UNLOCKED", "USER_2FA", user.getId().toString(), null, 
                Map.of("wasLockedUntil", credential.getLockedUntil() != null ? credential.getLockedUntil().toString() : "null"));
        
        long backupCodesRemaining = backupCodeRepository.countByUserIdAndUsedFalse(user.getId());
        return new TwoFactorStatusResponse(user.getUsername(), credential.isEnabled(), (int) backupCodesRemaining);
    }

    public TwoFactorStatusResponse getTwoFactorStatus(String username) {
        User user = loadUser(username);
        
        TwoFactorCredential credential = credentialRepository.findByUserId(user.getId())
                .orElse(null);
        
        if (credential == null) {
            return new TwoFactorStatusResponse(user.getUsername(), false, 0);
        }
        
        long backupCodesRemaining = backupCodeRepository.countByUserIdAndUsedFalse(user.getId());
        return new TwoFactorStatusResponse(user.getUsername(), credential.isEnabled(), (int) backupCodesRemaining);
    }

    private BackupCode matchBackupCode(User user, String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = code.trim().toUpperCase();
        return backupCodeRepository.findByUserIdAndUsedFalse(user.getId()).stream()
                .filter(bc -> passwordEncoder.matches(normalized, bc.getCodeHash()))
                .findFirst()
                .orElse(null);
    }

    private List<String> issueBackupCodes(User user) {
        backupCodeRepository.deleteByUserId(user.getId());

        SecureRandom random = new SecureRandom();
        List<String> plaintextCodes = new ArrayList<>(BACKUP_CODE_COUNT);
        List<BackupCode> rows = new ArrayList<>(BACKUP_CODE_COUNT);

        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            String plain = generateBackupCode(random);
            plaintextCodes.add(plain);

            BackupCode row = new BackupCode();
            row.setUser(user);
            row.setCodeHash(passwordEncoder.encode(plain));
            row.setUsed(false);
            row.setCreatedAt(LocalDateTime.now());
            rows.add(row);
        }

        backupCodeRepository.saveAll(rows);
        return plaintextCodes;
    }

    private String generateBackupCode(SecureRandom random) {
        StringBuilder sb = new StringBuilder(9);
        for (int i = 0; i < 8; i++) {
            if (i == 4) {
                sb.append('-');
            }
            sb.append(BACKUP_CODE_ALPHABET.charAt(random.nextInt(BACKUP_CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    private void assertNotLocked(TwoFactorCredential credential) {
        if (credential.getLockedUntil() != null && credential.getLockedUntil().isAfter(LocalDateTime.now())) {
            long minutesRemaining = java.time.Duration.between(LocalDateTime.now(), credential.getLockedUntil()).toMinutes();
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    String.format("Account locked due to too many failed 2FA attempts. Please try again in %d minutes. Failed attempts: %d/%d",
                            minutesRemaining, credential.getFailedAttempts(), maxAttempts));
        }
    }

    private void recordFailure(TwoFactorCredential credential) {
        credential.setFailedAttempts(credential.getFailedAttempts() + 1);
        if (credential.getFailedAttempts() >= maxAttempts) {
            credential.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutDurationMinutes));
        }
        credentialRepository.save(credential);
    }

    private void recordSuccess(TwoFactorCredential credential, String usedCode) {
        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);
        credential.setLastVerifiedAt(LocalDateTime.now());
        // Save the last used TOTP code to prevent reuse
        if (usedCode != null && usedCode.matches("\\d{6}")) {
            credential.setLastUsedCode(usedCode);
            credential.setLastCodeUsedAt(LocalDateTime.now());
        }
        credentialRepository.save(credential);
    }

    private User loadUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
