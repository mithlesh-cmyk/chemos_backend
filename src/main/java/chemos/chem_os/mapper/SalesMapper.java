package chemos.chem_os.mapper;

import chemos.chem_os.dto.CreateSaleRequest;
import chemos.chem_os.dto.UpdateSaleRequest;
import chemos.chem_os.model.Sales;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class SalesMapper {

    public Sales toEntity(CreateSaleRequest request){

        return Sales.builder()
                .date(LocalDate.now())
                .salesType(request.salesType())
                .companyFrom(request.companyFrom())
                .companyTo(request.companyTo())
                .product(request.product())
                .quantity(request.quantity())
                .price(request.price())
                .payment(request.payment())
                .deliveryTerm(request.deliveryTerm())
                .port(request.port())
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
                .build();

    }
    public void updateEntity(Sales sale, UpdateSaleRequest request) {
        sale.setSalesType(request.salesType());
        sale.setUpdatedAt(LocalDateTime.now());
        sale.setCompanyTo(request.companyTo());
        sale.setCompanyFrom(request.companyFrom());
        sale.setProduct(request.product());
        sale.setQuantity(request.quantity());
        sale.setPrice(request.price());
        sale.setPayment(request.payment());
        sale.setDeliveryTerm(request.deliveryTerm());
        sale.setPort(request.port());
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
    }
}
