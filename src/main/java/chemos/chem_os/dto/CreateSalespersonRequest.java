package chemos.chem_os.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSalespersonRequest(

        @NotBlank(message = "Salesperson name is required")
        String name

) {
}