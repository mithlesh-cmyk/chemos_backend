package chemos.chem_os.services;

import chemos.chem_os.dto.ProductStockBreakdownResponse;
import chemos.chem_os.dto.VesselStockStatsResponse;
import chemos.chem_os.model.DumpInventory;
import chemos.chem_os.model.Inventory;
import chemos.chem_os.repository.DumpInventoryRepository;
import chemos.chem_os.repository.InventoryRepository;
import chemos.chem_os.repository.PhysicalStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final DumpInventoryRepository dumpInventoryRepository;
    private final VesselStockStatsService vesselStockStatsService;


    @Transactional
    public void refreshInventory() {

        // Step 1: Backup current inventory
        backupInventory();

        // Step 2: Clear inventory table
        inventoryRepository.deleteAll();

        // Step 3: Recalculate inventory
        calculateInventory();

    }

    private void backupInventory() {

        List<Inventory> inventories = inventoryRepository.findAll();

        if (inventories.isEmpty()) {
            return;
        }

        List<DumpInventory> dumpInventories = inventories.stream()
                .map(this::convertToDumpInventory)
                .toList();

        dumpInventoryRepository.saveAll(dumpInventories);
    }

    private DumpInventory convertToDumpInventory(Inventory inventory) {

        return DumpInventory.builder()
                .product(inventory.getProduct())
                .port(inventory.getPort())
                .company(inventory.getCompany())
                .physicalStock(inventory.getPhysicalStock())
                .purchaseReady(inventory.getPurchaseReady())
                .physicalSold(inventory.getPhysicalSold())
                .physicalUnsold(inventory.getPhysicalUnsold())
                .incomingStock(inventory.getIncomingStock())
                .purchaseIncoming(inventory.getPurchaseIncoming())
                .incomingSales(inventory.getIncomingSales())
                .incomingBalance(inventory.getIncomingBalance())
                .totalStock(inventory.getTotalStock())
                .lastCsvUploadedAt(inventory.getLastCsvUploadedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    private void calculateInventory() {

        LocalDateTime lastUpload =
                physicalStockRepository.findLastCsvUploadTime();

        List<ProductStockBreakdownResponse> rows =
                vesselStockStatsService.getProductBreakdown();

        List<Inventory> inventories = rows.stream()
                .map(row -> convertToInventory(row, lastUpload))
                .toList();

        inventoryRepository.saveAll(inventories);
    }

    private final PhysicalStockRepository physicalStockRepository;

    private Inventory convertToInventory(
            ProductStockBreakdownResponse response,
            LocalDateTime lastUpload) {

        return Inventory.builder()
                .product(response.product())
                .port(response.dischargePort())
                .company(response.companyName())
                .physicalStock(response.physicalStock())
                .purchaseReady(response.physicalReady())
                .physicalSold(response.physicalSold())
                .physicalUnsold(response.physicalUnsold())
                .incomingStock(response.incomingStock())
                .purchaseIncoming(response.purchaseIncoming())
                .incomingSales(response.incomingSales())
                .incomingBalance(response.incomingBalance())
                .totalStock(response.totalStock())
                .lastCsvUploadedAt(lastUpload)
                .updatedAt(LocalDateTime.now())
                .build();
    }
}