package chemos.chem_os.auth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "two_factor_credentials")
public class TwoFactorCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // AES-256-GCM encrypted TOTP secret, base64(iv || ciphertext+tag). Null until enrollment is initiated.
    @Column(name = "encrypted_secret", columnDefinition = "TEXT")
    private String encryptedSecret;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = false;

    @Column(name = "enrolled_at")
    private LocalDateTime enrolledAt;

    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    // Track last used TOTP code to prevent reuse within the same time window
    @Column(name = "last_used_code")
    private String lastUsedCode;

    @Column(name = "last_code_used_at")
    private LocalDateTime lastCodeUsedAt;
}
