package chemos.chem_os.mapper;

import chemos.chem_os.dto.PortSuggestionResposne;
import chemos.chem_os.model.Ports;
import org.springframework.stereotype.Component;

@Component
public class PortMapper {
    public PortSuggestionResposne toSuggestionResponse(Ports port) {
        if (port == null) {
            return null;
        }
        return new PortSuggestionResposne(
                port.getId(),
                port.getDisplayName(),
                port.getLocode()
        );
    }
}
