package chemos.chem_os.services;

import chemos.chem_os.CompnayAlreadyExistsException;
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
    public CompanySuggestionResposne createCompany(CreateCompanyRequest companyRequest) {
        String displayName = CompanySanitizer.sanitizeDisplayName(companyRequest.companyName());
        String searchKey = CompanySanitizer.createSearchKey(companyRequest.companyName());


        if (companyRepository.findBySearchKey(searchKey).isPresent()) {
            throw new CompnayAlreadyExistsException("A company with this name already exists in the system!");
        }

        Companies companies = Companies.builder()
                .displayName(displayName)
                .searchKey(searchKey)
                .build();

        Companies savedCompany = companyRepository.save(companies);
        return companyMapper.toResponse(savedCompany);
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
}
