package chemos.chem_os.services;

import chemos.chem_os.CompnayAlreadyExistsException;
import chemos.chem_os.dto.CompanyCreationResponse;
import chemos.chem_os.dto.CompanySuggestionResposne;
import chemos.chem_os.dto.CreateCompanyRequest;
import chemos.chem_os.mapper.CompanyMapper;
import chemos.chem_os.model.Companies;
import chemos.chem_os.repository.CompanyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;

    @Transactional
    public CompanyCreationResponse createCompany(CreateCompanyRequest companyRequest) {

        String displayName = CompanySanitizer.sanitizeDisplayName(companyRequest.companyName());
        String searchKey = CompanySanitizer.createSearchKey(companyRequest.companyName());

        Companies existing = companyRepository.findBySearchKey(searchKey)
                .orElse(null);

        if (existing != null) {

            if (Boolean.TRUE.equals(existing.getIsActive())) {
                throw new CompnayAlreadyExistsException(
                        "A company with this name already exists."
                );
            }

            return new CompanyCreationResponse(
                    "INACTIVE_FOUND",
                    "Company already exists but is inactive.",
                    null,
                    existing.getId(),
                    true
            );
        }

        Companies companies = Companies.builder()
                .displayName(displayName)
                .searchKey(searchKey)
                .isActive(true)
                .build();

        Companies savedCompany = companyRepository.save(companies);

        return new CompanyCreationResponse(
                "CREATED",
                "Company created successfully!",
                companyMapper.toResponse(savedCompany),
                null,
                false
        );
    }
    public List<CompanySuggestionResposne> searchCompanies(String query) {

        String searchKey = (query == null || query.trim().isEmpty())
                ? ""
                : CompanySanitizer.createSearchKey(query.trim());

        return companyRepository
                .findSuggestions(searchKey, 20)
                .stream()
                .map(companyMapper::toResponse)
                .toList();
    }
    @Transactional
    public void deactivateCompany(String id) {

        Companies company = companyRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new RuntimeException("Active company not found"));

        company.setIsActive(false);

        companyRepository.save(company);
    }

    @Transactional
    public void reactivateCompany(String id) {

        Companies company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        company.setIsActive(true);

        companyRepository.save(company);
    }
    public class CompanyInactiveException extends RuntimeException {

        private final String companyId;

        public CompanyInactiveException(String message, String companyId) {
            super(message);
            this.companyId = companyId;
        }

        public String getCompanyId() {
            return companyId;
        }
    }
}
