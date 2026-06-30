package chemos.chem_os.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePortTransitDaysRequest(

        @NotBlank(message = "from_port_id is required")
        @JsonProperty("from_port_id")
        String fromPortId,

        @NotBlank(message = "to_port_id is required")
        @JsonProperty("to_port_id")
        String toPortId,

        @NotNull(message = "days is required")
        @Min(value = 1, message = "days must be at least 1")
        Integer days
) {
}
