package chemos.chem_os.mapper;

import chemos.chem_os.dto.CreatePurchaseRequest;
import chemos.chem_os.dto.UpdatePurchaseRequest;
import chemos.chem_os.model.*;
import chemos.chem_os.repository.CountryRepository;
import chemos.chem_os.repository.PaymentTermRepository;
import chemos.chem_os.repository.PortRepository;
import chemos.chem_os.repository.ProductRepository;
import chemos.chem_os.repository.StatusRepository;
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
    private final StatusRepository statusRepository;

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
                .status(resolveStatus("UNCONFIRMED"))
                .build();
    }

    public void updateEntity(Purchase purchase, UpdatePurchaseRequest request) {
        if (request.companyTo() != null) purchase.setCompanyTo(request.companyTo());
        if (request.purchaseType() != null) purchase.setPurchaseType(request.purchaseType());
        if (request.companyFrom() != null) purchase.setCompanyFrom(request.companyFrom());
        if (request.product() != null) purchase.setProduct(resolveProduct(request.product()));
        if (request.vesselName() != null) purchase.setVesselName(request.vesselName());
        if (request.shipment() != null) purchase.setShipment(request.shipment());
        if (request.quantity() != null) purchase.setQuantity(request.quantity());
        if (request.priceFc() != null) purchase.setPriceFc(request.priceFc());
        if (request.currency() != null) purchase.setCurrency(request.currency());
        if (request.offerUsd() != null) purchase.setOfferUsd(request.offerUsd());
        if (request.exchangeRate() != null) purchase.setExchangeRate(request.exchangeRate());
        if (request.priceInr() != null) purchase.setPriceInr(request.priceInr());
        if (request.deliveryTerm() != null) purchase.setDeliveryTerm(request.deliveryTerm());
        if (request.paymentDays() != null) purchase.setPaymentDays(request.paymentDays());
        if (request.port() != null) purchase.setPort(resolvePort(request.port()));
        if (request.marketPrice() != null) purchase.setMarketPrice(request.marketPrice());
        if (request.marketStatus() != null) purchase.setMarketStatus(request.marketStatus());
        if (request.replacementCost() != null) purchase.setReplacementCost(request.replacementCost());
        if (request.make() != null) purchase.setMake(request.make());
        if (request.packaging() != null) purchase.setPackaging(request.packaging());
        if (request.origin() != null && !request.origin().isBlank()) purchase.setOrigin(resolveCountry(request.origin()));
        if (request.expense() != null) purchase.setExpense(request.expense());
        if (request.customDuty() != null) purchase.setCustomDuty(request.customDuty());
        if (request.sws() != null) purchase.setSws(request.sws());
        if (request.add() != null) purchase.setAdd(request.add());
        if (request.otherExpense() != null) purchase.setOtherExpense(request.otherExpense());
        if (request.dischargePorts() != null) purchase.setDischargePort(resolvePort(request.dischargePorts()));
        if (request.priceType() != null) purchase.setPriceType(request.priceType());
        if (request.paymentTerm() != null) purchase.setPaymentTerm(resolvePaymentTerm(request.paymentTerm()));
        if (request.etd() != null) purchase.setEtd(request.etd());
        if (request.eta() != null) purchase.setEta(request.eta());
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

    private Status resolveStatus(String statusId) {
        return statusRepository.findById(statusId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Status not seeded: " + statusId));
    }
}
