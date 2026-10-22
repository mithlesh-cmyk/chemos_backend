package chemos.chem_os.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"query", "total", "limit", "offset", "data"})
public record ShipPagedResponse(
        String query,
        long total,
        int limit,
        int offset,
        List<Map<String, Object>> data
) {
}
