package chemos.chem_os.services;

import chemos.chem_os.dto.CreatePurchaseRequest;
import chemos.chem_os.dto.FieldHighlight;
import chemos.chem_os.dto.PhysicalStockImportResult;
import chemos.chem_os.dto.PurchaseComparisonItem;
import chemos.chem_os.dto.PurchaseComparisonResponse;
import chemos.chem_os.dto.UpdatePurchaseRequest;
import chemos.chem_os.mapper.PurchaseMapper;
import chemos.chem_os.model.EntryStatus;
import chemos.chem_os.model.PhysicalStock;
import chemos.chem_os.model.Purchase;
import chemos.chem_os.repository.PhysicalStockRepository;
import chemos.chem_os.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PhysicalStockRepository physicalStockRepository;
    private final PurchaseMapper purchaseMapper;
    private final AuditLogService auditLogService;

    public Purchase createPurchase(CreatePurchaseRequest createPurchaseRequest) {
        Purchase purchase = purchaseMapper.toEntity(createPurchaseRequest);
        Purchase saved = purchaseRepository.save(purchase);
        auditLogService.log("CREATE", "PURCHASE", saved.getId(), null, saved);
        return saved;
    }

    public List<Purchase> getAllPurchase() {
        return purchaseRepository.findAll();
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
        purchase.setStatus(EntryStatus.CONFIRMED);
        Purchase saved = purchaseRepository.save(purchase);
        auditLogService.log("CONFIRM", "PURCHASE", saved.getId(), null, saved);
        return saved;
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
                            landedCost
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
        List<Purchase> purchases = purchaseRepository.findByStatus(EntryStatus.CONFIRMED);

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
                        p.getProduct() != null ? p.getProduct() : "",
                        p.getPort() != null ? p.getPort() : "",
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

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";

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
                PhysicalStock entry = physicalStockRepository.findByPurchaseId(purchaseId)
                        .orElse(PhysicalStock.builder().purchaseId(purchaseId).build());
                entry.setPhysicalStock(stock);
                entry.setUpdatedAt(LocalDateTime.now());
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
}
