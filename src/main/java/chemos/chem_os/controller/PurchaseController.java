package chemos.chem_os.controller;

import chemos.chem_os.dto.CreatePurchaseRequest;
import chemos.chem_os.dto.PurchaseComparisonRequest;
import chemos.chem_os.dto.PurchaseComparisonResponse;
import chemos.chem_os.model.Purchase;
import chemos.chem_os.services.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<List<Purchase>> getAllPurchase() {
        List<Purchase> purchaseList = purchaseService.getAllPurchase();
        return ResponseEntity.ok(purchaseList);
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

    @PreAuthorize("hasAuthority('PURCHASE_APPROVE')")
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<Purchase> confirmPurchase(@PathVariable String id) {
        return ResponseEntity.ok(purchaseService.confirmPurchase(id));
    }
}
