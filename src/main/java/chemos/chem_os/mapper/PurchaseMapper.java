package chemos.chem_os.mapper;

import chemos.chem_os.dto.CreatePurchaseRequest;
import chemos.chem_os.model.Purchase;
import org.springframework.stereotype.Component;

@Component
public class PurchaseMapper {

    public Purchase toEntity(CreatePurchaseRequest request) {

        return Purchase.builder()
                .companyTo(request.companyTo())
                .purchaseType(request.purchaseType())
                .companyFrom(request.companyFrom())
                .product(request.product())
                .vesselName(request.vesselName())
                .shipment(request.shipment())
                .quantity(request.quantity())
                .priceFc(request.priceFc())
                .offerUsd(request.offerUsd())
                .exchangeRate(request.exchangeRate())
                .priceInr(request.priceInr())
                .deliveryTerm(request.deliveryTerm())
                .paymentDays(request.paymentDays())
                .port(request.port())
                .marketPrice(request.marketPrice())
                .marketStatus(request.marketStatus())
                .costPrice(request.costPrice())
                .replacementCost(request.replacementCost())
                .make(request.make())
                .packaging(request.packaging())
                .origin(request.origin())
                .expense(request.expense())
                .customDuty(request.customDuty())
                .sws(request.sws())
                .add(request.add())
                .otherExpense(request.otherExpense())
                .dischargePorts(request.dischargePorts())
                .priceType(request.priceType())
                .paymentTerm(request.paymentTerm())
                .etd(request.etd())
                .eta(request.eta())
                .build();
    }
}
