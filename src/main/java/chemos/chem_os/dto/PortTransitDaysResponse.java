package chemos.chem_os.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PortTransitDaysResponse(
        String id,

        @JsonProperty("from_port")
        PortSuggestionResposne fromPort,

        @JsonProperty("to_port")
        PortSuggestionResposne toPort,

        Integer days
) {
}
