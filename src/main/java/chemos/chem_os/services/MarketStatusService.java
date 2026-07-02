package chemos.chem_os.services;

import chemos.chem_os.dto.MarketStatusResponse;
import chemos.chem_os.repository.MarketStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketStatusService {

    private final MarketStatusRepository marketStatusRepository;

    public List<MarketStatusResponse> getAllMarketStatuses() {
        return marketStatusRepository.findAll()
                .stream()
                .map(ms -> new MarketStatusResponse(ms.getId(), ms.getName()))
                .toList();
    }
}