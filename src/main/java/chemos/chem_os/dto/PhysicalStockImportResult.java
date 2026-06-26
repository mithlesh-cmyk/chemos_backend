package chemos.chem_os.dto;

import java.util.List;

public record PhysicalStockImportResult(int updated, int skipped, List<String> errors) {}
