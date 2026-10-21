package chemos.chem_os.auth.dto;

import chemos.chem_os.auth.model.UserPermissionRestriction.OverrideEffect;
import jakarta.validation.constraints.NotNull;

public record PermissionOverrideRequest(
        @NotNull OverrideEffect effect,
        String reason
) {
}
