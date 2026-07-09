package chemos.chem_os.controller;

import chemos.chem_os.dto.*;
import chemos.chem_os.model.PhysicalStock;
import chemos.chem_os.model.Purchase;
import chemos.chem_os.services.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import java.time.LocalDateTime;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/purchase")
public class PurchaseController {

    private final PurchaseService purchaseService;

    @PreAuthorize("hasAuthority('PURCHASE_CREATE')")
    @PostMapping("/create/purchase_order")
    public ResponseEntity<ApiSuccessResponse<Purchase>> createPurchase(@RequestBody CreatePurchaseRequest purchaseRequest) {
        Purchase purchase = purchaseService.createPurchase(purchaseRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiSuccessResponse.<Purchase>builder()
                        .message("Purchase created successfully.")
                        .data(purchase)
                        .build()
        );
    }

    @PreAuthorize("hasAuthority('PURCHASE_VIEW')")
    @GetMapping("/allPurchase")
<<<<<<< Updated upstream
    public ResponseEntity<Page<Purchase>> getAllPurchase(
            @RequestParam(required = false) String status,
=======
    public ResponseEntity<ApiSuccessResponse<Page<Purchase>>> getAllPurchase(
            @RequestParam(required = false) EntryStatus status,
>>>>>>> Stashed changes
            @RequestParam(required = false) String product,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        Page<Purchase> purchases =
                purchaseService.getAllPurchase(status, product, pageable);

        String message = purchases.isEmpty()
                ? "No purchases found."
                : "Purchases fetched successfully.";

        return ResponseEntity.ok(
                ApiSuccessResponse.<Page<Purchase>>builder()
                        .message(message)
                        .data(purchases)
                        .build()
        );
    }

    @PreAuthorize("hasAuthority('PURCHASE_VIEW')")
    @GetMapping("/{id}")
    public ResponseEntity<Purchase> getPurchaseById(@PathVariable String id) {
        Purchase purchase = purchaseService.getPurchaseById(id);
        return ResponseEntity.ok(purchase);
    }

    @PreAuthorize("hasAuthority('PURCHASE_VIEW')")
    @PostMapping("/compare")
    public ResponseEntity<PurchaseComparisonResponse> comparePurchases(@RequestBody PurchaseComparisonRequest request) {
        return ResponseEntity.ok(purchaseService.comparePurchases(request.purchaseIds()));
    }

    @PreAuthorize("hasAuthority('PURCHASE_EDIT')")
    @PutMapping("/{id}")
    public ResponseEntity<Purchase> updatePurchase(@PathVariable String id, @RequestBody UpdatePurchaseRequest request) {
        return ResponseEntity.ok(purchaseService.updatePurchase(id, request));
    }

    @PreAuthorize("hasAuthority('PURCHASE_APPROVE')")
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<Purchase> confirmPurchase(@PathVariable String id) {
        return ResponseEntity.ok(purchaseService.confirmPurchase(id));
    }

    @PreAuthorize("hasAuthority('PURCHASE_APPROVE')")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Purchase> cancelPurchase(@PathVariable String id) {
        return ResponseEntity.ok(purchaseService.cancelPurchase(id));
    }

    @PreAuthorize("hasAuthority('PURCHASE_APPROVE')")
    @PatchMapping("/{id}/unconfirm")
    public ResponseEntity<Purchase> unconfirmPurchase(@PathVariable String id) {
        return ResponseEntity.ok(purchaseService.unconfirmPurchase(id));
    }

    @PreAuthorize("hasAuthority('PURCHASE_VIEW')")
    @GetMapping("/export-physical-stock")
    public ResponseEntity<byte[]> exportPhysicalStock() {
        byte[] csv = purchaseService.exportPhysicalStockCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"physical_stock.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @PreAuthorize("hasAuthority('PURCHASE_EDIT')")
    @PostMapping(value = "/import-physical-stock", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhysicalStockImportResult> importPhysicalStock(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(purchaseService.importPhysicalStock(file));
    }

    @PreAuthorize("hasAuthority('PURCHASE_VIEW')")
    @GetMapping("/physical-stock/sessions")
    public ResponseEntity<List<PhysicalStockSessionSummary>> getStockUpdateSessions(
            @RequestParam(required = false) String user) {
        return ResponseEntity.ok(purchaseService.getStockUpdateSessions(user));
    }

    @PreAuthorize("hasAuthority('PURCHASE_VIEW')")
    @GetMapping("/physical-stock/sessions/detail")
    public ResponseEntity<List<PhysicalStock>> getStockUpdateSessionDetail(
            @RequestParam String user,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime timestamp) {
        return ResponseEntity.ok(purchaseService.getStockUpdateSessionDetail(user, timestamp));
    }
}
