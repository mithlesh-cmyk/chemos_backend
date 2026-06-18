package chemos.chem_os.controller;

import chemos.chem_os.dto.CreateSaleRequest;
import chemos.chem_os.model.Sales;
import chemos.chem_os.services.SalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/sales")
public class SalesController {

    private final SalesService salesService;

    @PostMapping("/create/sales_order")
    public ResponseEntity<Sales> salesForm(@RequestBody CreateSaleRequest salesRecord){

        Sales savedSales = salesService.createSale(salesRecord);

        return ResponseEntity.ok(savedSales);
    }

    @GetMapping("/allSales")
    public ResponseEntity<Page<Sales>> getAllSales(@PageableDefault(size = 10) Pageable pageable) {
        Page<Sales> sales = salesService.getAllSales(pageable);
        return ResponseEntity.ok(sales);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Sales> getSaleById(@PathVariable String id){

        Sales sales = salesService.getSaleById(id);

        return ResponseEntity.ok(sales);
    }
}
