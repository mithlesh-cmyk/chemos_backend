package chemos.chem_os.mapper;

import chemos.chem_os.dto.CreatePurchaseRequest;
import chemos.chem_os.dto.UpdatePurchaseRequest;
import chemos.chem_os.model.*;
import chemos.chem_os.repository.CountryRepository;
import chemos.chem_os.repository.PaymentTermRepository;
import chemos.chem_os.repository.PortRepository;
import chemos.chem_os.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class PurchaseMapper {

    private final PortRepository portRepository;
    private final CountryRepository countryRepository;
    private final ProductRepository productRepository;
    private final PaymentTermRepository paymentTermRepository;

    public Purchase toEntity(CreatePurchaseRequest request) {

        return Purchase.builder()
                .companyTo(request.companyTo())
                .purchaseType(request.purchaseType())
                .companyFrom(request.companyFrom())
                .product(resolveProduct(request.product()))
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
                .replacementCost(request.replacementCost())
                .make(request.make())
                .packaging(request.packaging())
                .origin(resolveCountry(request.origin()))
                .expense(request.expense())
                .customDuty(request.customDuty())
                .sws(request.sws())
                .add(request.add())
                .otherExpense(request.otherExpense())
                .dischargePort(resolvePort(request.dischargePorts()))
                .priceType(request.priceType())
                .paymentTerm(resolvePaymentTerm(request.paymentTerm()))
                .etd(request.etd())
                .eta(request.eta())
                .build();
    }

    public void updateEntity(Purchase purchase, UpdatePurchaseRequest request) {
        purchase.setCompanyTo(request.companyTo());
        purchase.setPurchaseType(request.purchaseType());
        purchase.setCompanyFrom(request.companyFrom());
        purchase.setProduct(resolveProduct(request.product()));
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
        purchase.setReplacementCost(request.replacementCost());
        purchase.setMake(request.make());
        purchase.setPackaging(request.packaging());
        purchase.setOrigin(resolveCountry(request.origin()));
        purchase.setExpense(request.expense());
        purchase.setCustomDuty(request.customDuty());
        purchase.setSws(request.sws());
        purchase.setAdd(request.add());
        purchase.setOtherExpense(request.otherExpense());
        purchase.setDischargePort(resolvePort(request.dischargePorts()));
        purchase.setPriceType(request.priceType());
        purchase.setPaymentTerm(resolvePaymentTerm(request.paymentTerm()));
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

    private Countries resolveCountry(String countryId) {
        if (countryId == null || countryId.isBlank()) return null;
        return countryRepository.findById(countryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Country not found: " + countryId));
    }

    private Products resolveProduct(String productId) {
        if (productId == null || productId.isBlank()) return null;
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Product not found: " + productId));
    }

    private PaymentTerms resolvePaymentTerm(Integer paymentTermId) {
        if (paymentTermId == null) {
            return null;
        }

        return paymentTermRepository.findById(paymentTermId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Payment term not found: " + paymentTermId));
    }
}
