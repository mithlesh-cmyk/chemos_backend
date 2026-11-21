package chemos.chem_os.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdateSaleRequest(
        String salesType,
        String companyTo,
        String companyFrom,
        @JsonAlias({"product", "productId"})
        String productId,
        @JsonProperty("file_no")
        @JsonAlias("fileNo")
        String fileNo,
        Double quantity,
        Double price,
        String payment,
        String deliveryTerm,
        String port,
        Double marketPrice,
        String marketStatus,
        Integer storageDays,
        String make,
        String packaging,
        String origin,
        String transitTolerance,
        String message,
        String vesselName,
        String remarks,
        String salesPerson,
        String brokerName
) {}