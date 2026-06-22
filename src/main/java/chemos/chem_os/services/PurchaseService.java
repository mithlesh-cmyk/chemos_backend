package chemos.chem_os.services;

import chemos.chem_os.dto.*;
import chemos.chem_os.mapper.PurchaseMapper;
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

    public Purchase createPurchase(CreatePurchaseRequest createPurchaseRequest){
        Purchase purchase = purchaseMapper.toEntity(createPurchaseRequest);
        return purchaseRepository.save(purchase);
    }

    public List<Purchase> getAllPurchase() {
        return purchaseRepository.findAll();
    }

    public Purchase getPurchaseById(String id){
        return purchaseRepository.findById(id)
                .orElseThrow(() -> (
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Purchase not found with id: " + id
                        )
                        ));
    }

    public PurchaseComparisonResponse comparePurchases(List<String> purchaseIds) {
        if (purchaseIds == null || purchaseIds.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide at least 2 purchase IDs to compare");
        }

        List<Purchase> purchases = purchaseRepository.findAllById(purchaseIds);

        if (purchases.size() != purchaseIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "One or more purchases not found");
        }

        List<PurchaseComparisonItem> items = purchases.stream()
                .map(this::toComparisonItem)
                .toList();

        return new PurchaseComparisonResponse(items, computeHighlights(items));
    }

    private PurchaseComparisonItem toComparisonItem(Purchase p) {
        BigDecimal landedCost = Stream.of(p.getPriceInr(), p.getExpense(), p.getCustomDuty(),
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
    }

    private Map<String, FieldHighlight> computeHighlights(List<PurchaseComparisonItem> items) {
        Map<String, FieldHighlight> highlights = new LinkedHashMap<>();
        highlights.put("price_fc", computeHighlight(items, PurchaseComparisonItem::priceFc));
        highlights.put("price_inr_per_mt", computeHighlight(items, PurchaseComparisonItem::priceInrPerMt));
        highlights.put("landed_cost_per_mt", computeHighlight(items, PurchaseComparisonItem::landedCostPerMt));
        return highlights;
    }

    private FieldHighlight computeHighlight(List<PurchaseComparisonItem> items,
                                            Function<PurchaseComparisonItem, BigDecimal> extractor) {
        String bestId = null, worstId = null;
        BigDecimal min = null, max = null;

        for (PurchaseComparisonItem item : items) {
            BigDecimal val = extractor.apply(item);
            if (val == null) continue;
            if (min == null || val.compareTo(min) < 0) { min = val; bestId = item.id(); }
            if (max == null || val.compareTo(max) > 0) { max = val; worstId = item.id(); }
        }

        return new FieldHighlight(bestId, worstId);
    }
}
