package chemos.chem_os.services;

import chemos.chem_os.dto.ShipSuggestionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ShipMasterServiceTest {

    @Mock
    private NamedParameterJdbcTemplate jdbc;

    private ShipMasterService service;

    @BeforeEach
    void setUp() {
        service = new ShipMasterService(jdbc);
    }

    @Test
    void ignoresSearchTermsShorterThanTwoCharacters() {
        assertThat(service.searchShips(" A ")).isEmpty();
        verifyNoInteractions(jdbc);
    }

    @Test
    void searchesMasterShipsAndEscapesLikeWildcards() {
        ShipSuggestionResponse expected = new ShipSuggestionResponse(123L, "MV EXAMPLE", false);
        doReturn(List.of(expected)).when(jdbc)
                .query(anyString(), any(SqlParameterSource.class), any(RowMapper.class));

        assertThat(service.searchShips(" MV_% ")).containsExactly(expected);

        ArgumentCaptor<SqlParameterSource> parameters = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc).query(anyString(), parameters.capture(), any(RowMapper.class));
        assertThat(parameters.getValue().getValue("likeQuery")).isEqualTo("%MV\\_\\%%");
        assertThat(parameters.getValue().getValue("prefixQuery")).isEqualTo("MV\\_\\%%");
    }

    @Test
    void upsertsByIdAndMarksExistingShipWithoutReplacingItsName() {
        service.markAsMyShip(123L, "  USER ENTERED NAME  ");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SqlParameterSource> parameters = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc).update(sql.capture(), parameters.capture());
        assertThat(sql.getValue()).contains("ON CONFLICT (ship_id)", "SET my_ship = TRUE");
        assertThat(parameters.getValue().getValue("shipId")).isEqualTo(123L);
        assertThat(parameters.getValue().getValue("shipName")).isEqualTo("USER ENTERED NAME");
    }
}
