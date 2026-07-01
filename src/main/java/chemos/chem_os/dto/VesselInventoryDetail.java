package chemos.chem_os.dto;

import java.time.LocalDate;

public record VesselInventoryDetail(
        String vesselName,
        LocalDate eta,
        Long inventoryDays
) {
}
