package chemos.chem_os.controller;

import chemos.chem_os.dto.MarketStatusResponse;
import chemos.chem_os.services.MarketStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/market-status")
public class MarketStatusController {

    private final MarketStatusService marketStatusService;

    @GetMapping("/all")
    public ResponseEntity<List<MarketStatusResponse>> getAllMarketStatuses() {
        return ResponseEntity.ok(marketStatusService.getAllMarketStatuses());
    }
}
