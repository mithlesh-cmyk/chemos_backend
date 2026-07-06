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
        Map<GroupKey, String> companyByGroup = toCompanyMap(purchaseRepository.findCompanyFromByGroup());

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
            String companyName = companyByGroup.get(key);

            results.add(new VesselStockStatsResponse(
                    key.vesselName(), key.product(), key.dischargePort(),
                    physicalStockOpening, physicalSold, physicalUnsoldClosing,
                    incomingUnsoldOpening, incomingUnsoldNew, incomingSold, incomingUnsoldClosing,
                    totalStock, companyName
            ));
        }
        return results;
    }

    @Transactional(readOnly = true)
    public List<ProductStockBreakdownResponse> getProductBreakdown() {
        Map<ProductPortKey, List<VesselStockStatsResponse>> byProductPort = computeGroupStats().stream()
                .collect(Collectors.groupingBy(
                        r -> new ProductPortKey(r.product(), r.dischargePort()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<ProductStockBreakdownResponse> results = new ArrayList<>();
        for (Map.Entry<ProductPortKey, List<VesselStockStatsResponse>> entry : byProductPort.entrySet()) {
            List<VesselStockStatsResponse> rows = entry.getValue();
            results.add(new ProductStockBreakdownResponse(
                    entry.getKey().product(), entry.getKey().dischargePort(),
                    sumField(rows, VesselStockStatsResponse::physicalStockOpening),
                    sumField(rows, VesselStockStatsResponse::physicalSold),
                    sumField(rows, VesselStockStatsResponse::physicalUnsoldClosing),
                    sumField(rows, VesselStockStatsResponse::incomingUnsoldOpening),
                    sumField(rows, VesselStockStatsResponse::incomingUnsoldNew),
                    sumField(rows, VesselStockStatsResponse::incomingSold),
                    sumField(rows, VesselStockStatsResponse::incomingUnsoldClosing),
                    sumField(rows, VesselStockStatsResponse::totalStock),
                    joinCompanies(rows)
            ));
        }
        return results;
    }

    private String joinCompanies(List<VesselStockStatsResponse> rows) {
        return rows.stream()
                .map(VesselStockStatsResponse::companyName)
                .filter(c -> c != null && !c.isBlank())
                .flatMap(c -> java.util.Arrays.stream(c.split(",\\s*")))
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .collect(Collectors.joining(", "));
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
                    .findBySnapshotDateAndVesselNameAndProductAndPort(today, key.vesselName(), key.product(), key.dischargePort())
                    .orElse(IncomingUnsoldSnapshot.builder()
                            .snapshotDate(today)
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

        auditLogService.log("SNAPSHOT", "INCOMING_UNSOLD_SNAPSHOT", today.toString(), null, upserted);
        log.info("Incoming-unsold nightly snapshot complete for {}: {} group(s) upserted", today, upserted);
    }

    private double resolveIncomingOpening(GroupKey key, LocalDate today) {
        return snapshotRepository
                .findTopByVesselNameAndProductAndPortAndSnapshotDateLessThanOrderBySnapshotDateDesc(
                        key.vesselName(), key.product(), key.dischargePort(), today)
                .map(IncomingUnsoldSnapshot::getIncomingUnsoldClosing)
                .orElse(0.0);
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
                        Collectors.mapping(VesselGroupCompany::companyFrom, Collectors.toCollection(LinkedHashSet::new)),
                        companies -> String.join(", ", companies))));
    }

    private record GroupKey(String vesselName, String product, String dischargePort) {
    }

    private record ProductPortKey(String product, String dischargePort) {
    }
}
