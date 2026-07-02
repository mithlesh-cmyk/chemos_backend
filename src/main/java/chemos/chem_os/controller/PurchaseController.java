package chemos.chem_os.controller;

import chemos.chem_os.dto.*;
import chemos.chem_os.model.EntryStatus;
import chemos.chem_os.model.PhysicalStock;
import chemos.chem_os.model.Purchase;
import chemos.chem_os.services.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/purchase")
public class PurchaseController {

    private final PurchaseService purchaseService;

    @PreAuthorize("hasAuthority('PURCHASE_CREATE')")
    @PostMapping("/create/purchase_order")
    public ResponseEntity<Purchase> createPurchase(@RequestBody CreatePurchaseRequest purchaseRequest) {
        Purchase purchase = purchaseService.createPurchase(purchaseRequest);
        return ResponseEntity.ok(purchase);
    }

    @PreAuthorize("hasAuthority('PURCHASE_VIEW')")
    @GetMapping("/allPurchase")
    public ResponseEntity<List<Purchase>> getAllPurchase(
            @RequestParam(required = false) EntryStatus status,
            @RequestParam(required = false) String product) {
        return ResponseEntity.ok(purchaseService.getAllPurchase(status, product));
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
