package chemos.chem_os.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FieldHighlight(
        @JsonProperty("best_id")
        String bestId,

        @JsonProperty("worst_id")
        String worstId
) {
}
