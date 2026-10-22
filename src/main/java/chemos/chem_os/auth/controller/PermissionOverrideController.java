package chemos.chem_os.auth.controller;

import chemos.chem_os.auth.dto.PermissionOverrideRequest;
import chemos.chem_os.auth.dto.PermissionOverrideResponse;
import chemos.chem_os.auth.service.PermissionOverrideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/users/{username}/permission-overrides")
@PreAuthorize("hasAuthority('ROLE_MANAGEMENT')")
public class PermissionOverrideController {

    private final PermissionOverrideService permissionOverrideService;

    @PutMapping("/{permissionCode}")
    public ResponseEntity<PermissionOverrideResponse> setOverride(
            @PathVariable String username,
            @PathVariable("permissionCode") String permissionCode,
            @Valid @RequestBody PermissionOverrideRequest request) {
        return ResponseEntity.ok(permissionOverrideService.setOverride(username, permissionCode, request));
    }

    @DeleteMapping("/{permissionCode}")
    public ResponseEntity<Void> removeOverride(
            @PathVariable String username,
            @PathVariable("permissionCode") String permissionCode) {
        permissionOverrideService.removeOverride(username, permissionCode);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<PermissionOverrideResponse>> listOverrides(@PathVariable String username) {
        return ResponseEntity.ok(permissionOverrideService.listOverrides(username));
    }
}
