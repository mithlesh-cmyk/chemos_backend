package chemos.chem_os.mapper;

import chemos.chem_os.dto.PaymentTermResponse;
import chemos.chem_os.model.PaymentTerms;
import org.springframework.stereotype.Component;

@Component
public class PaymentTermMapper {

    public PaymentTermResponse toResponse(PaymentTerms paymentTerms) {
        if (paymentTerms == null) {
            return null;
        }
        return new PaymentTermResponse(
                paymentTerms.getId(),
                paymentTerms.getPaymentTerm(),
                paymentTerms.getCreditDays()
        );
    }
}
