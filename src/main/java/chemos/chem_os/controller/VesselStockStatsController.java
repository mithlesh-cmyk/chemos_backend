package chemos.chem_os.controller;

import chemos.chem_os.dto.ApiSuccessResponse;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/stock-stats")
public class VesselStockStatsController {

    private final VesselStockStatsService vesselStockStatsService;

    @PreAuthorize("hasAuthority('STOCK_STATS_VIEW')")
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<List<VesselStockStatsResponse>>> getStockStats() {

        List<VesselStockStatsResponse> response =
                vesselStockStatsService.getStats();

        String message = response.isEmpty()
                ? "No stock statistics found."
                : "Stock statistics fetched successfully.";

        return ResponseEntity.ok(
                ApiSuccessResponse.<List<VesselStockStatsResponse>>builder()
                        .message(message)
                        .data(response)
                        .build()
        );
    }

    @PreAuthorize("hasAuthority('STOCK_STATS_VIEW')")
    @GetMapping("/summary")
    public ResponseEntity<ApiSuccessResponse<VesselStockStatsSummaryResponse>> getStockStatsSummary(
            @RequestParam(required = false) String vesselName,
            @RequestParam(required = false) String product) {

        VesselStockStatsSummaryResponse response =
                vesselStockStatsService.getSummary(vesselName, product);

        return ResponseEntity.ok(
                ApiSuccessResponse.<VesselStockStatsSummaryResponse>builder()
                        .message("Stock statistics summary fetched successfully.")
                        .data(response)
                        .build()
        );
    }

    @PreAuthorize("hasAuthority('STOCK_STATS_VIEW')")
    @GetMapping("/by-product")
    public ResponseEntity<ApiSuccessResponse<List<ProductStockBreakdownResponse>>> getStockStatsByProduct() {

        List<ProductStockBreakdownResponse> response =
                vesselStockStatsService.getProductBreakdown();

        String message = response.isEmpty()
                ? "No product stock breakdown found."
                : "Product stock breakdown fetched successfully.";

        return ResponseEntity.ok(
                ApiSuccessResponse.<List<ProductStockBreakdownResponse>>builder()
                        .message(message)
                        .data(response)
                        .build()
        );
    }
}