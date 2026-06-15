package chemos.chem_os.mapper;

import chemos.chem_os.dto.CompanySuggestionResposne;
import chemos.chem_os.model.Companies;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public CompanySuggestionResposne toResponse(Companies company) {

        return new CompanySuggestionResposne(
                company.getId(),
                company.getDisplayName()
        );
    }


}
