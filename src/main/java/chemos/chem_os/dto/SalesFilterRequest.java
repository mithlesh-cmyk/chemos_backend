package chemos.chem_os.dto;

import java.time.LocalDate;

public record SalesFilterRequest(
        String product,
        String companyTo,
        String port,
        LocalDate startDate,
        LocalDate endDate
) {}