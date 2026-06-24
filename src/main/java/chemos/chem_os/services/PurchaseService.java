package chemos.chem_os.services;

import chemos.chem_os.dto.CreatePurchaseRequest;
import chemos.chem_os.dto.FieldHighlight;
import chemos.chem_os.dto.PurchaseComparisonItem;
import chemos.chem_os.dto.PurchaseComparisonResponse;
import chemos.chem_os.dto.UpdatePurchaseRequest;
import chemos.chem_os.mapper.PurchaseMapper;
import chemos.chem_os.model.EntryStatus;
import chemos.chem_os.model.Purchase;
import chemos.chem_os.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
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
