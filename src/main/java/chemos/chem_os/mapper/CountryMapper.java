package chemos.chem_os.mapper;

import chemos.chem_os.dto.CountrySuggestionResponse;
import chemos.chem_os.model.Countries;
import org.springframework.stereotype.Component;

@Component
public class CountryMapper {

    public CountrySuggestionResponse toSuggestionResponse(Countries countries){
        if(countries == null){
            return null;
        }

        return new CountrySuggestionResponse(
                countries.getId(),
                countries.getDisplayName()
        );
    }
}
