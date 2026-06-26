package chemos.chem_os.controller;

import chemos.chem_os.model.AuditLog;
import chemos.chem_os.services.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditLogService auditLogService;

    @PreAuthorize("hasAuthority('USER_MANAGEMENT')")
    @GetMapping("/logs")
    public ResponseEntity<Page<AuditLog>> getLogs(
            @RequestParam(required = false) String entityType,
            @PageableDefault(size = 20, sort = "performedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getLogs(entityType, pageable));
    }
}
