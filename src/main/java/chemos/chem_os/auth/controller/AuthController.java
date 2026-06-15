package chemos.chem_os.auth.controller;

import chemos.chem_os.auth.service.AuthService;
import chemos.chem_os.auth.dto.LoginRequest;
import chemos.chem_os.auth.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request){
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/test")
    public String test() {
        return passwordEncoder.encode("admin123");
    }
}
