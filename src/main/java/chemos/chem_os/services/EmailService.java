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
        StringBuilder body = new StringBuilder()
                .append("Hello ").append(name).append(",\n\n")
                .append("An account has been created for you on Sumairo OS.\n\n")
                .append("Email:    ").append(to).append('\n')
                .append("Username: ").append(username).append('\n')
                .append("Password: ").append(password).append("\n\n");
        appendLoginUrl(body);
        send(to, username, "Your Sumairo OS account details", body.toString());
    }

    @Async
    public void sendPasswordChangedEmail(String to, String name, String username, String password) {
        StringBuilder body = new StringBuilder()
                .append("Hello ").append(name).append(",\n\n")
                .append("Your Sumairo OS password has been changed by an administrator.\n\n")
                .append("Username:     ").append(username).append('\n')
                .append("New password: ").append(password).append("\n\n");
        appendLoginUrl(body);
        send(to, username, "Your Sumairo OS password was changed", body.toString());
    }

    private void appendLoginUrl(StringBuilder body) {
        if (!loginUrl.isBlank()) {
            body.append("Login: ").append(loginUrl).append("\n\n");
        }
    }

    private void send(String to, String username, String subject, String text) {
        if (!enabled) {
            log.info("Mail disabled; skipping '{}' email for user {}", subject, username);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (!from.isBlank()) {
                message.setFrom(from);
            }
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("'{}' email sent for user {}", subject, username);
        } catch (Exception e) {
            log.error("Failed to send '{}' email for user {}: {}", subject, username, e.getMessage());
        }
    }
}
