package chemos.chem_os.auth.controller;

import chemos.chem_os.auth.dto.PermissionOverrideRequest;
import chemos.chem_os.auth.dto.PermissionOverrideResponse;
import chemos.chem_os.auth.service.PermissionOverrideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Lets a user delegate ONE specific permission they already hold to ONE specific other user,
// beyond whatever that user's role grants by default (e.g. a Sales Manager handing SALE_EDIT
// to a particular Sales Executive). Kept separate from AuthController — user CRUD/2FA is a
// different concern from per-user permission overrides.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/users/{username}/permission-overrides")
public class PermissionOverrideController {

    private final PermissionOverrideService permissionOverrideService;

    // Guard: caller must already hold the exact permission they're trying to grant/deny —
    // e.g. a Sales Manager can delegate SALE_EDIT but never PURCHASE_EDIT. Known v1 limitation:
    // there's no manager->report relationship in the User model, so any two users who both hold
    // a permission can override it on each other, not just "a manager acting on their reports."
    @PreAuthorize("hasAuthority(#permissionCode)")
    @PutMapping("/{permissionCode}")
    public ResponseEntity<PermissionOverrideResponse> setOverride(
            @PathVariable String username,
            @P("permissionCode") @PathVariable("permissionCode") String permissionCode,
            @Valid @RequestBody PermissionOverrideRequest request) {
        return ResponseEntity.ok(permissionOverrideService.setOverride(username, permissionCode, request));
    }

    @PreAuthorize("hasAuthority(#permissionCode)")
    @DeleteMapping("/{permissionCode}")
    public ResponseEntity<Void> removeOverride(
            @PathVariable String username,
            @P("permissionCode") @PathVariable("permissionCode") String permissionCode) {
        permissionOverrideService.removeOverride(username, permissionCode);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<PermissionOverrideResponse>> listOverrides(@PathVariable String username) {
        return ResponseEntity.ok(permissionOverrideService.listOverrides(username));
    }
}
