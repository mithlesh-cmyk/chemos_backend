package chemos.chem_os.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:true}")
    private boolean enabled;

    @Value("${app.mail.from:}")
    private String from;

    @Value("${app.login-url:}")
    private String loginUrl;

    // Async + failure-tolerant: a mail outage must never break user creation.
    @Async
    public void sendWelcomeEmail(String to, String name, String username, String password) {
        if (!enabled) {
            log.info("Mail disabled; skipping welcome email for user {}", username);
            return;
        }
        try {
            StringBuilder body = new StringBuilder()
                    .append("Hello ").append(name).append(",\n\n")
                    .append("An account has been created for you on ChemOS.\n\n")
                    .append("Email:    ").append(to).append('\n')
                    .append("Username: ").append(username).append('\n')
                    .append("Password: ").append(password).append("\n\n");
            if (!loginUrl.isBlank()) {
                body.append("Login: ").append(loginUrl).append("\n\n");
            }

            SimpleMailMessage message = new SimpleMailMessage();
            if (!from.isBlank()) {
                message.setFrom(from);
            }
            message.setTo(to);
            message.setSubject("Your ChemOS account details");
            message.setText(body.toString());
            mailSender.send(message);
            log.info("Welcome email sent for user {}", username);
        } catch (Exception e) {
            log.error("Failed to send welcome email for user {}: {}", username, e.getMessage());
        }
    }
}
