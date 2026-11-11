package chemos.chem_os.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ShipSuggestionResponse(
        @JsonProperty("ship_id") Long shipId,
        @JsonProperty("ship_name") String shipName,
        @JsonProperty("my_ship") boolean myShip
) {
}
