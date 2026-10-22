package chemos.chem_os.controller;

import chemos.chem_os.dto.ShipPagedResponse;
import chemos.chem_os.services.ShipDataService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1")
public class ShipDataController {

    private final ShipDataService shipDataService;

    @GetMapping("/mainshipdata")
    public ShipPagedResponse getMainShipData(
            @RequestParam(defaultValue = "10") @Min(1) @Max(10) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset) {
        return shipDataService.getMainShipData(limit, offset);
    }

    @GetMapping("/ship-inventory")
    public ShipPagedResponse getShipInventory(
            @RequestParam(defaultValue = "10") @Min(1) @Max(10) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset) {
        return shipDataService.getShipInventory(limit, offset);
    }

    @GetMapping("/ship-inventory/search")
    public ShipPagedResponse searchShipInventory(
            @RequestParam @Size(min = 1) String q,
            @RequestParam(defaultValue = "10") @Min(1) @Max(10) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset) {
        return shipDataService.searchShipInventory(q, limit, offset);
    }
}
