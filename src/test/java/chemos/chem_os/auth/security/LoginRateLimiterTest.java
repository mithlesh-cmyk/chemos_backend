package chemos.chem_os.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRateLimiterTest {

    private LoginRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new LoginRateLimiter();
        ReflectionTestUtils.setField(limiter, "maxAttempts", 3);
        ReflectionTestUtils.setField(limiter, "lockoutDurationMinutes", 15L);
    }

    @Test
    void allowsLoginWhenNoPriorFailures() {
        assertThatCode(() -> limiter.assertNotLocked("alice")).doesNotThrowAnyException();
    }

    @Test
    void locksOutAfterReachingMaxAttempts() {
        limiter.recordFailure("alice");
        limiter.recordFailure("alice");
        limiter.recordFailure("alice"); // 3rd failure hits maxAttempts=3

        assertThatThrownBy(() -> limiter.assertNotLocked("alice"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Too many failed login attempts");
    }

    @Test
    void doesNotLockOutBeforeReachingMaxAttempts() {
        limiter.recordFailure("alice");
        limiter.recordFailure("alice"); // only 2 of 3

        assertThatCode(() -> limiter.assertNotLocked("alice")).doesNotThrowAnyException();
    }

    @Test
    void successResetsFailureCount() {
        limiter.recordFailure("alice");
        limiter.recordFailure("alice");
        limiter.recordSuccess("alice");
        limiter.recordFailure("alice");
        limiter.recordFailure("alice");

        // Only 2 failures since the reset -- still below maxAttempts=3
        assertThatCode(() -> limiter.assertNotLocked("alice")).doesNotThrowAnyException();
    }

    @Test
    void lockoutIsIsolatedPerUsername() {
        limiter.recordFailure("alice");
        limiter.recordFailure("alice");
        limiter.recordFailure("alice");

        assertThat(limiter).isNotNull();
        assertThatCode(() -> limiter.assertNotLocked("bob")).doesNotThrowAnyException();
    }

    @Test
    void usernameMatchingIsCaseAndWhitespaceInsensitive() {
        limiter.recordFailure(" Alice ");
        limiter.recordFailure("ALICE");
        limiter.recordFailure("alice");

        assertThatThrownBy(() -> limiter.assertNotLocked("alice"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
