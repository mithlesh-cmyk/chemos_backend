package chemos.chem_os.dto;

import java.util.List;

public record SalesCsvImportResult(int updated, int skipped, List<String> errors) {}
