package chemos.chem_os.controller;

import chemos.chem_os.dto.CreatePurchaseRequest;
import chemos.chem_os.dto.UpdatePurchaseRequest;
import chemos.chem_os.model.Purchase;
import chemos.chem_os.services.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/purchase")
public class PurchaseController {

    private final PurchaseService purchaseService;

    @PostMapping("/create/purchase_order")
    public ResponseEntity<Purchase> createPurchase(@RequestBody CreatePurchaseRequest purchaseRequest){
        Purchase purchase = purchaseService.createPurchase(purchaseRequest);

        return ResponseEntity.ok(purchase);
    }

    @GetMapping("/allPurchase")
    public ResponseEntity<List<Purchase>> getAllPurchase(){
        List<Purchase> purchaseList = purchaseService.getAllPurchase();
        return ResponseEntity.ok(purchaseList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Purchase> getPurchaseById(@PathVariable String id){
        Purchase purchase = purchaseService.getPurchaseById(id);

        return ResponseEntity.ok(purchase);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Purchase> updatePurchase(
            @PathVariable String id,
            @RequestBody UpdatePurchaseRequest updateRequest) {
        Purchase purchase = purchaseService.updatePurchase(id, updateRequest);
        return ResponseEntity.ok(purchase);
    }
}
