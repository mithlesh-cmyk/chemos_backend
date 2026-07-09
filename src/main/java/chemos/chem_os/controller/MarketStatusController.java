package chemos.chem_os.controller;

import chemos.chem_os.dto.MarketStatusResponse;
import chemos.chem_os.services.MarketStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import chemos.chem_os.dto.ApiSuccessResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/market-status")
public class MarketStatusController {

    private final MarketStatusService marketStatusService;

    @GetMapping("/all")
    public ResponseEntity<ApiSuccessResponse<List<MarketStatusResponse>>> getAllMarketStatuses() {

        List<MarketStatusResponse> marketStatuses =
                marketStatusService.getAllMarketStatuses();

        String message = marketStatuses.isEmpty()
                ? "No market statuses found."
                : "Market statuses fetched successfully.";

        return ResponseEntity.ok(
                ApiSuccessResponse.<List<MarketStatusResponse>>builder()
                        .message(message)
                        .data(marketStatuses)
                        .build()
        );
    }
}