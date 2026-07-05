package chemos.chem_os.controller;

import chemos.chem_os.dto.CreateSalePurchaseLinkRequest;
import chemos.chem_os.dto.PurchaseLinkSummaryResponse;
import chemos.chem_os.dto.SaleLinkSummaryResponse;
import chemos.chem_os.dto.SalePurchaseLinkResponse;
import chemos.chem_os.dto.UpdateSalePurchaseLinkRequest;
import chemos.chem_os.services.SalePurchaseLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/links")
public class SalePurchaseLinkController {

    private final SalePurchaseLinkService linkService;


    @PreAuthorize("hasAuthority('SALE_EDIT')")
    @PostMapping
    public ResponseEntity<SalePurchaseLinkResponse> createLink(
            @RequestBody CreateSalePurchaseLinkRequest request) {
        return ResponseEntity.ok(linkService.createLink(request));
    }


    @PreAuthorize("hasAuthority('SALE_EDIT')")
    @PutMapping("/{id}")
    public ResponseEntity<SalePurchaseLinkResponse> updateLink(
            @PathVariable String id,
            @RequestBody UpdateSalePurchaseLinkRequest request) {
        return ResponseEntity.ok(linkService.updateLink(id, request));
    }


    @PreAuthorize("hasAuthority('SALE_EDIT')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLink(@PathVariable String id) {
        linkService.deleteLink(id);
        return ResponseEntity.noContent().build();
    }


    @PreAuthorize("hasAuthority('SALE_VIEW')")
    @GetMapping("/me")
    public ResponseEntity<List<SalePurchaseLinkResponse>> getMyLinks() {
        return ResponseEntity.ok(linkService.getLinksByUser(null));
    }


    @PreAuthorize("hasAuthority('SALE_VIEW')")
    @GetMapping("/user/{username}")
    public ResponseEntity<List<SalePurchaseLinkResponse>> getLinksByUser(@PathVariable String username) {
        return ResponseEntity.ok(linkService.getLinksByUser(username));
    }


    @PreAuthorize("hasAuthority('SALE_VIEW')")
    @GetMapping("/sale/{saleId}")
    public ResponseEntity<SaleLinkSummaryResponse> getSaleLinkSummary(
            @PathVariable String saleId) {
        return ResponseEntity.ok(linkService.getSaleLinkSummary(saleId));
    }


    @PreAuthorize("hasAuthority('PURCHASE_VIEW')")
    @GetMapping("/purchase/{purchaseId}")
    public ResponseEntity<PurchaseLinkSummaryResponse> getPurchaseLinkSummary(
            @PathVariable String purchaseId) {
        return ResponseEntity.ok(linkService.getPurchaseLinkSummary(purchaseId));
    }
}
