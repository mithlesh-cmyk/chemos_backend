package chemos.chem_os.dto;

import lombok.Builder;

@Builder
public record SalespersonSuggestionResponse(
        String id,
        String name
) {
}