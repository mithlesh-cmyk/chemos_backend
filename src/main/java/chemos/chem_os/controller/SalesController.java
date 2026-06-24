package chemos.chem_os.controller;

import chemos.chem_os.dto.CreateSaleRequest;
import chemos.chem_os.dto.UpdateSaleRequest;
import chemos.chem_os.model.Sales;
import chemos.chem_os.services.SalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/sales")
public class SalesController {

    private final SalesService salesService;

    @PreAuthorize("hasAuthority('SALE_CREATE')")
    @PostMapping("/create/sales_order")
    public ResponseEntity<Sales> salesForm(@RequestBody CreateSaleRequest salesRecord){
        Sales savedSales = salesService.createSale(salesRecord);
        return ResponseEntity.ok(savedSales);
    }

    @PreAuthorize("hasAuthority('SALE_VIEW')")
    @GetMapping("/allSales")
    public ResponseEntity<Page<Sales>> getAllSales(@PageableDefault(size = 10, sort = "date", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Sales> sales = salesService.getAllSales(pageable);
        return ResponseEntity.ok(sales);
    }

    @PreAuthorize("hasAuthority('SALE_VIEW')")
    @GetMapping("/{id}")
    public ResponseEntity<Sales> getSaleById(@PathVariable String id){
        Sales sales = salesService.getSaleById(id);
        return ResponseEntity.ok(sales);
    }

    @PreAuthorize("hasAuthority('SALE_EDIT')")
    @PutMapping("/{id}")
    public ResponseEntity<Sales> updateSale(@PathVariable String id, @RequestBody UpdateSaleRequest request) {
        return ResponseEntity.ok(salesService.updateSale(id, request));
    }

    @PreAuthorize("hasAuthority('SALE_APPROVE')")
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<Sales> confirmSale(@PathVariable String id) {
        return ResponseEntity.ok(salesService.confirmSale(id));
    }
}
