package chemos.chem_os.mapper;

import chemos.chem_os.dto.CreatePurchaseRequest;
import chemos.chem_os.model.Ports;
import chemos.chem_os.model.Purchase;
import chemos.chem_os.repository.PortRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import chemos.chem_os.dto.UpdatePurchaseRequest;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class PurchaseMapper {

    private final PortRepository portRepository;

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
                .currency(request.currency())
                .offerUsd(request.offerUsd())
                .exchangeRate(request.exchangeRate())
                .priceInr(request.priceInr())
                .deliveryTerm(request.deliveryTerm())
                .paymentDays(request.paymentDays())
                .port(resolvePort(request.port()))
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
                .dischargePort(resolvePort(request.dischargePorts()))
                .priceType(request.priceType())
                .paymentTerm(request.paymentTerm())
                .etd(request.etd())
                .eta(request.eta())
                .build();
    }

    public void updateEntity(Purchase purchase, UpdatePurchaseRequest request) {
        purchase.setCompanyTo(request.companyTo());
        purchase.setPurchaseType(request.purchaseType());
        purchase.setCompanyFrom(request.companyFrom());
        purchase.setProduct(request.product());
        purchase.setVesselName(request.vesselName());
        purchase.setShipment(request.shipment());
        purchase.setQuantity(request.quantity());
        purchase.setPriceFc(request.priceFc());
        purchase.setCurrency(request.currency());
        purchase.setOfferUsd(request.offerUsd());
        purchase.setExchangeRate(request.exchangeRate());
        purchase.setPriceInr(request.priceInr());
        purchase.setDeliveryTerm(request.deliveryTerm());
        purchase.setPaymentDays(request.paymentDays());
        purchase.setPort(resolvePort(request.port()));
        purchase.setMarketPrice(request.marketPrice());
        purchase.setMarketStatus(request.marketStatus());
        purchase.setCostPrice(request.costPrice());
        purchase.setReplacementCost(request.replacementCost());
        purchase.setMake(request.make());
        purchase.setPackaging(request.packaging());
        purchase.setOrigin(request.origin());
        purchase.setExpense(request.expense());
        purchase.setCustomDuty(request.customDuty());
        purchase.setSws(request.sws());
        purchase.setAdd(request.add());
        purchase.setOtherExpense(request.otherExpense());
        purchase.setDischargePort(resolvePort(request.dischargePorts()));
        purchase.setPriceType(request.priceType());
        purchase.setPaymentTerm(request.paymentTerm());
        purchase.setEtd(request.etd());
        purchase.setEta(request.eta());
    }

    private Ports resolvePort(String portIdentifier) {
        if (portIdentifier == null || portIdentifier.isBlank()) return null;
        return portRepository.findById(portIdentifier)
                .or(() -> portRepository.findByDisplayNameIgnoreCase(portIdentifier))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Port not found: " + portIdentifier));
    }
}