package chemos.chem_os.auth.dto;

public record EnrollInitResponse(
        String secretBase32,
        String otpauthUri
) {
}
