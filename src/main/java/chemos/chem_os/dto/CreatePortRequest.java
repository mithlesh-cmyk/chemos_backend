package chemos.chem_os.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePortRequest(
        @NotBlank(message = "Port name cannot be blank")
        String portName
) {
}
