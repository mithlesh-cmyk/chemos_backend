package chemos.chem_os.services;

import chemos.chem_os.dto.CreatePortTransitDaysRequest;
import chemos.chem_os.dto.PortSuggestionResposne;
import chemos.chem_os.dto.PortTransitDaysResponse;
import chemos.chem_os.mapper.PortMapper;
import chemos.chem_os.model.PortTransitDays;
import chemos.chem_os.model.Ports;
import chemos.chem_os.repository.PortRepository;
import chemos.chem_os.repository.PortTransitDaysRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PortTransitDaysService {

    private final PortTransitDaysRepository portTransitDaysRepository;
    private final PortRepository portRepository;
    private final PortMapper portMapper;

    public List<PortTransitDaysResponse> getAll() {
        return portTransitDaysRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public Optional<PortTransitDaysResponse> findByPorts(String fromPortId, String toPortId) {
        return portTransitDaysRepository.findByFromPortIdAndToPortId(fromPortId, toPortId)
                .map(this::toResponse);
    }

    @Transactional
    public PortTransitDaysResponse create(CreatePortTransitDaysRequest request) {
        if (portTransitDaysRepository.existsByFromPortIdAndToPortId(request.fromPortId(), request.toPortId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transit days already exist for this port pair");
        }

        Ports fromPort = portRepository.findById(request.fromPortId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Port not found: " + request.fromPortId()));

        Ports toPort = portRepository.findById(request.toPortId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Port not found: " + request.toPortId()));

        PortTransitDays saved = portTransitDaysRepository.save(
                PortTransitDays.builder()
                        .fromPort(fromPort)
                        .toPort(toPort)
                        .days(request.days())
                        .build()
        );

        return toResponse(saved);
    }

    @Transactional
    public PortTransitDaysResponse updateDays(String id, Integer days) {
        PortTransitDays existing = portTransitDaysRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Transit days entry not found: " + id));

        existing.setDays(days);
        return toResponse(portTransitDaysRepository.save(existing));
    }

    @Transactional
    public void delete(String id) {
        if (!portTransitDaysRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Transit days entry not found: " + id);
        }
        portTransitDaysRepository.deleteById(id);
    }

    private PortTransitDaysResponse toResponse(PortTransitDays entity) {
        PortSuggestionResposne fromPort = portMapper.toSuggestionResponse(entity.getFromPort());
        PortSuggestionResposne toPort = portMapper.toSuggestionResponse(entity.getToPort());
        return new PortTransitDaysResponse(entity.getId(), fromPort, toPort, entity.getDays());
    }
}
