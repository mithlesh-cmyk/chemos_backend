package chemos.chem_os.services;

import chemos.chem_os.dto.CreatePortRequest;
import chemos.chem_os.dto.PortSuggestionResposne;
import chemos.chem_os.mapper.PortMapper;
import chemos.chem_os.model.Ports;
import chemos.chem_os.repository.PortRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

import java.nio.channels.IllegalChannelGroupException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PortService {

    private final PortRepository portRepository;
    private final PortMapper portMapper;


    @Transactional(readOnly = true)
    public Page<PortSuggestionResposne> searchPorts(String query, Pageable pageable) {

        String cleanQuery = (query == null || query.trim().isEmpty())
                ? ""
                : query.trim().replaceAll("\\s+", " ");

        return portRepository.searchPorts(cleanQuery, pageable)
                .map(portMapper::toSuggestionResponse);
    }

    public PortSuggestionResposne createCustomPort(CreatePortRequest createPortRequest){
        String sanitizedName = createPortRequest.portName().trim().replaceAll("\\s+", " ").toUpperCase();

        String searchKey = sanitizedName.toLowerCase();

        if(portRepository.existsBySearchKey(searchKey)){
            throw new IllegalChannelGroupException();
        }

        Ports newPort = Ports.builder()
                .displayName(sanitizedName)
                .searchKey(searchKey)
                .locode(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Ports savedPort = portRepository.save(newPort);

        return portMapper.toSuggestionResponse(savedPort);
    }
}
