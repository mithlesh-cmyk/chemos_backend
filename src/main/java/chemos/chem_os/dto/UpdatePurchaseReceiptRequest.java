package chemos.chem_os.dto;

import java.time.LocalDate;

public record UpdatePurchaseReceiptRequest(
        Double quantityReceived,
        LocalDate payDueDate
) {}
