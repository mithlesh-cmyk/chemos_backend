package chemos.chem_os.dto;

import java.time.LocalDateTime;

public record VesselInventoryRow(
        String vesselName,
        String product,
        String dischargePort,
        LocalDateTime date,
        String companyFrom
) {
}
