package chemos.chem_os.services;

import chemos.chem_os.dto.*;
import chemos.chem_os.mapper.PurchaseMapper;
import chemos.chem_os.model.PhysicalStock;
import chemos.chem_os.model.Purchase;
import chemos.chem_os.model.Status;
import chemos.chem_os.repository.PhysicalStockRepository;
import chemos.chem_os.repository.PortTransitDaysRepository;
import chemos.chem_os.repository.PurchaseRepository;
import chemos.chem_os.repository.StatusRepository;
import org.springframework.data.jpa.domain.Specification;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.criteria.JoinType;

import java.util.List;


@Service
@RequiredArgsConstructor
public class
PurchaseService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");
    private final PurchaseRepository purchaseRepository;
    private final PhysicalStockRepository physicalStockRepository;
    private final PurchaseMapper purchaseMapper;
    private final AuditLogService auditLogService;
    private final PortTransitDaysRepository portTransitDaysRepository;
    private final StatusRepository statusRepository;
    private final CurrentUserService currentUserService;

    public Purchase createPurchase(CreatePurchaseRequest createPurchaseRequest) {
        Purchase purchase = purchaseMapper.toEntity(createPurchaseRequest);
        String currentUser = currentUserService.getUsername();
        purchase.setCreatedBy(currentUser);
        purchase.setUpdatedBy(currentUser);
        Purchase saved = purchaseRepository.save(purchase);
        auditLogService.log("CREATE", "PURCHASE", saved.getId(), null, saved);
        return saved;
    }

    public Page<Purchase> getAllPurchase(
            String status,
            String product,
            Pageable pageable) {

        Specification<Purchase> spec = (root, query, cb) -> cb.conjunction();

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
                        cb.equal(
                                cb.lower(productJoin.get("name")),
                                productFilter.toLowerCase()
                        )
                );
            });
        }

        return purchaseRepository.findAll(spec, pageable);
    }

    public Purchase getPurchaseById(String id) {
        return purchaseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Purchase not found with id: " + id
                ));
    }

    public Purchase updatePurchase(String id, UpdatePurchaseRequest updateRequest) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Purchase not found with id: " + id
                ));
        Purchase before = purchase.toBuilder().build();
        purchaseMapper.updateEntity(purchase, updateRequest);
        purchase.setUpdatedBy(currentUserService.getUsername());
        Purchase saved = purchaseRepository.save(purchase);
        auditLogService.log("UPDATE", "PURCHASE", saved.getId(), before, saved);
        return saved;
    }

    public Purchase updatePurchaseReceipt(
            String id,
            UpdatePurchaseReceiptRequest request) {

        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Purchase not found with id: " + id
                ));

        Purchase before = purchase.toBuilder().build();

        purchase.setQuantityReceived(request.quantityReceived());
        purchase.setPayDueDate(request.payDueDate());

        purchase.setUpdatedBy(currentUserService.getUsername());

        Purchase saved = purchaseRepository.save(purchase);

        auditLogService.log("UPDATE", "PURCHASE", saved.getId(), before, saved);

        return saved;
    }

    public Purchase confirmPurchase(String id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Purchase not found with id: " + id
                ));
        purchase.setStatus(resolveStatus("CONFIRMED"));
        purchase.setConfirmedAt(LocalDateTime.now(BUSINESS_ZONE));
        purchase.setUpdatedBy(currentUserService.getUsername());
        Purchase saved = purchaseRepository.save(purchase);
        auditLogService.log("CONFIRM", "PURCHASE", saved.getId(), null, saved);
        return saved;
    }

    public Purchase cancelPurchase(String id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Purchase not found with id: " + id
                ));
        purchase.setStatus(resolveStatus("CANCELLED"));
        purchase.setUpdatedBy(currentUserService.getUsername());
        Purchase saved = purchaseRepository.save(purchase);
        auditLogService.log("CANCEL", "PURCHASE", saved.getId(), null, saved);
        return saved;
    }

    public Purchase unconfirmPurchase(String id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Purchase not found with id: " + id
                ));
        if ("UNCONFIRMED".equals(purchase.getStatus().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Purchase is already unconfirmed");
        }
        purchase.setStatus(resolveStatus("UNCONFIRMED"));
        purchase.setUpdatedBy(currentUserService.getUsername());
        Purchase saved = purchaseRepository.save(purchase);
        auditLogService.log("UNCONFIRM", "PURCHASE", saved.getId(), null, saved);
        return saved;
    }

    private Status resolveStatus(String statusId) {
        return statusRepository.findById(statusId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Status not seeded: " + statusId));
    }

    public PurchaseComparisonResponse comparePurchases(List<String> purchaseIds) {
        List<Purchase> purchases = purchaseRepository.findAllById(purchaseIds);

        List<PurchaseComparisonItem> items = purchases.stream()
                .map(p -> {
                    BigDecimal landedCost = Stream.of(
                                    p.getPriceInr(), p.getExpense(), p.getCustomDuty(),
                                    p.getSws(), p.getAdd(), p.getOtherExpense())
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    Integer transitDays = null;
                    if (p.getPort() != null && p.getDischargePort() != null) {
                        transitDays = portTransitDaysRepository
                                .findFirstByFromPortIdAndToPortId(p.getPort().getId(), p.getDischargePort().getId())
                                .map(td -> td.getDays())
                                .orElse(null);
                    }

                    return new PurchaseComparisonItem(
                            p.getId(),
                            p.getCompanyFrom(),
                            p.getQuantity(),
                            p.getDeliveryTerm(),
                            p.getPriceFc(),
                            p.getCurrency(),
                            p.getExchangeRate(),
                            p.getPriceInr(),
                            p.getEta(),
                            p.getExpense(),
                            p.getCustomDuty(),
                            p.getSws(),
                            p.getAdd(),
                            p.getOtherExpense(),
                            landedCost,
                            transitDays
                    );
                })
                .toList();

        Map<String, FieldHighlight> highlights = new LinkedHashMap<>();
        highlights.put("priceFc",       highlight(items, PurchaseComparisonItem::priceFc));
        highlights.put("exchangeRate",  highlight(items, PurchaseComparisonItem::exchangeRate));
        highlights.put("priceInrPerMt", highlight(items, PurchaseComparisonItem::priceInrPerMt));
        highlights.put("expense",       highlight(items, PurchaseComparisonItem::expense));
        highlights.put("customDuty",    highlight(items, PurchaseComparisonItem::customDuty));
        highlights.put("sws",           highlight(items, PurchaseComparisonItem::sws));
        highlights.put("add",           highlight(items, PurchaseComparisonItem::add));
        highlights.put("otherExpense",  highlight(items, PurchaseComparisonItem::otherExpense));
        highlights.put("landedCostPerMt", highlight(items, PurchaseComparisonItem::landedCostPerMt));

        return new PurchaseComparisonResponse(items, highlights);
    }

    @Transactional(readOnly = true)
    public byte[] exportPhysicalStockCsv() {
        List<Purchase> purchases = purchaseRepository.findByStatus_Id("CONFIRMED");

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("PURCHASE_ID", "VESSEL_DATE", "VESSEL_NAME", "PRODUCT", "PORT", "PHYSICAL_STOCK")
                .build();

        StringWriter sw = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(sw, format)) {
            for (Purchase p : purchases) {
                printer.printRecord(
                        p.getId(),
                        p.getEta() != null ? p.getEta().toString() : "",
                        p.getVesselName() != null ? p.getVesselName() : "",
                        p.getProduct() != null ? p.getProduct().getName() : "",
                        p.getPort() != null ? p.getPort().getDisplayName() : "",
                        ""  // always blank — user fills this in
                );
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate CSV");
        }
        return sw.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public PhysicalStockImportResult importPhysicalStock(MultipartFile file) {
        int updated = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        String currentUser = currentUserService.getUsername();

        LocalDateTime sessionTimestamp = LocalDateTime.now();

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("PURCHASE_ID", "VESSEL_DATE", "VESSEL_NAME", "PRODUCT", "PORT", "PHYSICAL_STOCK")
                .setSkipHeaderRecord(true)
                .build();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, format)) {

            for (CSVRecord record : parser) {
                String purchaseId = record.get("PURCHASE_ID").trim();
                String stockStr = record.get("PHYSICAL_STOCK").trim();

                if (stockStr.isEmpty()) {
                    skipped++;
                    continue;
                }

                double stock;
                try {
                    stock = Double.parseDouble(stockStr);
                } catch (NumberFormatException e) {
                    errors.add("Row " + record.getRecordNumber() + ": invalid value '" + stockStr + "' for PURCHASE_ID=" + purchaseId);
                    skipped++;
                    continue;
                }

                if (!purchaseRepository.existsById(purchaseId)) {
                    errors.add("Row " + record.getRecordNumber() + ": no purchase found for PURCHASE_ID=" + purchaseId);
                    skipped++;
                    continue;
                }

                // upsert — overwrite if already exists for this purchase
//                PhysicalStock entry = physicalStockRepository.findByPurchaseId(purchaseId)
//                        .orElse(PhysicalStock.builder().purchaseId(purchaseId).build());
//                entry.setPhysicalStock(stock);
//                entry.setUpdatedAt(sessionTimestamp);
//                entry.setUpdatedBy(currentUser);
//                physicalStockRepository.save(entry);
//                updated++;
                //Changed the upsert behav. for storing old values
                PhysicalStock entry = physicalStockRepository.findByPurchaseId(purchaseId)
                        .orElse(PhysicalStock.builder().purchaseId(purchaseId).build());

                double oldValue = entry.getPhysicalStock() != null ? entry.getPhysicalStock() : -1;

                if (oldValue == stock) {
                    // value didn't actually change — count as skipped
                    skipped++;
                    continue;
                }

// value changed — store previous, update current
                entry.setPreviousStock(oldValue != -1 ? oldValue : null);
                entry.setPhysicalStock(stock);
                entry.setUpdatedAt(sessionTimestamp);
                entry.setUpdatedBy(currentUser);
                physicalStockRepository.save(entry);
                updated++;

            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to parse CSV: " + e.getMessage());
        }

        PhysicalStockImportResult result = new PhysicalStockImportResult(updated, skipped, errors);
        auditLogService.log("IMPORT", "PHYSICAL_STOCK", null, null, result);
        return result;
    }

    private FieldHighlight highlight(List<PurchaseComparisonItem> items,
                                     Function<PurchaseComparisonItem, BigDecimal> extractor) {
        String bestId = items.stream()
                .filter(i -> extractor.apply(i) != null)
                .min(Comparator.comparing(extractor))
                .map(PurchaseComparisonItem::id)
                .orElse(null);
        String worstId = items.stream()
                .filter(i -> extractor.apply(i) != null)
                .max(Comparator.comparing(extractor))
                .map(PurchaseComparisonItem::id)
                .orElse(null);
        return new FieldHighlight(bestId, worstId);
    }

    @Transactional(readOnly = true)
    public List<PhysicalStockSessionSummary> getStockUpdateSessions(String user) {
        return (user == null || user.isBlank())
                ? physicalStockRepository.findSessionSummaries()
                : physicalStockRepository.findSessionSummariesByUser(user);
    }

    @Transactional(readOnly = true)
    public List<PhysicalStock> getStockUpdateSessionDetail(String user, LocalDateTime timestamp) {
        return physicalStockRepository.findByUpdatedByAndUpdatedAt(user, timestamp);
    }
}
