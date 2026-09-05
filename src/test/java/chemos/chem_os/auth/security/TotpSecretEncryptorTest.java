package chemos.chem_os.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TotpSecretEncryptorTest {

    private static final String VALID_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    private TotpSecretEncryptor encryptor;

    @BeforeEach
    void setUp() {
        encryptor = new TotpSecretEncryptor();
        ReflectionTestUtils.setField(encryptor, "base64Key", VALID_KEY);
        ReflectionTestUtils.invokeMethod(encryptor, "init");
    }

    @Test
    void encryptThenDecryptReturnsOriginalSecret() {
        String secret = "JBSWY3DPEHPK3PXP";

        String encrypted = encryptor.encrypt(secret);
        String decrypted = encryptor.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(secret);
    }

    @Test
    void encryptingTheSameSecretTwiceProducesDifferentCiphertext() {
        String secret = "JBSWY3DPEHPK3PXP";

        String first = encryptor.encrypt(secret);
        String second = encryptor.encrypt(secret);

        // Random IV per call -- ciphertext must differ even for identical plaintext.
        assertThat(first).isNotEqualTo(second);
        assertThat(encryptor.decrypt(first)).isEqualTo(secret);
        assertThat(encryptor.decrypt(second)).isEqualTo(secret);
    }

    @Test
    void rejectsKeyThatDoesNotDecodeToThirtyTwoBytes() {
        TotpSecretEncryptor badEncryptor = new TotpSecretEncryptor();
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        ReflectionTestUtils.setField(badEncryptor, "base64Key", shortKey);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(badEncryptor, "init"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void decryptFailsOnTamperedCiphertext() {
        String encrypted = encryptor.encrypt("JBSWY3DPEHPK3PXP");
        byte[] tampered = Base64.getDecoder().decode(encrypted);
        tampered[tampered.length - 1] ^= 0x01; // flip a bit inside the GCM auth tag
        String tamperedEncoded = Base64.getEncoder().encodeToString(tampered);

        assertThatThrownBy(() -> encryptor.decrypt(tamperedEncoded))
                .isInstanceOf(TotpSecretEncryptor.TotpDecryptionException.class);
    }
}
