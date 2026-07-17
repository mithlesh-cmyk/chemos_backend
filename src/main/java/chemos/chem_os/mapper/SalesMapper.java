package chemos.chem_os.mapper;

import chemos.chem_os.dto.CreateSaleRequest;
import chemos.chem_os.dto.UpdateSaleRequest;
import chemos.chem_os.model.Ports;
import chemos.chem_os.model.Products;
import chemos.chem_os.model.Sales;
import chemos.chem_os.model.Salespersons;
import chemos.chem_os.model.Status;
import chemos.chem_os.repository.PortRepository;
import chemos.chem_os.repository.ProductRepository;
import chemos.chem_os.repository.SalespersonRepository;
import chemos.chem_os.repository.StatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class SalesMapper {

    private final ProductRepository productRepository;
    private final PortRepository portRepository;
    private final SalespersonRepository salespersonRepository;
    private final StatusRepository statusRepository;

    private Products resolveProduct(String productId) {
        if (productId == null || productId.isBlank()) return null;
        return productRepository.findById(productId)
            .or(() -> productRepository.findByNameIgnoreCase(productId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Product not found with id: " + productId
                ));
    }

    private Ports resolvePort(String portIdentifier) {
        if (portIdentifier == null || portIdentifier.isBlank()) {
            return null;
        }

        return portRepository.findById(portIdentifier)
                .or(() -> portRepository.findByDisplayNameIgnoreCase(portIdentifier))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Port not found: " + portIdentifier));
    }

    private Salespersons resolveSalesperson(String salesPersonIdentifier) {
        if (salesPersonIdentifier == null || salesPersonIdentifier.isBlank()) {
            return null;
        }

        return salespersonRepository.findById(salesPersonIdentifier)
                .or(() -> salespersonRepository.findByNameIgnoreCase(salesPersonIdentifier))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Salesperson not found: " + salesPersonIdentifier));
    }

    private Status resolveStatus(String statusId) {
        return statusRepository.findById(statusId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Status not seeded: " + statusId));
    }

    public Sales toEntity(CreateSaleRequest request) {
        return Sales.builder()
                .date(request.date() != null ? request.date() : LocalDate.now())
                .salesType(request.salesType())
                .companyFrom(request.companyFrom())
                .companyTo(request.companyTo())
                .product(resolveProduct(request.productId()))
                .quantity(request.quantity())
                .price(request.price())
                .payment(request.payment())
                .deliveryTerm(request.deliveryTerm())
                .port(resolvePort(request.port()))
                .marketPrice(request.marketPrice())
                .marketStatus(request.marketStatus())
                .storageDays(request.storageDays())
                .make(request.make())
                .packaging(request.packaging())
                .origin(request.origin())
                .transitTolerance(request.transitTolerance())
                .message(request.message())
                .vesselName(request.vesselName())
                .remarks(request.remarks())
                .salesPerson(resolveSalesperson(request.salesPerson()))
                .brokerName(request.brokerName())
                .status(resolveStatus("UNCONFIRMED"))
                .build();
    }

    public void updateEntity(Sales sale, UpdateSaleRequest request) {
        sale.setSalesType(request.salesType());
        sale.setCompanyTo(request.companyTo());
        sale.setCompanyFrom(request.companyFrom());
        sale.setProduct(resolveProduct(request.productId()));
        sale.setQuantity(request.quantity());
        sale.setPrice(request.price());
        sale.setPayment(request.payment());
        sale.setDeliveryTerm(request.deliveryTerm());
        sale.setPort(resolvePort(request.port()));
        sale.setMarketPrice(request.marketPrice());
        sale.setMarketStatus(request.marketStatus());
        sale.setStorageDays(request.storageDays());
        sale.setMake(request.make());
        sale.setPackaging(request.packaging());
        sale.setOrigin(request.origin());
        sale.setTransitTolerance(request.transitTolerance());
        sale.setMessage(request.message());
        sale.setVesselName(request.vesselName());
        sale.setRemarks(request.remarks());
        sale.setSalesPerson(resolveSalesperson(request.salesPerson()));
        sale.setBrokerName(request.brokerName());
    }
}