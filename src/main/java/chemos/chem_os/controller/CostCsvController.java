package chemos.chem_os.controller;

import chemos.chem_os.dto.ApiSuccessResponse;
import chemos.chem_os.dto.CostCsvEntryResponse;
import chemos.chem_os.dto.CostCsvTotalResponse;
import chemos.chem_os.dto.CostCsvUploadResponse;
import chemos.chem_os.services.CostCsvService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cost-csv")
public class CostCsvController {

    private final CostCsvService costCsvService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiSuccessResponse<CostCsvUploadResponse>> uploadCsv(
            @RequestParam("file") MultipartFile file) {
        CostCsvUploadResponse result = costCsvService.uploadCsv(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.<CostCsvUploadResponse>builder()
                        .message("CSV uploaded successfully. " + result.rowCount() + " rows saved.")
                        .data(result)
                        .build());
    }

    @GetMapping("/uploads")
    public ResponseEntity<ApiSuccessResponse<List<CostCsvUploadResponse>>> getAllUploads() {
        List<CostCsvUploadResponse> uploads = costCsvService.getAllUploads();
        return ResponseEntity.ok(ApiSuccessResponse.<List<CostCsvUploadResponse>>builder()
                .message("Uploads fetched successfully")
                .data(uploads)
                .build());
    }

    @GetMapping("/uploads/{uploadId}/entries")
    public ResponseEntity<ApiSuccessResponse<List<CostCsvEntryResponse>>> getEntriesByUpload(
            @PathVariable Long uploadId) {
        List<CostCsvEntryResponse> entries = costCsvService.getEntriesByUpload(uploadId);
        return ResponseEntity.ok(ApiSuccessResponse.<List<CostCsvEntryResponse>>builder()
                .message("Entries fetched successfully")
                .data(entries)
                .build());
    }

    @GetMapping("/uploads/entries")
    public ResponseEntity<ApiSuccessResponse<List<CostCsvEntryResponse>>> getEntriesByUploadAndDate(
            @RequestParam Long uploadId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<CostCsvEntryResponse> entries = costCsvService.getEntriesByUploadAndDate(uploadId, date);
        return ResponseEntity.ok(ApiSuccessResponse.<List<CostCsvEntryResponse>>builder()
                .message("Entries fetched successfully")
                .data(entries)
                .build());
    }

    @GetMapping("/entries/total")
    public ResponseEntity<ApiSuccessResponse<CostCsvTotalResponse>> getTotalCost() {
        CostCsvTotalResponse total = costCsvService.getTotalCost();
        return ResponseEntity.ok(ApiSuccessResponse.<CostCsvTotalResponse>builder()
                .message("Total cost fetched successfully")
                .data(total)
                .build());
    }
}
