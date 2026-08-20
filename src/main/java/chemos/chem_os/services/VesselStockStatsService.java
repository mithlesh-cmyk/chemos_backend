package chemos.chem_os.services;

import chemos.chem_os.dto.ProductPortFinancialSummaryResponse;
import chemos.chem_os.dto.ProductStockBreakdownResponse;
import chemos.chem_os.dto.VesselGroupCompany;
import chemos.chem_os.dto.VesselStockGroupAggregate;
import chemos.chem_os.dto.VesselStockStatsResponse;
import chemos.chem_os.dto.VesselStockStatsSummaryResponse;
import chemos.chem_os.model.IncomingUnsoldSnapshot;
import chemos.chem_os.model.Purchase;
import chemos.chem_os.model.Sales;
import chemos.chem_os.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VesselStockStatsService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    private final SalesRepository salesRepository;
    private final PurchaseRepository purchaseRepository;
    private final InventoryRepository inventoryRepository;
    private final PhysicalStockRepository physicalStockRepository;
    private final IncomingUnsoldSnapshotRepository snapshotRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<VesselStockStatsResponse> getStats() {
        return computeGroupStats();
    }

    @Transactional(readOnly = true)
    public VesselStockStatsSummaryResponse getSummary(String vesselName, String product) {
        String vesselFilter = normalize(vesselName);
        String productFilter = normalize(product);

        List<VesselStockStatsResponse> matching = computeGroupStats().stream()
                .filter(r -> vesselFilter == null || vesselFilter.equals(normalize(r.vesselName())))
                .filter(r -> productFilter == null || productFilter.equals(normalize(r.product())))
                .toList();

        double totalStock = round(matching.stream().mapToDouble(VesselStockStatsResponse::totalStock).sum());
        double physicalUnsoldClosing = round(matching.stream().mapToDouble(VesselStockStatsResponse::physicalUnsoldClosing).sum());
        double incomingUnsoldClosing = round(matching.stream().mapToDouble(VesselStockStatsResponse::incomingUnsoldClosing).sum());
        double incomingSold = round(matching.stream().mapToDouble(VesselStockStatsResponse::incomingSold).sum());

        return new VesselStockStatsSummaryResponse(totalStock, physicalUnsoldClosing, incomingUnsoldClosing, incomingSold);
    }

    private List<VesselStockStatsResponse> computeGroupStats() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);

        LocalDateTime lastCsvUpload = physicalStockRepository.findLastCsvUploadTime();

        Map<GroupKey, Double> physicalOpeningByGroup = toMap(physicalStockRepository.sumPhysicalStockOpeningByGroup());

        Map<GroupKey, Double> physicalSoldByGroup;
        Map<GroupKey, Double> physicalReadyByGroup;

        Map<GroupKey, Double> incomingNewByGroup;
        Map<GroupKey, Double> incomingSoldByGroup;

        if (lastCsvUpload == null) {
            physicalSoldByGroup = toMap(salesRepository.sumReadyMarketSoldByGroup(today));
            physicalReadyByGroup = toMap(purchaseRepository.sumPhysicalReadyByGroup());
            incomingNewByGroup = toMap(purchaseRepository.sumIncomingNewByGroup(today));
            incomingSoldByGroup = toMap(salesRepository.sumIncomingSoldByGroup(today));
        } else {
            physicalSoldByGroup = toMap(salesRepository.sumReadyMarketSoldAfter(lastCsvUpload));
            physicalReadyByGroup = toMap(purchaseRepository.sumPhysicalReadyAfter(lastCsvUpload));
            incomingNewByGroup = toMap(purchaseRepository.sumIncomingNewAfter(lastCsvUpload));
            incomingSoldByGroup = toMap(salesRepository.sumIncomingSoldAfter(lastCsvUpload));
        }

        Map<GroupKey, String> companyByGroup = toCompanyMap(purchaseRepository.findCompanyToByGroup());
        Map<GroupKey, String> salesCompanyByGroup = toCompanyMap(salesRepository.findCompanyFromByGroup());
        Map<GroupKey, Purchase> latestPurchaseByGroup = computeLatestPurchaseByGroup();

        Set<GroupKey> allGroups = new LinkedHashSet<>();
        allGroups.addAll(physicalOpeningByGroup.keySet());
        allGroups.addAll(physicalSoldByGroup.keySet());
        allGroups.addAll(incomingNewByGroup.keySet());
        allGroups.addAll(incomingSoldByGroup.keySet());
        allGroups.addAll(physicalReadyByGroup.keySet());

        List<VesselStockStatsResponse> results = new ArrayList<>();
        for (GroupKey key : allGroups) {
            double physicalStockOpening = round(physicalOpeningByGroup.getOrDefault(key, 0.0));
            double physicalSold = round(physicalSoldByGroup.getOrDefault(key, 0.0));
            double physicalReady = round(physicalReadyByGroup.getOrDefault(key, 0.0));
            double physicalUnsoldClosing = round(physicalReady + physicalStockOpening - physicalSold);

            double incomingUnsoldOpening = round(resolveIncomingOpening(key, today));
            double incomingUnsoldNew = round(incomingNewByGroup.getOrDefault(key, 0.0));
            double incomingSold = round(incomingSoldByGroup.getOrDefault(key, 0.0));
            double incomingUnsoldClosing = round(incomingUnsoldOpening + incomingUnsoldNew - incomingSold);

            double totalStock = round(physicalUnsoldClosing + incomingUnsoldClosing);
            String companyName = companyByGroup.getOrDefault(key, salesCompanyByGroup.get(key));
            Purchase latestPurchase = latestPurchaseByGroup.get(key);

            results.add(new VesselStockStatsResponse(
                    key.vesselName(), cleanProductName(key.product()), key.dischargePort(),
                    physicalStockOpening, physicalSold, physicalUnsoldClosing,
                    incomingUnsoldOpening, incomingUnsoldNew, incomingSold, incomingUnsoldClosing,
                    totalStock, companyName,
                    latestPurchase != null ? latestPurchase.getMarketPrice() : null,
                    latestPurchase != null ? latestPurchase.getReplacementCost() : null,
                    latestPurchase != null ? latestPurchase.getCreatedAt() : null,
                    latestPurchase != null ? latestPurchase.getEta() : null
            ));
        }
        return results;
    }

    private List<VesselStockStatsResponse> computeHistoricalGroupStats() {
        Map<GroupKey, Double> physicalOpeningByGroup = toMap(physicalStockRepository.sumPhysicalStockOpeningByGroup());
        Map<GroupKey, Double> physicalSoldByGroup = toMap(salesRepository.sumReadyMarketSoldAllTimeByGroup());
        Map<GroupKey, Double> incomingNewByGroup = toMap(purchaseRepository.sumIncomingAllTimeByGroup());
        Map<GroupKey, Double> incomingSoldByGroup = toMap(salesRepository.sumIncomingSoldAllTimeByGroup());
        Map<GroupKey, Double> physicalReadyByGroup = toMap(purchaseRepository.sumPhysicalReadyByGroup());
        Map<GroupKey, String> companyByGroup = toCompanyMap(purchaseRepository.findCompanyToByGroup());
        Map<GroupKey, String> salesCompanyByGroup = toCompanyMap(salesRepository.findCompanyFromByGroup());
        Map<GroupKey, Purchase> latestPurchaseByGroup = computeLatestPurchaseByGroup();

        Set<GroupKey> allGroups = new LinkedHashSet<>();
        allGroups.addAll(physicalOpeningByGroup.keySet());
        allGroups.addAll(physicalSoldByGroup.keySet());
        allGroups.addAll(incomingNewByGroup.keySet());
        allGroups.addAll(incomingSoldByGroup.keySet());
        allGroups.addAll(physicalReadyByGroup.keySet());

        List<VesselStockStatsResponse> results = new ArrayList<>();
        for (GroupKey key : allGroups) {
            double physicalStockOpening = round(physicalOpeningByGroup.getOrDefault(key, 0.0));
            double physicalSold = round(physicalSoldByGroup.getOrDefault(key, 0.0));
            double physicalReady = round(physicalReadyByGroup.getOrDefault(key, 0.0));
            double physicalUnsoldClosing = round(physicalReady + physicalStockOpening - physicalSold);

            double incomingUnsoldOpening = 0.0;
            double incomingUnsoldNew = round(incomingNewByGroup.getOrDefault(key, 0.0));
            double incomingSold = round(incomingSoldByGroup.getOrDefault(key, 0.0));
            double incomingUnsoldClosing = round(incomingUnsoldOpening + incomingUnsoldNew - incomingSold);

            double totalStock = round(physicalUnsoldClosing + incomingUnsoldClosing);
            String companyName = companyByGroup.getOrDefault(key, salesCompanyByGroup.get(key));
            Purchase latestPurchase = latestPurchaseByGroup.get(key);

            results.add(new VesselStockStatsResponse(
                    key.vesselName(), cleanProductName(key.product()), key.dischargePort(),
                    physicalStockOpening, physicalSold, physicalUnsoldClosing,
                    incomingUnsoldOpening, incomingUnsoldNew, incomingSold, incomingUnsoldClosing,
                    totalStock, companyName,
                    latestPurchase != null ? latestPurchase.getMarketPrice() : null,
                    latestPurchase != null ? latestPurchase.getReplacementCost() : null,
                    latestPurchase != null ? latestPurchase.getCreatedAt() : null,
                    latestPurchase != null ? latestPurchase.getEta() : null
            ));
        }
        return results;
    }

    private Map<GroupKey, Purchase> computeLatestPurchaseByGroup() {
        Map<GroupKey, Purchase> latestByGroup = new LinkedHashMap<>();

        for (Purchase p : purchaseRepository.findByStatus_IdAndIsDeletedFalse("CONFIRMED")) {
            if (p.getVesselName() == null || p.getProduct() == null || p.getProduct().getName() == null
                    || p.getDischargePort() == null || p.getDischargePort().getDisplayName() == null) {
                continue;
            }

            GroupKey key = new GroupKey(
                    p.getVesselName().trim().toUpperCase(),
                    p.getProduct().getName().trim().toUpperCase(),
                    p.getDischargePort().getDisplayName().trim().toUpperCase());

            Purchase existing = latestByGroup.get(key);
            if (existing == null || isAfter(p.getCreatedAt(), existing.getCreatedAt())) {
                latestByGroup.put(key, p);
            }
        }

        return latestByGroup;
    }

    private boolean isAfter(LocalDateTime candidate, LocalDateTime current) {
        if (candidate == null) {
            return false;
        }
        return current == null || candidate.isAfter(current);
    }

    @Transactional(readOnly = true)
    public List<ProductStockBreakdownResponse> getProductBreakdownHistorical() {
        Map<ProductPortKey, List<VesselStockStatsResponse>> byProductPort = computeHistoricalGroupStats().stream()
                .collect(Collectors.groupingBy(
                        r -> new ProductPortKey(r.product(), r.dischargePort()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        Map<ProductPortKey, String> companyByProductPort = toProductPortCompanyMap(purchaseRepository.findCompanyToByGroup());
        Map<ProductPortKey, String> salesCompanyByProductPort = toProductPortCompanyMap(salesRepository.findCompanyFromByGroup());

        Map<GroupKey, Double> physicalReadyByGroup = toMap(purchaseRepository.sumPhysicalReadyByGroup());
        Map<ProductPortKey, Double> physicalReadyByProductPort = physicalReadyByGroup.entrySet().stream()
                .collect(Collectors.groupingBy(
                        e -> new ProductPortKey(cleanProductName(e.getKey().product()), e.getKey().dischargePort()),
                        LinkedHashMap::new,
                        Collectors.summingDouble(Map.Entry::getValue)));

        Set<ProductPortKey> allKeys = new LinkedHashSet<>();
        allKeys.addAll(byProductPort.keySet());
        allKeys.addAll(physicalReadyByProductPort.keySet());

        List<ProductStockBreakdownResponse> results = new ArrayList<>();
        for (ProductPortKey key : allKeys) {
            List<VesselStockStatsResponse> rows = byProductPort.getOrDefault(key, List.of());
            results.add(new ProductStockBreakdownResponse(
                    key.product(), key.dischargePort(),
                    round(physicalReadyByProductPort.getOrDefault(key, 0.0)),
                    round(sumField(rows, VesselStockStatsResponse::physicalStockOpening)),
                    round(sumField(rows, VesselStockStatsResponse::physicalSold)),
                    round(sumField(rows, VesselStockStatsResponse::physicalUnsoldClosing)),
                    round(sumField(rows, VesselStockStatsResponse::incomingUnsoldOpening)),
                    round(sumField(rows, VesselStockStatsResponse::incomingUnsoldNew)),
                    round(sumField(rows, VesselStockStatsResponse::incomingSold)),
                    round(sumField(rows, VesselStockStatsResponse::incomingUnsoldClosing)),
                    round(sumField(rows, VesselStockStatsResponse::totalStock)),
                    companyByProductPort.getOrDefault(key, salesCompanyByProductPort.getOrDefault(key, ""))
            ));
        }
        return results.stream().filter(r -> !isAllZero(r)).toList();
    }

    private static boolean isAllZero(ProductStockBreakdownResponse r) {
        return r.physicalReady() == 0.0
                && r.physicalStock() == 0.0
                && r.physicalSold() == 0.0
                && r.physicalUnsold() == 0.0
                && r.incomingStock() == 0.0
                && r.purchaseIncoming() == 0.0
                && r.incomingSales() == 0.0
                && r.incomingBalance() == 0.0
                && r.totalStock() == 0.0;
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }

    private String cleanProductName(String productName) {
        if (productName == null || productName.isBlank()) {
            return productName;
        }

        String[] parts = productName.split("-");
        if (parts.length > 1) {
            String lastPart = parts[parts.length - 1];

            if (lastPart.matches("\\d{8}")) {

                StringBuilder cleanName = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) {
                    if (i > 0) cleanName.append(" ");
                    cleanName.append(parts[i]);
                }
                return cleanName.toString();
            }
        }
        return productName;
    }

    @Transactional(readOnly = true)
    public List<ProductStockBreakdownResponse> getProductBreakdown() {
        Map<ProductPortKey, List<VesselStockStatsResponse>> byProductPort = computeGroupStats().stream()
                .collect(Collectors.groupingBy(
                        r -> new ProductPortKey(r.product(), r.dischargePort()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        Map<ProductPortKey, String> companyByProductPort = toProductPortCompanyMap(purchaseRepository.findCompanyToByGroup());
        Map<ProductPortKey, String> salesCompanyByProductPort = toProductPortCompanyMap(salesRepository.findCompanyFromByGroup());

        LocalDateTime lastCsvUpload = physicalStockRepository.findLastCsvUploadTime();

        Map<GroupKey, Double> physicalReadyByGroup;
        if (lastCsvUpload == null) {
            physicalReadyByGroup = toMap(purchaseRepository.sumPhysicalReadyByGroup());
        } else {
            physicalReadyByGroup = toMap(purchaseRepository.sumPhysicalReadyAfter(lastCsvUpload));
        }

        Map<ProductPortKey, Double> physicalReadyByProductPort = physicalReadyByGroup.entrySet().stream()
                .collect(Collectors.groupingBy(
                        e -> new ProductPortKey(cleanProductName(e.getKey().product()), e.getKey().dischargePort()),
                        LinkedHashMap::new,
                        Collectors.summingDouble(Map.Entry::getValue)));

        Set<ProductPortKey> allKeys = new LinkedHashSet<>();
        allKeys.addAll(byProductPort.keySet());
        allKeys.addAll(physicalReadyByProductPort.keySet());

        List<ProductStockBreakdownResponse> results = new ArrayList<>();
        for (ProductPortKey key : allKeys) {
            List<VesselStockStatsResponse> rows = byProductPort.getOrDefault(key, List.of());
            results.add(new ProductStockBreakdownResponse(
                    key.product(), key.dischargePort(),
                    round(physicalReadyByProductPort.getOrDefault(key, 0.0)),
                    round(sumField(rows, VesselStockStatsResponse::physicalStockOpening)),
                    round(sumField(rows, VesselStockStatsResponse::physicalSold)),
                    round(sumField(rows, VesselStockStatsResponse::physicalUnsoldClosing)),
                    round(sumField(rows, VesselStockStatsResponse::incomingUnsoldOpening)),
                    round(sumField(rows, VesselStockStatsResponse::incomingUnsoldNew)),
                    round(sumField(rows, VesselStockStatsResponse::incomingSold)),
                    round(sumField(rows, VesselStockStatsResponse::incomingUnsoldClosing)),
                    round(sumField(rows, VesselStockStatsResponse::totalStock)),
                    companyByProductPort.getOrDefault(key, salesCompanyByProductPort.getOrDefault(key, ""))
            ));
        }
        return results.stream().filter(r -> !isAllZero(r)).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductPortFinancialSummaryResponse> getProductFinancialSummary() {
        Map<ProductPortKey, ProductStockBreakdownResponse> physicalByKey = getProductBreakdown().stream()
                .collect(Collectors.toMap(
                        r -> new ProductPortKey(r.product(), r.dischargePort()),
                        r -> r,
                        (a, b) -> a,
                        LinkedHashMap::new));

        Map<ProductPortKey, double[]> costAccumulator = new LinkedHashMap<>();
        Map<ProductPortKey, Double> quantityReceivedByKey = new LinkedHashMap<>();
        for (Purchase p : purchaseRepository.findByStatus_IdAndIsDeletedFalse("CONFIRMED")) {
            if (p.getProduct() == null || p.getProduct().getName() == null
                    || p.getDischargePort() == null || p.getDischargePort().getDisplayName() == null) {
                continue;
            }
            ProductPortKey key = new ProductPortKey(
                    cleanProductName(p.getProduct().getName().trim().toUpperCase()),
                    p.getDischargePort().getDisplayName().trim().toUpperCase());

            if (p.getQuantityReceived() != null) {
                quantityReceivedByKey.merge(key, p.getQuantityReceived(), Double::sum);
            }

            if (p.getPriceInr() != null && p.getQuantity() != null && p.getQuantity() != 0) {
                double[] acc = costAccumulator.computeIfAbsent(key, k -> new double[2]);
                acc[0] += p.getPriceInr().doubleValue() * p.getQuantity();
                acc[1] += p.getQuantity();
            }
        }

        Map<ProductPortKey, double[]> saleAccumulator = new LinkedHashMap<>();
        Map<ProductPortKey, Double> soldUnliftedByKey = new LinkedHashMap<>();
        for (Sales s : salesRepository.findByStatus_IdAndIsDeletedFalse("CONFIRMED")) {
            if (s.getProduct() == null || s.getProduct().getName() == null
                    || s.getPort() == null || s.getPort().getDisplayName() == null) {
                continue;
            }
            ProductPortKey key = new ProductPortKey(
                    cleanProductName(s.getProduct().getName().trim().toUpperCase()),
                    s.getPort().getDisplayName().trim().toUpperCase());

            if (s.getRemainingQty() != null) {
                soldUnliftedByKey.merge(key, s.getRemainingQty(), Double::sum);
            }

            if (s.getPrice() != null && s.getLiftedQty() != null && s.getLiftedQty() != 0) {
                double[] acc = saleAccumulator.computeIfAbsent(key, k -> new double[2]);
                acc[0] += s.getPrice() * s.getLiftedQty();
                acc[1] += s.getLiftedQty();
            }
        }

        Set<ProductPortKey> allKeys = new LinkedHashSet<>();
        allKeys.addAll(physicalByKey.keySet());
        allKeys.addAll(costAccumulator.keySet());
        allKeys.addAll(saleAccumulator.keySet());
        allKeys.addAll(soldUnliftedByKey.keySet());
        allKeys.addAll(quantityReceivedByKey.keySet());

        List<ProductPortFinancialSummaryResponse> results = new ArrayList<>();
        for (ProductPortKey key : allKeys) {
            ProductStockBreakdownResponse physical = physicalByKey.get(key);
            double physicalStock = physical != null ? physical.physicalStock() : 0.0;
            double physicalUnsold = physical != null ? physical.physicalUnsold() : 0.0;
            double soldUnlifted = round(soldUnliftedByKey.getOrDefault(key, 0.0));
            double quantityReceived = round(quantityReceivedByKey.getOrDefault(key, 0.0));
            String companyName = physical != null ? physical.companyName() : "";

            double[] costAcc = costAccumulator.get(key);
            BigDecimal averageWeightedCost = costAcc != null && costAcc[1] != 0
                    ? BigDecimal.valueOf(costAcc[0] / costAcc[1]).setScale(4, RoundingMode.HALF_UP)
                    : null;

            double[] saleAcc = saleAccumulator.get(key);
            BigDecimal averageWeightedSale = saleAcc != null && saleAcc[1] != 0
                    ? BigDecimal.valueOf(saleAcc[0] / saleAcc[1]).setScale(4, RoundingMode.HALF_UP)
                    : null;

            results.add(new ProductPortFinancialSummaryResponse(
                    key.product(), key.dischargePort(),
                    round(physicalStock), round(physicalUnsold), soldUnlifted,
                    quantityReceived, companyName,
                    averageWeightedCost, averageWeightedSale
            ));
        }
        return results;
    }

    private Map<ProductPortKey, String> toProductPortCompanyMap(List<VesselGroupCompany> rows) {
        return rows.stream().collect(Collectors.groupingBy(
                r -> new ProductPortKey(cleanProductName(r.product()), r.dischargePort()),
                LinkedHashMap::new,
                Collectors.collectingAndThen(
                        Collectors.mapping(VesselGroupCompany::companyTo, Collectors.toCollection(LinkedHashSet::new)),
                        companies -> String.join(", ", companies))));
    }

    private double sumField(List<VesselStockStatsResponse> rows, java.util.function.ToDoubleFunction<VesselStockStatsResponse> extractor) {
        return rows.stream().mapToDouble(extractor).sum();
    }

    private String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim().toUpperCase();
    }

    @Scheduled(cron = "0 30 0 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void runNightlySnapshot() {
        LocalDate snapshotDate = LocalDate.now(BUSINESS_ZONE).minusDays(1);
        log.info("Running incoming-unsold nightly snapshot for {}", snapshotDate);

        Map<GroupKey, Double> incomingNewByGroup = toMap(purchaseRepository.sumIncomingNewByGroup(snapshotDate));
        Map<GroupKey, Double> incomingSoldByGroup = toMap(salesRepository.sumIncomingSoldByGroup(snapshotDate));

        Set<GroupKey> groupsWithActivity = new LinkedHashSet<>();
        groupsWithActivity.addAll(incomingNewByGroup.keySet());
        groupsWithActivity.addAll(incomingSoldByGroup.keySet());

        int upserted = 0;
        for (GroupKey key : groupsWithActivity) {
            double opening = round(resolveIncomingOpening(key, snapshotDate));
            double incomingNew = round(incomingNewByGroup.getOrDefault(key, 0.0));
            double incomingSold = round(incomingSoldByGroup.getOrDefault(key, 0.0));
            double closing = round(opening + incomingNew - incomingSold);

            IncomingUnsoldSnapshot snapshot = snapshotRepository
                    .findBySnapshotDateAndVesselNameAndProductAndPort(snapshotDate, key.vesselName(), key.product(), key.dischargePort())
                    .orElse(IncomingUnsoldSnapshot.builder()
                            .snapshotDate(snapshotDate)
                            .vesselName(key.vesselName())
                            .product(key.product())
                            .port(key.dischargePort())
                            .build());

            snapshot.setIncomingUnsoldOpening(opening);
            snapshot.setIncomingUnsoldNew(incomingNew);
            snapshot.setIncomingSold(incomingSold);
            snapshot.setIncomingUnsoldClosing(closing);
            snapshot.setComputedAt(LocalDateTime.now(BUSINESS_ZONE));

            snapshotRepository.save(snapshot);
            upserted++;
        }

        auditLogService.log("SNAPSHOT", "INCOMING_UNSOLD_SNAPSHOT", snapshotDate.toString(), null, upserted);

    }

    private double resolveIncomingOpening(GroupKey key, LocalDate today) {
        return snapshotRepository
                .findTopByVesselNameAndProductAndPortAndSnapshotDateLessThanOrderBySnapshotDateDesc(
                        key.vesselName(), key.product(), key.dischargePort(), today)
                .map(IncomingUnsoldSnapshot::getIncomingUnsoldClosing)
                .orElseGet(() -> purchaseRepository.sumIncomingConfirmedBefore(key.vesselName(), key.product(), key.dischargePort(), today)
                        - salesRepository.sumIncomingConfirmedBefore(key.vesselName(), key.product(), key.dischargePort(), today));
    }

    private Map<GroupKey, Double> toMap(List<VesselStockGroupAggregate> rows) {
        return rows.stream().collect(Collectors.toMap(
                r -> new GroupKey(r.vesselName(), r.product(), r.dischargePort()),
                VesselStockGroupAggregate::total,
                Double::sum,
                LinkedHashMap::new));
    }

    private Map<GroupKey, String> toCompanyMap(List<VesselGroupCompany> rows) {
        return rows.stream().collect(Collectors.groupingBy(
                r -> new GroupKey(r.vesselName(), r.product(), r.dischargePort()),
                LinkedHashMap::new,
                Collectors.collectingAndThen(
                        Collectors.mapping(VesselGroupCompany::companyTo, Collectors.toCollection(LinkedHashSet::new)),
                        companies -> String.join(", ", companies))));
    }

    private record GroupKey(String vesselName, String product, String dischargePort) {
    }

    private record ProductPortKey(String product, String dischargePort) {
    }

    public LocalDateTime getLastCsvUploadedAt() {
        return inventoryRepository.getLastCsvUploadedAt();
    }
}