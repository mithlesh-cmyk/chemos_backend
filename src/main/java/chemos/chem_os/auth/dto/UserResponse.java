package chemos.chem_os.auth.dto;

import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        Boolean isActive,
        String name,
        String email,
        String role,              // role name e.g. "PURCHASE_MANAGER"
        String roleDisplay,       // display name e.g. "Purchase Manager"
        List<String> permissions  // effective permission codes: role ∪ parent ∪ user overrides
) {
}
