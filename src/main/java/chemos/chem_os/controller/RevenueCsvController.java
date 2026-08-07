package chemos.chem_os.controller;

import chemos.chem_os.dto.ApiSuccessResponse;
import chemos.chem_os.dto.RevenueCsvEntryResponse;
import chemos.chem_os.dto.RevenueCsvTotalResponse;
import chemos.chem_os.dto.RevenueCsvUploadResponse;
import chemos.chem_os.services.RevenueCsvService;
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
@RequestMapping("/api/v1/revenue-csv")
public class RevenueCsvController {

    private final RevenueCsvService revenueCsvService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiSuccessResponse<RevenueCsvUploadResponse>> uploadCsv(
            @RequestParam("file") MultipartFile file) {
        RevenueCsvUploadResponse result = revenueCsvService.uploadCsv(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.<RevenueCsvUploadResponse>builder()
                        .message("CSV uploaded successfully. " + result.rowCount() + " rows saved.")
                        .data(result)
                        .build());
    }

    @GetMapping("/uploads")
    public ResponseEntity<ApiSuccessResponse<List<RevenueCsvUploadResponse>>> getAllUploads() {
        List<RevenueCsvUploadResponse> uploads = revenueCsvService.getAllUploads();
        return ResponseEntity.ok(ApiSuccessResponse.<List<RevenueCsvUploadResponse>>builder()
                .message("Uploads fetched successfully")
                .data(uploads)
                .build());
    }

    @GetMapping("/uploads/{uploadId}/entries")
    public ResponseEntity<ApiSuccessResponse<List<RevenueCsvEntryResponse>>> getEntriesByUpload(
            @PathVariable Long uploadId) {
        List<RevenueCsvEntryResponse> entries = revenueCsvService.getEntriesByUpload(uploadId);
        return ResponseEntity.ok(ApiSuccessResponse.<List<RevenueCsvEntryResponse>>builder()
                .message("Entries fetched successfully")
                .data(entries)
                .build());
    }

    @GetMapping("/uploads/entries")
    public ResponseEntity<ApiSuccessResponse<List<RevenueCsvEntryResponse>>> getEntriesByUploadAndDate(
            @RequestParam Long uploadId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<RevenueCsvEntryResponse> entries = revenueCsvService.getEntriesByUploadAndDate(uploadId, date);
        return ResponseEntity.ok(ApiSuccessResponse.<List<RevenueCsvEntryResponse>>builder()
                .message("Entries fetched successfully")
                .data(entries)
                .build());
    }

    @GetMapping("/entries/total")
    public ResponseEntity<ApiSuccessResponse<RevenueCsvTotalResponse>> getTotalAmount() {
        RevenueCsvTotalResponse total = revenueCsvService.getTotalAmount();
        return ResponseEntity.ok(ApiSuccessResponse.<RevenueCsvTotalResponse>builder()
                .message("Total amount fetched successfully")
                .data(total)
                .build());
    }
}
