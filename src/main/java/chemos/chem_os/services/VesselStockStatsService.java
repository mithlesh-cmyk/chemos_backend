package chemos.chem_os.services;

import chemos.chem_os.dto.ProductStockBreakdownResponse;
import chemos.chem_os.dto.VesselGroupCompany;
import chemos.chem_os.dto.VesselStockGroupAggregate;
import chemos.chem_os.dto.VesselStockStatsResponse;
import chemos.chem_os.dto.VesselStockStatsSummaryResponse;
import chemos.chem_os.model.IncomingUnsoldSnapshot;
import chemos.chem_os.repository.IncomingUnsoldSnapshotRepository;
import chemos.chem_os.repository.PhysicalStockRepository;
import chemos.chem_os.repository.PurchaseRepository;
import chemos.chem_os.repository.SalesRepository;
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

        Map<GroupKey, Double> physicalOpeningByGroup = toMap(physicalStockRepository.sumPhysicalStockOpeningByGroup());
        Map<GroupKey, Double> physicalSoldByGroup = toMap(salesRepository.sumReadyMarketSoldByGroup(today));
        Map<GroupKey, Double> incomingNewByGroup = toMap(purchaseRepository.sumIncomingNewByGroup(today));
        Map<GroupKey, Double> incomingSoldByGroup = toMap(salesRepository.sumIncomingSoldByGroup(today));
        Map<GroupKey, Double> physicalReadyByGroup = toMap(purchaseRepository.sumPhysicalReadyByGroup());
        Map<GroupKey, String> companyByGroup = toCompanyMap(purchaseRepository.findCompanyToByGroup());
        Map<GroupKey, String> salesCompanyByGroup = toCompanyMap(salesRepository.findCompanyFromByGroup());

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

            results.add(new VesselStockStatsResponse(
                    key.vesselName(), cleanProductName(key.product()), key.dischargePort(),
                    physicalStockOpening, physicalSold, physicalUnsoldClosing,
                    incomingUnsoldOpening, incomingUnsoldNew, incomingSold, incomingUnsoldClosing,
                    totalStock, companyName
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

            results.add(new VesselStockStatsResponse(
                    key.vesselName(), cleanProductName(key.product()), key.dischargePort(),
                    physicalStockOpening, physicalSold, physicalUnsoldClosing,
                    incomingUnsoldOpening, incomingUnsoldNew, incomingSold, incomingUnsoldClosing,
                    totalStock, companyName
            ));
        }
        return results;
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
        return results;
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
}