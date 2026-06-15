package chemos.chem_os.auth.dto;

public record LoginResponse(
        String token,
        String username
) {
}
