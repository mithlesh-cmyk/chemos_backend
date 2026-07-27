package chemos.chem_os.dto;

public record CompanyReactivateResponse(
        String message,
        String existingCompanyId,
        boolean canReactivate
) {
}