package chemos.chem_os.services;

import chemos.chem_os.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public String getUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            String name = auth.getName();
            if (name != null && !name.isBlank() && !"anonymousUser".equals(name)) {
                return name;
            }
        }
        return "system";
    }

    // Super roles (e.g. ADMIN) bypass ownership filters and see all records.
    public boolean isSuperRole() {
        return userRepository.findByUsername(getUsername())
                .map(user -> user.getRole().isSuperRole())
                .orElse(false);
    }
}
