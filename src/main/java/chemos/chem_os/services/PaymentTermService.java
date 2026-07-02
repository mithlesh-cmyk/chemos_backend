package chemos.chem_os.services;

import chemos.chem_os.dto.PaymentTermResponse;
import chemos.chem_os.mapper.PaymentTermMapper;
import chemos.chem_os.repository.PaymentTermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentTermService {

    private final PaymentTermRepository paymentTermRepository;
    private final PaymentTermMapper paymentTermMapper;

    @Transactional(readOnly = true)
    public List<PaymentTermResponse> getAllActivePaymentTerms() {
        return paymentTermRepository.findAllActive()
                .stream()
                .map(paymentTermMapper::toResponse)
                .toList();
    }
}
