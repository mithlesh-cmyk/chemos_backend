package chemos.chem_os.auth.dto;

public record LoginResponse(
        String token,
        String username,
        String role        // e.g. "ADMIN", "PURCHASE_MANAGER" — frontend uses this to show/hide UI
) {
}
