package chemos.chem_os.services;


import chemos.chem_os.dto.CreateSaleRequest;
import chemos.chem_os.dto.SalesCsvImportResult;
import chemos.chem_os.dto.SalesFilterRequest;
import chemos.chem_os.dto.SalesLiftedValueByType;
import chemos.chem_os.dto.SalesLiftedValueSummary;
import chemos.chem_os.dto.UpdateSaleRequest;

import chemos.chem_os.dto.*;
import chemos.chem_os.mapper.SalesMapper;
import chemos.chem_os.model.Sales;
import chemos.chem_os.model.Status;
import chemos.chem_os.repository.SalesRepository;
import chemos.chem_os.repository.StatusRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.JoinType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    private static final String[] SALES_CSV_HEADERS = {
            "SALE_ID", "DATE", "SALE_TYPE", "COMPANY_TO", "COMPANY_FROM",
            "PRODUCT", "QUANTITY", "PRICE", "PAYMENT_TERM", "DELIVERY_TERM",
            "PORT", "MARKET_PRICE", "MARKET_STATUS", "STORAGE_DAYS", "MAKE",
            "PACKAGING", "ORIGIN", "TRANSIT_TOLERANCE", "MESSAGE", "VESSEL_NAME",
            "REMARKS", "SALES_PERSON", "BROKER_NAME", "STATUS",
            "LIFTED_QTY", "REMAINING_QTY"
    };

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

    public Page<Sales> getAllSales(String status, String product,  String search,Pageable pageable) {
        Specification<Sales> spec = (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
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
        if (search != null && !search.isBlank()) {

            String cleanSearch = search.trim().toLowerCase();
            String keyword = "%" + cleanSearch + "%";

            spec = spec.and((root, query, cb) -> {

                var productJoin = root.join("product", JoinType.LEFT);
                var portJoin = root.join("port", JoinType.LEFT);

                return cb.or(

                        // ------------------------
                        // Normal search
                        // ------------------------

                        cb.like(cb.lower(root.get("id")), keyword),

                        cb.like(cb.lower(root.get("companyFrom")), keyword),

                        cb.like(cb.lower(root.get("companyTo")), keyword),

                        cb.like(cb.lower(productJoin.get("id")), keyword),

                        cb.like(cb.lower(productJoin.get("name")), keyword),

                        cb.like(cb.lower(portJoin.get("displayName")), keyword),

                        // ------------------------
                        // Typo / Fuzzy search
                        // ------------------------

                        cb.greaterThan(
                                cb.function(
                                        "similarity",
                                        Double.class,
                                        cb.lower(root.get("companyFrom")),
                                        cb.literal(cleanSearch)
                                ),
                                0.25
                        ),

                        cb.greaterThan(
                                cb.function(
                                        "similarity",
                                        Double.class,
                                        cb.lower(root.get("companyTo")),
                                        cb.literal(cleanSearch)
                                ),
                                0.25
                        ),

                        cb.greaterThan(
                                cb.function(
                                        "similarity",
                                        Double.class,
                                        cb.lower(productJoin.get("name")),
                                        cb.literal(cleanSearch)
                                ),
                                0.20
                        ),

                        cb.greaterThan(
                                cb.function(
                                        "similarity",
                                        Double.class,
                                        cb.lower(portJoin.get("displayName")),
                                        cb.literal(cleanSearch)
                                ),
                                0.25
                        )
                );
            });
        }
        return salesRepository.findAll(spec, pageable);
    }

    public Sales getSaleById(String id) {
        return salesRepository.findByIdAndIsDeletedFalse(id)
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

    @Transactional(readOnly = true)
    public SalesLiftedValueSummary getLiftedValueSummary() {
        List<SalesLiftedValueByType> byType = salesRepository.sumLiftedValueBySaleType();
        double grandTotal = byType.stream()
                .mapToDouble(SalesLiftedValueByType::totalValue)
                .sum();
        return new SalesLiftedValueSummary(grandTotal, byType);
    }

    @Transactional(readOnly = true)
    public byte[] exportSalesCsv() {
        List<Sales> sales = salesRepository.findByIsDeletedFalse();

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(SALES_CSV_HEADERS)
                .build();

        StringWriter sw = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(sw, format)) {
            for (Sales s : sales) {
                printer.printRecord(
                        s.getId(),
                        s.getDate() != null ? s.getDate().toString() : "",
                        nullToEmpty(s.getSalesType()),
                        nullToEmpty(s.getCompanyTo()),
                        nullToEmpty(s.getCompanyFrom()),
                        s.getProduct() != null ? s.getProduct().getName() : "",
                        s.getQuantity() != null ? s.getQuantity() : "",
                        s.getPrice() != null ? s.getPrice() : "",
                        nullToEmpty(s.getPayment()),
                        nullToEmpty(s.getDeliveryTerm()),
                        s.getPort() != null ? s.getPort().getDisplayName() : "",
                        s.getMarketPrice() != null ? s.getMarketPrice() : "",
                        nullToEmpty(s.getMarketStatus()),
                        s.getStorageDays() != null ? s.getStorageDays() : "",
                        nullToEmpty(s.getMake()),
                        nullToEmpty(s.getPackaging()),
                        nullToEmpty(s.getOrigin()),
                        nullToEmpty(s.getTransitTolerance()),
                        nullToEmpty(s.getMessage()),
                        nullToEmpty(s.getVesselName()),
                        nullToEmpty(s.getRemarks()),
                        s.getSalesPerson() != null ? s.getSalesPerson().getName() : "",
                        nullToEmpty(s.getBrokerName()),
                        s.getStatus() != null ? s.getStatus().getName() : "",
                        s.getLiftedQty() != null ? s.getLiftedQty() : "",
                        s.getRemainingQty() != null ? s.getRemainingQty() : ""
                );
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate CSV");
        }
        return sw.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public SalesCsvImportResult importSalesCsv(MultipartFile file) {
        int updated = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(SALES_CSV_HEADERS)
                .setSkipHeaderRecord(true)
                .build();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, format)) {

            for (CSVRecord record : parser) {
                String saleId = record.get("SALE_ID");
                if (saleId == null || saleId.isBlank()) {
                    errors.add("Row " + record.getRecordNumber() + ": missing SALE_ID");
                    skipped++;
                    continue;
                }
                saleId = saleId.trim();

                Sales sale = salesRepository.findByIdAndIsDeletedFalse(saleId).orElse(null);
                if (sale == null) {
                    errors.add("Row " + record.getRecordNumber() + ": no sale found for SALE_ID=" + saleId);
                    skipped++;
                    continue;
                }

                Sales snapshot = sale.toBuilder().build();

                Double liftedQty = parseNullableDouble(record, "LIFTED_QTY", saleId, errors);
                Double remainingQty = parseNullableDouble(record, "REMAINING_QTY", saleId, errors);

                sale.setLiftedQty(liftedQty);
                sale.setRemainingQty(remainingQty);
                sale.setUpdatedBy(currentUserService.getUsername());

                Sales saved = salesRepository.save(sale);
                auditLogService.log("IMPORT_UPDATE", "SALE", saved.getId(), snapshot, saved);
                updated++;
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to parse CSV: " + e.getMessage());
        }

        return new SalesCsvImportResult(updated, skipped, errors);
    }

    private Double parseNullableDouble(CSVRecord record, String column, String saleId, List<String> errors) {
        String raw = record.get(column);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            errors.add("Row " + record.getRecordNumber() + ": invalid value '" + raw.trim()
                    + "' for " + column + " (SALE_ID=" + saleId + ") — left as null");
            return null;
        }
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    @Transactional
    public Sales updateLiftedQty(String id, UpdateLiftedQtyRequest request) {
        Sales sale = getSaleById(id);

        if (!"CONFIRMED".equals(sale.getStatus().getId())
                || !"ready".equalsIgnoreCase(sale.getMarketStatus())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Lifted quantity can only be updated for CONFIRMED and READY sales"
            );
        }

        Double payload = request.liftedQty();
        if (payload == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "liftedQty is required");
        }
        if (payload < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "liftedQty cannot be negative");
        }

        Sales snapshot = sale.toBuilder().build();

        double currentLifted = sale.getLiftedQty() != null ? sale.getLiftedQty() : 0.0;
        double newLifted = currentLifted + payload;
        double newRemaining = sale.getQuantity() - newLifted;

        sale.setLiftedQty(newLifted);
        sale.setRemainingQty(newRemaining);
        sale.setUpdatedBy(currentUserService.getUsername());

        Sales saved = salesRepository.save(sale);
        auditLogService.log("LIFT", "SALE", saved.getId(), snapshot, saved);
        return saved;
    }

    @Transactional
    public Sales markComplete(String id) {
        Sales sale = getSaleById(id);

        if (!"CONFIRMED".equals(sale.getStatus().getId())
        ) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only CONFIRMED sales can be marked complete");
        }
        if (Boolean.TRUE.equals(sale.getCompleted())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sale is already marked complete");
        }

        Sales snapshot = sale.toBuilder().build();
        sale.setCompleted(true);
        sale.setUpdatedBy(currentUserService.getUsername());

        Sales saved = salesRepository.save(sale);
        auditLogService.log("COMPLETE", "SALE", saved.getId(), snapshot, saved);
        return saved;
    }

    public void deleteSale(String id) {
        Sales sale = getSaleById(id);
        Sales snapshot = sale.toBuilder().build();
        sale.setIsDeleted(true);
        sale.setUpdatedBy(currentUserService.getUsername());
        Sales saved = salesRepository.save(sale);
        auditLogService.log("DELETE", "SALE", saved.getId(), snapshot, saved);
    }
}