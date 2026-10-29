package chemos.chem_os.services;

import chemos.chem_os.dto.ShipPagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShipDataService {

    private static final String INVENTORY_COLUMNS = """
            vessel_ship_id AS ship_id,
            COALESCE(vessel_name, vessel_ais_name) AS vessel_name,
            voyage_departure_port_name,
            voyage_departure_utc,
            voyage_departure_label,
            voyage_arrival_port_name,
            COALESCE(voyage_eta_utc, voyage_arrival_utc) AS voyage_eta_utc,
            position_area_name AS current_area,
            position_area_code AS area_code,
            vessel_type,
            COALESCE(vessel_subtype, vessel_type) AS vessel_subtype,
            vessel_updated_at
            """;

    private static final String SEARCH_SQL = """
            WITH matches AS (
                SELECT %s,
                    GREATEST(
                        similarity(COALESCE(vessel_name, ''), :q),
                        similarity(COALESCE(vessel_ais_name, ''), :q),
                        similarity(CAST(vessel_ship_id AS TEXT), :q)
                    ) AS score
                FROM mainshipdata
                WHERE
                    similarity(COALESCE(vessel_name, ''), :q) > 0.15
                    OR similarity(COALESCE(vessel_ais_name, ''), :q) > 0.15
                    OR similarity(CAST(vessel_ship_id AS TEXT), :q) > 0.15
                    OR vessel_name ILIKE :likeQ
                    OR vessel_ais_name ILIKE :likeQ
                    OR CAST(vessel_ship_id AS TEXT) ILIKE :likeQ
            )
            SELECT *, COUNT(*) OVER() AS total_count
            FROM matches
            ORDER BY score DESC, vessel_name ASC
            LIMIT :limit OFFSET :offset
            """.formatted(INVENTORY_COLUMNS);

    private static final String SHIP_NAME = "COALESCE(vessel_name, vessel_ais_name)";

    private static final String SHIP_LIST_SQL = """
            SELECT id, vessel_ship_id, %s AS vessel_name, COUNT(*) OVER() AS total_count
            FROM mainshipdata
            %%s
            ORDER BY %%s
            LIMIT :limit OFFSET :offset
            """.formatted(SHIP_NAME);

    private static final String SHIP_NAME_FILTER = "WHERE vessel_name ILIKE :likeQ OR vessel_ais_name ILIKE :likeQ";

    private static final String SHIP_SUGGEST_ORDER = """
            CASE WHEN %s ILIKE :prefixQ THEN 0 ELSE 1 END,
            GREATEST(similarity(COALESCE(vessel_name, ''), :q),
                     similarity(COALESCE(vessel_ais_name, ''), :q)) DESC,
            %s ASC, id ASC""".formatted(SHIP_NAME, SHIP_NAME);

    private final NamedParameterJdbcTemplate jdbc;

    public ShipPagedResponse getMainShipData(int limit, int offset) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM mainshipdata ORDER BY id LIMIT :limit OFFSET :offset",
                pageParams(limit, offset));
        return new ShipPagedResponse(null, countAll(), limit, offset, rows);
    }

    public ShipPagedResponse getShipInventory(int limit, int offset) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT " + INVENTORY_COLUMNS + " FROM mainshipdata ORDER BY id LIMIT :limit OFFSET :offset",
                pageParams(limit, offset));
        return new ShipPagedResponse(null, countAll(), limit, offset, rows);
    }

    public ShipPagedResponse searchShipInventory(String q, int limit, int offset) {
        MapSqlParameterSource params = pageParams(limit, offset)
                .addValue("q", q)
                .addValue("likeQ", "%" + q + "%");
        List<Map<String, Object>> rows = jdbc.queryForList(SEARCH_SQL, params);

        long total = rows.isEmpty() ? 0 : ((Number) rows.get(0).get("total_count")).longValue();
        rows.forEach(row -> {
            row.remove("score");
            row.remove("total_count");
        });
        return new ShipPagedResponse(q, total, limit, offset, rows);
    }

    /** Lightweight ship list (id, vessel_ship_id, vessel_name); with a non-blank q it doubles as name suggestions. */
    public ShipPagedResponse getShips(String q, int limit, int offset) {
        String term = q == null ? "" : q.trim();
        MapSqlParameterSource params = pageParams(limit, offset);
        String sql;
        if (term.isEmpty()) {
            term = null;
            sql = SHIP_LIST_SQL.formatted("", "id");
        } else {
            String escaped = term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            params.addValue("q", term)
                    .addValue("likeQ", "%" + escaped + "%")
                    .addValue("prefixQ", escaped + "%");
            sql = SHIP_LIST_SQL.formatted(SHIP_NAME_FILTER, SHIP_SUGGEST_ORDER);
        }
        List<Map<String, Object>> rows = jdbc.queryForList(sql, params);
        long total = rows.isEmpty() ? 0 : ((Number) rows.get(0).get("total_count")).longValue();
        rows.forEach(row -> row.remove("total_count"));
        return new ShipPagedResponse(term, total, limit, offset, rows);
    }

    private long countAll() {
        Long total = jdbc.getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM mainshipdata", Long.class);
        return total == null ? 0 : total;
    }

    private MapSqlParameterSource pageParams(int limit, int offset) {
        return new MapSqlParameterSource()
                .addValue("limit", limit)
                .addValue("offset", offset);
    }
}
