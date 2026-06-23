package chemos.chem_os.auth.security;

import chemos.chem_os.auth.model.User;
import chemos.chem_os.auth.repository.UserRepository;
import chemos.chem_os.auth.service.PermissionResolverService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PermissionResolverService permissionResolverService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtService.isValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = jwtService.extractUsername(token);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            User user = userRepository.findByUsernameWithPermissions(username).orElse(null);

            if (user == null || !user.getIsActive()) {
                filterChain.doFilter(request, response);
                return;
            }

            // Resolve effective permission codes from DB (role hierarchy + user restrictions).
            // Super roles receive all permission codes, ensuring every admin action
            // maps to a real permission code for audit log consistency.
            Set<String> permissionCodes = permissionResolverService.resolve(user);

            List<GrantedAuthority> authorities = permissionCodes.stream()
                    .map(SimpleGrantedAuthority::new)
                    .map(a -> (GrantedAuthority) a)
                    .toList();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
