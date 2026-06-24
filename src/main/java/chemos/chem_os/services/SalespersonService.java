package chemos.chem_os.services;

import chemos.chem_os.dto.CreateSalespersonRequest;
import chemos.chem_os.dto.SalespersonSuggestionResponse;
import chemos.chem_os.mapper.SalespersonMapper;
import chemos.chem_os.model.Salespersons;
import chemos.chem_os.repository.SalespersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SalespersonService {

    private final SalespersonRepository salespersonRepository;
    private final SalespersonMapper salespersonMapper;

    @Transactional(readOnly = true)
    public List<SalespersonSuggestionResponse> searchSalespersons(String query) {

        String cleanQuery = (query == null) ? "" : query.trim();

        return salespersonRepository.searchSalespersons(cleanQuery)
                .stream()
                .map(salespersonMapper::toSuggestionResponse)
                .toList();
    }

    @Transactional
    public SalespersonSuggestionResponse createSalesperson(CreateSalespersonRequest request) {

        Salespersons salesperson = Salespersons.builder()
                .name(request.name().trim())
                .build();

        Salespersons savedSalesperson = salespersonRepository.save(salesperson);

        return salespersonMapper.toSuggestionResponse(savedSalesperson);
    }
}