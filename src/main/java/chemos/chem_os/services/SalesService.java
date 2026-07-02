package chemos.chem_os.services;

import chemos.chem_os.dto.CreateSaleRequest;
import chemos.chem_os.dto.SalesFilterRequest;
import chemos.chem_os.dto.UpdateSaleRequest;
import chemos.chem_os.mapper.SalesMapper;
import chemos.chem_os.model.EntryStatus;
import chemos.chem_os.model.Sales;
import chemos.chem_os.repository.SalesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final SalesRepository salesRepository;
    private final SalesMapper salesMapper;
    private final AuditLogService auditLogService;

    public Sales createSale(CreateSaleRequest request){
        Sales sales = salesMapper.toEntity(request);
        Sales saved = salesRepository.save(sales);
        auditLogService.log("CREATE", "SALE", saved.getId(), null, saved);
        return saved;
    }

    public Page<Sales> getAllSales(EntryStatus status, String product, Pageable pageable) {
        Specification<Sales> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (product != null && !product.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("product")), product.trim().toLowerCase()));
        }
        return salesRepository.findAll(spec, pageable);
    }

    public Sales getSaleById(String id) {
        return salesRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sale not found with id: " + id
                ));
    }

    public Sales updateSale(String id, UpdateSaleRequest request) {
        Sales sale = getSaleById(id);
        Sales snapshot = sale.toBuilder().build();
        salesMapper.updateEntity(sale, request);
        Sales saved = salesRepository.save(sale);
        auditLogService.log("UPDATE", "SALE", saved.getId(), snapshot, saved);
        return saved;
    }

    public Sales confirmSale(String id) {
        Sales before = getSaleById(id);
        if (before.getStatus() == EntryStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sale is already confirmed");
        }
        Sales snapshot = before.toBuilder().build();
        before.setStatus(EntryStatus.CONFIRMED);
        Sales saved = salesRepository.save(before);
        auditLogService.log("CONFIRM", "SALE", saved.getId(), snapshot, saved);
        return saved;
    }

    public Page<Sales> getFilteredSales(SalesFilterRequest filters, Pageable pageable) {

        LocalDate effectiveStart = filters.startDate() != null ? filters.startDate() : LocalDate.of(1900, 1, 1);
        LocalDate effectiveEnd = filters.endDate() != null ? filters.endDate() : LocalDate.of(2999, 12, 31);

        return salesRepository.findWithFilters(
                filters.product(),
                filters.companyTo(),
                filters.port(),
                effectiveStart,
                effectiveEnd,
                pageable
        );
    }
}
