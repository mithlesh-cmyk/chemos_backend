package chemos.chem_os.dto;

import lombok.Builder;

@Builder
public record SalespersonSuggestionResponse(
        Long id,
        String name
) {
}