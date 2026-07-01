package chemos.chem_os.services;

import chemos.chem_os.dto.ProductStockBreakdownResponse;
import chemos.chem_os.dto.VesselInventoryDetail;
import chemos.chem_os.dto.VesselInventoryRow;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
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

        double totalStock = matching.stream().mapToDouble(VesselStockStatsResponse::totalStock).sum();
        double physicalUnsoldClosing = matching.stream().mapToDouble(VesselStockStatsResponse::physicalUnsoldClosing).sum();
        double incomingUnsoldClosing = matching.stream().mapToDouble(VesselStockStatsResponse::incomingUnsoldClosing).sum();
        double incomingSold = matching.stream().mapToDouble(VesselStockStatsResponse::incomingSold).sum();

        return new VesselStockStatsSummaryResponse(totalStock, physicalUnsoldClosing, incomingUnsoldClosing, incomingSold);
    }

    private List<VesselStockStatsResponse> computeGroupStats() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);

        Map<GroupKey, Double> physicalOpeningByGroup = toMap(physicalStockRepository.sumPhysicalStockOpeningByGroup());
        Map<GroupKey, Double> physicalSoldByGroup = toMap(salesRepository.sumReadyMarketSoldByGroup(today));
        Map<GroupKey, Double> incomingNewByGroup = toMap(purchaseRepository.sumIncomingNewByGroup(today));
        Map<GroupKey, Double> incomingSoldByGroup = toMap(salesRepository.sumIncomingSoldByGroup(today));

        Set<GroupKey> allGroups = new LinkedHashSet<>();
        allGroups.addAll(physicalOpeningByGroup.keySet());
        allGroups.addAll(physicalSoldByGroup.keySet());
        allGroups.addAll(incomingNewByGroup.keySet());
        allGroups.addAll(incomingSoldByGroup.keySet());

        List<VesselStockStatsResponse> results = new ArrayList<>();
        for (GroupKey key : allGroups) {
            double physicalStockOpening = physicalOpeningByGroup.getOrDefault(key, 0.0);
            double physicalSold = physicalSoldByGroup.getOrDefault(key, 0.0);
            double physicalUnsoldClosing = physicalStockOpening - physicalSold;

            double incomingUnsoldOpening = resolveIncomingOpening(key, today);
            double incomingUnsoldNew = incomingNewByGroup.getOrDefault(key, 0.0);
            double incomingSold = incomingSoldByGroup.getOrDefault(key, 0.0);
            double incomingUnsoldClosing = incomingUnsoldOpening + incomingUnsoldNew - incomingSold;

            double totalStock = physicalUnsoldClosing + incomingUnsoldClosing;

            results.add(new VesselStockStatsResponse(
                    key.vesselName(), key.product(), key.port(),
                    physicalStockOpening, physicalSold, physicalUnsoldClosing,
                    incomingUnsoldOpening, incomingUnsoldNew, incomingSold, incomingUnsoldClosing,
                    totalStock
            ));
        }
        return results;
    }

    @Transactional(readOnly = true)
    public List<ProductStockBreakdownResponse> getProductBreakdown() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);

        Map<ProductPortKey, List<VesselStockStatsResponse>> byProductPort = computeGroupStats().stream()
                .collect(Collectors.groupingBy(
                        r -> new ProductPortKey(r.product(), r.port()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        Map<ProductPortKey, List<VesselInventoryDetail>> vesselInventoryByProductPort = physicalStockRepository
                .findVesselInventoryRows().stream()
                .collect(Collectors.groupingBy(
                        r -> new ProductPortKey(r.product(), r.port()),
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(Collectors.toList(), rows -> toVesselInventoryDetails(rows, today))));

        List<ProductStockBreakdownResponse> results = new ArrayList<>();
        for (Map.Entry<ProductPortKey, List<VesselStockStatsResponse>> entry : byProductPort.entrySet()) {
            List<VesselStockStatsResponse> rows = entry.getValue();
            results.add(new ProductStockBreakdownResponse(
                    entry.getKey().product(), entry.getKey().port(),
                    sumField(rows, VesselStockStatsResponse::physicalStockOpening),
                    sumField(rows, VesselStockStatsResponse::physicalSold),
                    sumField(rows, VesselStockStatsResponse::physicalUnsoldClosing),
                    sumField(rows, VesselStockStatsResponse::incomingUnsoldOpening),
                    sumField(rows, VesselStockStatsResponse::incomingUnsoldNew),
                    sumField(rows, VesselStockStatsResponse::incomingSold),
                    sumField(rows, VesselStockStatsResponse::incomingUnsoldClosing),
                    sumField(rows, VesselStockStatsResponse::totalStock),
                    vesselInventoryByProductPort.getOrDefault(entry.getKey(), List.of())
            ));
        }
        return results;
    }

    private List<VesselInventoryDetail> toVesselInventoryDetails(List<VesselInventoryRow> rows, LocalDate today) {
        return rows.stream()
                .map(r -> {
                    LocalDate eta = r.date().toLocalDate();
                    return new VesselInventoryDetail(r.vesselName(), eta, ChronoUnit.DAYS.between(eta, today), r.company());
                })
                .sorted(Comparator.comparing(VesselInventoryDetail::eta))
                .toList();
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
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        log.info("Running incoming-unsold nightly snapshot for {}", today);

        Map<GroupKey, Double> incomingNewByGroup = toMap(purchaseRepository.sumIncomingNewByGroup(today));
        Map<GroupKey, Double> incomingSoldByGroup = toMap(salesRepository.sumIncomingSoldByGroup(today));

        Set<GroupKey> groupsWithActivity = new LinkedHashSet<>();
        groupsWithActivity.addAll(incomingNewByGroup.keySet());
        groupsWithActivity.addAll(incomingSoldByGroup.keySet());

        int upserted = 0;
        for (GroupKey key : groupsWithActivity) {
            double opening = resolveIncomingOpening(key, today);
            double incomingNew = incomingNewByGroup.getOrDefault(key, 0.0);
            double incomingSold = incomingSoldByGroup.getOrDefault(key, 0.0);
            double closing = opening + incomingNew - incomingSold;

            IncomingUnsoldSnapshot snapshot = snapshotRepository
                    .findBySnapshotDateAndVesselNameAndProductAndPort(today, key.vesselName(), key.product(), key.port())
                    .orElse(IncomingUnsoldSnapshot.builder()
                            .snapshotDate(today)
                            .vesselName(key.vesselName())
                            .product(key.product())
                            .port(key.port())
                            .build());

            snapshot.setIncomingUnsoldOpening(opening);
            snapshot.setIncomingUnsoldNew(incomingNew);
            snapshot.setIncomingSold(incomingSold);
            snapshot.setIncomingUnsoldClosing(closing);
            snapshot.setComputedAt(LocalDateTime.now(BUSINESS_ZONE));

            snapshotRepository.save(snapshot);
            upserted++;
        }

        auditLogService.log("SNAPSHOT", "INCOMING_UNSOLD_SNAPSHOT", today.toString(), null, upserted);
        log.info("Incoming-unsold nightly snapshot complete for {}: {} group(s) upserted", today, upserted);
    }

    private double resolveIncomingOpening(GroupKey key, LocalDate today) {
        return snapshotRepository
                .findTopByVesselNameAndProductAndPortAndSnapshotDateLessThanOrderBySnapshotDateDesc(
                        key.vesselName(), key.product(), key.port(), today)
                .map(IncomingUnsoldSnapshot::getIncomingUnsoldClosing)
                .orElse(0.0);
    }

    private Map<GroupKey, Double> toMap(List<VesselStockGroupAggregate> rows) {
        return rows.stream().collect(Collectors.toMap(
                r -> new GroupKey(r.vesselName(), r.product(), r.port()),
                VesselStockGroupAggregate::total,
                Double::sum,
                LinkedHashMap::new));
    }

    private record GroupKey(String vesselName, String product, String port) {
    }

    private record ProductPortKey(String product, String port) {
    }
}
