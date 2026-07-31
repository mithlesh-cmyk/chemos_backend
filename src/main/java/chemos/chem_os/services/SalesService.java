package chemos.chem_os.services;

import chemos.chem_os.dto.CreateSaleRequest;
import chemos.chem_os.dto.SalesFilterRequest;
import chemos.chem_os.dto.UpdateSaleRequest;
import chemos.chem_os.mapper.SalesMapper;
import chemos.chem_os.model.Sales;
import chemos.chem_os.model.Status;
import chemos.chem_os.repository.SalesRepository;
import chemos.chem_os.repository.StatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.JoinType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    private final SalesRepository salesRepository;
    private final SalesMapper salesMapper;
    private final AuditLogService auditLogService;
    private final StatusRepository statusRepository;
    private final CurrentUserService currentUserService;

    public Sales createSale(CreateSaleRequest request){
        Sales sales = salesMapper.toEntity(request);
        String currentUser = currentUserService.getUsername();
        sales.setCreatedBy(currentUser);
        sales.setUpdatedBy(currentUser);
        Sales saved = salesRepository.save(sales);
        auditLogService.log("CREATE", "SALE", saved.getId(), null, saved);
        return saved;
    }

    public Page<Sales> getAllSales(String status, String product, Pageable pageable) {
        Specification<Sales> spec = (root, query, cb) -> cb.conjunction();
        if (status != null && !status.isBlank()) {
            String statusFilter = status.trim();
            spec = spec.and((root, query, cb) -> {
                var statusJoin = root.join("status", JoinType.LEFT);
                return cb.or(
                        cb.equal(cb.upper(statusJoin.get("id")), statusFilter.toUpperCase()),
                        cb.equal(cb.lower(statusJoin.get("name")), statusFilter.toLowerCase())
                );
            });
        }
        if (product != null && !product.isBlank()) {
            String productFilter = product.trim();
            spec = spec.and((root, query, cb) -> {
                var productJoin = root.join("product", JoinType.LEFT);
                return cb.or(
                        cb.equal(productJoin.get("id"), productFilter),
                        cb.equal(cb.lower(productJoin.get("name")), productFilter.toLowerCase())
                );
            });
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
        sale.setUpdatedBy(currentUserService.getUsername());
        Sales saved = salesRepository.save(sale);
        auditLogService.log("UPDATE", "SALE", saved.getId(), snapshot, saved);
        return saved;
    }

    public Sales confirmSale(String id) {
        Sales before = getSaleById(id);
        if ("CONFIRMED".equals(before.getStatus().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sale is already confirmed");
        }
        Sales snapshot = before.toBuilder().build();
        before.setStatus(resolveStatus("CONFIRMED"));
        before.setConfirmedAt(LocalDateTime.now(BUSINESS_ZONE));
        before.setUpdatedBy(currentUserService.getUsername());
        Sales saved = salesRepository.save(before);
        auditLogService.log("CONFIRM", "SALE", saved.getId(), snapshot, saved);
        return saved;
    }

    public Sales cancelSale(String id) {
        Sales before = getSaleById(id);
        if ("CANCELLED".equals(before.getStatus().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sale is already cancelled");
        }
        Sales snapshot = before.toBuilder().build();
        before.setStatus(resolveStatus("CANCELLED"));
        before.setUpdatedBy(currentUserService.getUsername());
        Sales saved = salesRepository.save(before);
        auditLogService.log("CANCEL", "SALE", saved.getId(), snapshot, saved);
        return saved;
    }

    public Sales unconfirmSale(String id) {
        Sales before = getSaleById(id);
        if ("UNCONFIRMED".equals(before.getStatus().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sale is already unconfirmed");
        }
        Sales snapshot = before.toBuilder().build();
        before.setStatus(resolveStatus("UNCONFIRMED"));
        before.setConfirmedAt(null);
        before.setUpdatedBy(currentUserService.getUsername());
        Sales saved = salesRepository.save(before);
        auditLogService.log("UNCONFIRM", "SALE", saved.getId(), snapshot, saved);
        return saved;
    }

    private Status resolveStatus(String statusId) {
        return statusRepository.findById(statusId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Status not seeded: " + statusId));
    }

    public Page<Sales> getFilteredSales(SalesFilterRequest filters, Pageable pageable) {

        LocalDate effectiveStart = filters.startDate() != null ? filters.startDate() : LocalDate.of(1900, 1, 1);
        LocalDate effectiveEnd = filters.endDate() != null ? filters.endDate() : LocalDate.of(2999, 12, 31);

        return salesRepository.findWithFilters(
                filters.productId(),
                filters.companyTo(),
                filters.port(),
                effectiveStart,
                effectiveEnd,
                pageable
        );
    }
}