package chemos.chem_os.controller;


import chemos.chem_os.dto.CreatePortRequest;
import chemos.chem_os.dto.PortSuggestionResposne;
import chemos.chem_os.services.PortService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ports")
public class PortController {

    private final PortService portService;

    @GetMapping("/suggestions")
    public ResponseEntity<Page<PortSuggestionResposne>> getSuggestionPorts(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "is_indian", required = false) Boolean isIndian,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<PortSuggestionResposne> responsePage = portService.searchPorts(query, isIndian, pageable);
        return ResponseEntity.ok(responsePage);
    }

    @PostMapping("/createPort")
    public ResponseEntity<PortSuggestionResposne> addCustomPort(@RequestBody CreatePortRequest createPortRequest){
        PortSuggestionResposne savedPort = portService.createCustomPort(createPortRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPort);
    }

}
