package chemos.chem_os.controller;

import chemos.chem_os.dto.CountrySuggestionResponse;
import chemos.chem_os.services.CountryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/countries")
public class CountryController {

    private final CountryService countryService;

    @GetMapping("/search")
    public ResponseEntity<Page<CountrySuggestionResponse>> getSuggestionCountries(
            @RequestParam(value = "query", required = false, defaultValue = "") String query,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<CountrySuggestionResponse> responsePage = countryService.searchCountries(query, pageable);
        return ResponseEntity.ok(responsePage);
    }
}
