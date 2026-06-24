package chemos.chem_os.controller;

import chemos.chem_os.dto.CreateSalespersonRequest;
import chemos.chem_os.dto.SalespersonSuggestionResponse;
import chemos.chem_os.services.SalespersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/salespersons")
public class SalespersonController {

    private final SalespersonService salespersonService;

    @GetMapping("/suggestions")
    public ResponseEntity<List<SalespersonSuggestionResponse>> getSuggestionSalespersons(
            @RequestParam(value = "query", required = false, defaultValue = "") String query) {

        return ResponseEntity.ok(salespersonService.searchSalespersons(query));
    }

    @PostMapping("/createSalesperson")
    public ResponseEntity<SalespersonSuggestionResponse> createSalesperson(
            @Valid @RequestBody CreateSalespersonRequest request) {

        SalespersonSuggestionResponse response =
                salespersonService.createSalesperson(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}