package chemos.chem_os.controller;

import chemos.chem_os.dto.CompanyCreationResponse;
import chemos.chem_os.dto.CompanySuggestionResposne;
import chemos.chem_os.dto.CreateCompanyRequest;
import chemos.chem_os.services.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping("/create-company")
    public ResponseEntity<CompanyCreationResponse> createCompany(
            @RequestBody CreateCompanyRequest companyRequest) {

        CompanyCreationResponse response =
                companyService.createCompany(companyRequest);

        if ("INACTIVE_FOUND".equals(response.status())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/search")
    public List<CompanySuggestionResposne> searchCompanies(
            @RequestParam(value = "query", required = false, defaultValue = "") String query) {
        return companyService.searchCompanies(query);
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateCompany(@PathVariable String id) {
        companyService.deactivateCompany(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivateCompany(
            @PathVariable String id) {

        companyService.reactivateCompany(id);

        return ResponseEntity.noContent().build();
    }
}
