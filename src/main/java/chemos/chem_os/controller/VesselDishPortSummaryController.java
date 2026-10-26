package chemos.chem_os.controller;

import chemos.chem_os.dto.VesselDishPortSummaryResponse;
import chemos.chem_os.services.VesselDishPortSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/vessel-dish-port-summary")
public class VesselDishPortSummaryController {

    private final VesselDishPortSummaryService service;

    @GetMapping
    public ResponseEntity<List<VesselDishPortSummaryResponse>> getAll(
            @RequestParam(name = "product_id", required = false) String productId) {
        return ResponseEntity.ok(service.getAll(productId));
    }
}
