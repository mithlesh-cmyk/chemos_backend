package chemos.chem_os.mapper;

import chemos.chem_os.dto.SalespersonSuggestionResponse;
import chemos.chem_os.model.Salespersons;
import org.springframework.stereotype.Component;

@Component
public class SalespersonMapper {

    public SalespersonSuggestionResponse toSuggestionResponse(Salespersons salesperson) {
        return SalespersonSuggestionResponse.builder()
                .id(salesperson.getId())
                .name(salesperson.getName())
                .build();
    }
}