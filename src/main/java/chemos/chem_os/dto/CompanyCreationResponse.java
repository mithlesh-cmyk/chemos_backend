package chemos.chem_os.dto;

public record CompanyCreationResponse<T>(
        String message,
        T data
) {
}
