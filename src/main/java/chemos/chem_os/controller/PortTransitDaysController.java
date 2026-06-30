package chemos.chem_os.controller;

import chemos.chem_os.dto.CreatePortTransitDaysRequest;
import chemos.chem_os.dto.PortTransitDaysResponse;
import chemos.chem_os.services.PortTransitDaysService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/transit-days")
public class PortTransitDaysController {

    private final PortTransitDaysService portTransitDaysService;

    @GetMapping
    public ResponseEntity<List<PortTransitDaysResponse>> getAll() {
        return ResponseEntity.ok(portTransitDaysService.getAll());
    }

    @GetMapping("/lookup")
    public ResponseEntity<PortTransitDaysResponse> lookup(
            @RequestParam("from_port_id") String fromPortId,
            @RequestParam("to_port_id") String toPortId) {

        return portTransitDaysService.findByPorts(fromPortId, toPortId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PortTransitDaysResponse> create(
            @Valid @RequestBody CreatePortTransitDaysRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(portTransitDaysService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PortTransitDaysResponse> updateDays(
            @PathVariable String id,
            @RequestBody Map<String, Integer> body) {

        Integer days = body.get("days");
        if (days == null || days < 1) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(portTransitDaysService.updateDays(id, days));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        portTransitDaysService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
