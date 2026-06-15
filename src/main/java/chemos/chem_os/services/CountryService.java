package chemos.chem_os.services;

import chemos.chem_os.dto.CountrySuggestionResponse;
import chemos.chem_os.mapper.CountryMapper;
import chemos.chem_os.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CountryService {

    private final CountryRepository countryRepository;
    private final CountryMapper countryMapper;

    @Transactional(readOnly = true)
    public Page<CountrySuggestionResponse> searchCountries(String query, Pageable pageable) {

        String cleanQuery = (query == null || query.trim().isEmpty())
                ? ""
                : query.trim().replaceAll("\\s+", " ");

        return countryRepository.searchCountries(cleanQuery, pageable)
                .map(countryMapper::toSuggestionResponse);
    }
}
