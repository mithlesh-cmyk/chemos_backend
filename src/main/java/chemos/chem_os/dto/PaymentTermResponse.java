package chemos.chem_os.dto;

public record PaymentTermResponse(
        Integer id,
        String paymentTerm,
        Integer creditDays
) {
}
