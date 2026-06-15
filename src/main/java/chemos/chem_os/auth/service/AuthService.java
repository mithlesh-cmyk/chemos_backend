package chemos.chem_os.auth.service;

import chemos.chem_os.auth.dto.LoginRequest;
import chemos.chem_os.auth.dto.LoginResponse;
import chemos.chem_os.auth.model.User;
import chemos.chem_os.auth.repository.UserRepository;
import chemos.chem_os.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));


        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }



        String mockToken = jwtService.generateToken(user.getUsername());

        return new LoginResponse(mockToken, user.getUsername());
    }
}
