package chemos.chem_os.services;

import chemos.chem_os.auth.model.User;
import chemos.chem_os.auth.repository.UserRepository;
import chemos.chem_os.model.AuditLog;
import chemos.chem_os.repository.AuditLogRepository;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void log(String action, String entityType, String entityId, Object dataBefore, Object dataAfter) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";

            String performedByName = username;
            String performedByRole = "UNKNOWN";

            try {
                User user = userRepository.findByUsername(username).orElse(null);
                if (user != null) {
                    performedByName = user.getName();
                    performedByRole = user.getRole().getDisplayName();
                }
            } catch (Exception e) {
                log.warn("Audit: could not resolve user details for '{}': {}", username, e.getMessage());
            }

            AuditLog entry = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .performedBy(username)
                    .performedByName(performedByName)
                    .performedByRole(performedByRole)
                    .dataBefore(dataBefore != null ? objectMapper.writeValueAsString(dataBefore) : null)
                    .dataAfter(dataAfter != null ? objectMapper.writeValueAsString(dataAfter) : null)
                    .performedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")))
                    .build();

            auditLogRepository.save(entry);

        } catch (Exception e) {
            // Never let audit failure break the real operation
            log.error("Audit log write failed [action={} entity={} id={}]: {}", action, entityType, entityId, e.getMessage());
        }
    }

    public Page<AuditLog> getLogs(String entityType, Pageable pageable) {
        if (entityType != null && !entityType.isBlank()) {
            return auditLogRepository.findByEntityType(entityType.toUpperCase(), pageable);
        }
        return auditLogRepository.findAll(pageable);
    }
}
