package chemos.chem_os.services;

import chemos.chem_os.dto.ShipSuggestionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShipMasterService {

    private static final String SEARCH_SHIPS_SQL = """
            SELECT ship_id, ship_name, my_ship
            FROM shipwithid
            WHERE ship_name ILIKE :likeQuery
            ORDER BY
                CASE WHEN ship_name ILIKE :prefixQuery THEN 0 ELSE 1 END,
                my_ship DESC,
                ship_name ASC
            LIMIT 10
            """;

    private static final String MARK_AS_MY_SHIP_SQL = """
            INSERT INTO shipwithid (ship_id, ship_name, my_ship)
            VALUES (:shipId, :shipName, TRUE)
            ON CONFLICT (ship_id) DO UPDATE
            SET my_ship = TRUE
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public List<ShipSuggestionResponse> searchShips(String query) {
        String term = query == null ? "" : query.trim();
        if (term.length() < 2) {
            return List.of();
        }

        String escapedTerm = term.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("likeQuery", "%" + escapedTerm + "%")
                .addValue("prefixQuery", escapedTerm + "%");

        return jdbc.query(SEARCH_SHIPS_SQL, params, (rs, rowNum) ->
                new ShipSuggestionResponse(
                        rs.getLong("ship_id"),
                        rs.getString("ship_name"),
                        rs.getBoolean("my_ship")
                ));
    }

    public void markAsMyShip(Long shipId, String shipName) {
        jdbc.update(MARK_AS_MY_SHIP_SQL, new MapSqlParameterSource()
                .addValue("shipId", shipId)
                .addValue("shipName", shipName.trim()));
    }
}
