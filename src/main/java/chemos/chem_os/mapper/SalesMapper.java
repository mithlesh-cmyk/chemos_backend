package chemos.chem_os.mapper;

import chemos.chem_os.dto.CreateSaleRequest;
import chemos.chem_os.model.Sales;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

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
}
