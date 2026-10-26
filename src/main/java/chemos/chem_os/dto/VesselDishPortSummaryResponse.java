package chemos.chem_os.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VesselDishPortSummaryResponse(
        Long id,
        String product,
        @JsonProperty("vessel_name") String vesselName,
        @JsonProperty("dish_port_clean") String dishPortClean,
        @JsonProperty("total_qty") BigDecimal totalQty,
        @JsonProperty("shipment_count") Integer shipmentCount,
        @JsonProperty("avg_waiting_days") BigDecimal avgWaitingDays,
        @JsonProperty("avg_berth_days") BigDecimal avgBerthDays,
        @JsonProperty("dish_port_share") BigDecimal dishPortShare,
        @JsonProperty("ship_id") Long shipId,
        @JsonProperty("product_id") String productId,
        @JsonProperty("port_id") String portId,
        @JsonProperty("created_at") LocalDateTime createdAt
) {
}
