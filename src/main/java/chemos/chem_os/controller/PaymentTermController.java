package chemos.chem_os.controller;

import chemos.chem_os.dto.PaymentTermResponse;
import chemos.chem_os.services.PaymentTermService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payment-terms")
public class PaymentTermController {

    private final PaymentTermService paymentTermService;

    @GetMapping
    public ResponseEntity<List<PaymentTermResponse>> getAllPaymentTerms() {
        return ResponseEntity.ok(paymentTermService.getAllActivePaymentTerms());
    }
}
