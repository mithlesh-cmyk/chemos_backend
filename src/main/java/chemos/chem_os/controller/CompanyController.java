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
    public ResponseEntity<CompanyCreationResponse<CompanySuggestionResposne>> createCompany(
            @RequestBody CreateCompanyRequest companyRequest) {

        CompanySuggestionResposne data = companyService.createCompany(companyRequest);


        CompanyCreationResponse<CompanySuggestionResposne> response = new CompanyCreationResponse<>(
                "Company created successfully!",
                data
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/search")
    public List<CompanySuggestionResposne> searchCompanies(
            @RequestParam(value = "query", required = false, defaultValue = "") String query) {
        return companyService.searchCompanies(query);
    }

}
