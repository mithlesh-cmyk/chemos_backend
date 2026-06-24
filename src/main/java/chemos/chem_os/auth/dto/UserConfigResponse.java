package chemos.chem_os.auth.dto;

import java.util.List;

public record UserConfigResponse(
        UserInfo user,
        List<String> permissions,
        ModulesConfig modules
) {
    public record UserInfo(
            String username,
            String name,
            String role,
            String roleDisplayName
    ) {}

    public record ModuleAccess(
            boolean canView,
            boolean canCreate,
            boolean canEdit,
            boolean canApprove
    ) {}

    public record ModulesConfig(
            ModuleAccess sales,
            ModuleAccess purchases,
            ModuleAccess company,
            ModuleAccess products
    ) {}
}
