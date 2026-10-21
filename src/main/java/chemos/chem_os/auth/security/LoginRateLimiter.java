package chemos.chem_os.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// In-memory brute-force guard for the password step of /auth/login, mirroring the
// lockout behavior TwoFactorAuthService already applies to the 2FA-code step.
// Per-instance only: state is not shared across app instances behind a load balancer.
@Component
public class LoginRateLimiter {

    private static class Attempt {
        int failedAttempts;
        LocalDateTime lockedUntil;
    }

    private final Map<String, Attempt> attemptsByUsername = new ConcurrentHashMap<>();

    @Value("${login.lockout.max-attempts}")
    private int maxAttempts;

    @Value("${login.lockout.duration-minutes}")
    private long lockoutDurationMinutes;

    public void assertNotLocked(String username) {
        Attempt attempt = attemptsByUsername.get(normalize(username));
        if (attempt == null) {
            return;
        }
        synchronized (attempt) {
            if (attempt.lockedUntil != null && attempt.lockedUntil.isAfter(LocalDateTime.now())) {
                long minutesRemaining = Duration.between(LocalDateTime.now(), attempt.lockedUntil).toMinutes() + 1;
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        String.format("Too many failed login attempts. Please try again in %d minute(s).", minutesRemaining));
            }
        }
    }

    public void recordFailure(String username) {
        Attempt attempt = attemptsByUsername.computeIfAbsent(normalize(username), k -> new Attempt());
        synchronized (attempt) {
            attempt.failedAttempts++;
            if (attempt.failedAttempts >= maxAttempts) {
                attempt.lockedUntil = LocalDateTime.now().plusMinutes(lockoutDurationMinutes);
            }
        }
    }

    public void recordSuccess(String username) {
        attemptsByUsername.remove(normalize(username));
    }

    private String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
