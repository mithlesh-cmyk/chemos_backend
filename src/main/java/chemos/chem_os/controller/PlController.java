package chemos.chem_os.controller;

import chemos.chem_os.dto.ApiSuccessResponse;
import chemos.chem_os.dto.PlEntryResponse;
import chemos.chem_os.dto.PlUploadResponse;
import chemos.chem_os.services.PlService;
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
@RequestMapping("/api/v1/pl")
public class PlController {

    private final PlService plService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiSuccessResponse<PlUploadResponse>> uploadCsv(
            @RequestParam("file") MultipartFile file) {
        PlUploadResponse result = plService.uploadCsv(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.<PlUploadResponse>builder()
                        .message("CSV uploaded successfully. " + result.rowCount() + " rows saved.")
                        .data(result)
                        .build());
    }

    @GetMapping("/uploads")
    public ResponseEntity<ApiSuccessResponse<List<PlUploadResponse>>> getAllUploads() {
        List<PlUploadResponse> uploads = plService.getAllUploads();
        return ResponseEntity.ok(ApiSuccessResponse.<List<PlUploadResponse>>builder()
                .message("Uploads fetched successfully")
                .data(uploads)
                .build());
    }

    @GetMapping("/uploads/{uploadId}/entries")
    public ResponseEntity<ApiSuccessResponse<List<PlEntryResponse>>> getEntriesByUpload(
            @PathVariable Long uploadId) {
        List<PlEntryResponse> entries = plService.getEntriesByUpload(uploadId);
        return ResponseEntity.ok(ApiSuccessResponse.<List<PlEntryResponse>>builder()
                .message("Entries fetched successfully")
                .data(entries)
                .build());
    }

    @GetMapping("/uploads/entries")
    public ResponseEntity<ApiSuccessResponse<List<PlEntryResponse>>> getEntriesByUploadAndDate(
            @RequestParam Long uploadId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<PlEntryResponse> entries = plService.getEntriesByUploadAndDate(uploadId, date);
        return ResponseEntity.ok(ApiSuccessResponse.<List<PlEntryResponse>>builder()
                .message("Entries fetched successfully")
                .data(entries)
                .build());
    }
}
