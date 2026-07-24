package chemos.chem_os.controller;

import chemos.chem_os.dto.ProductStockBreakdownResponse;
import chemos.chem_os.dto.VesselStockStatsResponse;
import chemos.chem_os.dto.VesselStockStatsSummaryResponse;
import chemos.chem_os.services.VesselStockStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/stock-stats")
public class VesselStockStatsController {

    private final VesselStockStatsService vesselStockStatsService;

    @PreAuthorize("hasAuthority('STOCK_STATS_VIEW')")
    @GetMapping
    public ResponseEntity<List<VesselStockStatsResponse>> getStockStats() {
        return ResponseEntity.ok(vesselStockStatsService.getStats());
    }

    @PreAuthorize("hasAuthority('STOCK_STATS_VIEW')")
    @GetMapping("/summary")
    public ResponseEntity<VesselStockStatsSummaryResponse> getStockStatsSummary(
            @RequestParam(required = false) String vesselName,
            @RequestParam(required = false) String product) {
        return ResponseEntity.ok(vesselStockStatsService.getSummary(vesselName, product));
    }

    @PreAuthorize("hasAuthority('STOCK_STATS_VIEW')")
    @GetMapping("/by-product")
    public ResponseEntity<List<ProductStockBreakdownResponse>> getStockStatsByProduct() {
        return ResponseEntity.ok(vesselStockStatsService.getProductBreakdown());
    }

    @PreAuthorize("hasAuthority('STOCK_STATS_VIEW')")
    @GetMapping("/by-product/history")
    public ResponseEntity<List<ProductStockBreakdownResponse>> getStockStatsByProductHistory() {
        return ResponseEntity.ok(vesselStockStatsService.getProductBreakdownHistorical());
    }

    @PreAuthorize("hasAuthority('STOCK_STATS_VIEW')")
    @GetMapping("/last-upload")
    public ResponseEntity<Map<String, LocalDateTime>> getLastUpload() {

        return ResponseEntity.ok(
                Map.of(
                        "lastCsvUploadedAt",
                        vesselStockStatsService.getLastCsvUploadedAt()
                )
        );
    }
}
