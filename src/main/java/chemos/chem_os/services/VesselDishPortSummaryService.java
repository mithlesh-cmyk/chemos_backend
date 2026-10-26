package chemos.chem_os.services;

import chemos.chem_os.dto.VesselDishPortSummaryResponse;
import chemos.chem_os.model.VesselDishPortSummary;
import chemos.chem_os.repository.VesselDishPortSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VesselDishPortSummaryService {

    private final VesselDishPortSummaryRepository repository;

    public List<VesselDishPortSummaryResponse> getAll(String productId) {
        if (productId != null && (productId.isBlank() || productId.length() > 255)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "product_id must be non-blank and at most 255 characters");
        }
        List<VesselDishPortSummary> summaries = productId == null
                ? repository.findAll(Sort.by("id"))
                : repository.findByProductIdOrderByIdAsc(productId);
        return summaries.stream()
                .map(this::toResponse)
                .toList();
    }

    private VesselDishPortSummaryResponse toResponse(VesselDishPortSummary entity) {
        return new VesselDishPortSummaryResponse(
                entity.getId(), entity.getProduct(), entity.getVesselName(),
                entity.getDishPortClean(), entity.getTotalQty(), entity.getShipmentCount(),
                entity.getAvgWaitingDays(), entity.getAvgBerthDays(), entity.getDishPortShare(),
                entity.getShipId(), entity.getProductId(), entity.getPortId(), entity.getCreatedAt());
    }
}
