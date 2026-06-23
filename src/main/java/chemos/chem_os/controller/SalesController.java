package chemos.chem_os.controller;

import chemos.chem_os.dto.CreateSaleRequest;
import chemos.chem_os.dto.UpdateSaleRequest;
import chemos.chem_os.model.Sales;
import chemos.chem_os.services.SalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/sales")
public class SalesController {

    private final SalesService salesService;

    @PostMapping("/create/sales_order")
    public ResponseEntity<Sales> salesForm(@RequestBody CreateSaleRequest salesRecord) {

        Sales savedSales = salesService.createSale(salesRecord);

        return ResponseEntity.ok(savedSales);
    }

    @GetMapping("/allSales")
    public ResponseEntity<List<Sales>> getAllSales() {
        List<Sales> sales = salesService.getAllSales();
        return ResponseEntity.ok(sales);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Sales> getSaleById(@PathVariable String id) {

        Sales sales = salesService.getSaleById(id);

        return ResponseEntity.ok(sales);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Sales> updateSale(
            @PathVariable String id,
            @RequestBody UpdateSaleRequest updateRequest) {
        Sales sale = salesService.updateSale(id, updateRequest);
        return ResponseEntity.ok(sale);
    }
}