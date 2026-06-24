package chemos.chem_os.auth.dto;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        Boolean isActive,
        String role,         // role name e.g. "PURCHASE_MANAGER"
        String roleDisplay   // display name e.g. "Purchase Manager"
) {
}
