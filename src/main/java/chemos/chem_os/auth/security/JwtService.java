package chemos.chem_os.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {

    public static final String STAGE_FULL = "FULL";
    public static final String STAGE_PRE_AUTH = "PRE_AUTH";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${totp.preauth.expiry-minutes}")
    private long preAuthExpiryMinutes;

    // Called after 2FA verification succeeds — full-access token, carries role and stage=FULL.
    // JwtAuthFilter only resolves authorities for FULL-stage tokens.
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)           // e.g. "ADMIN", "PURCHASE_MANAGER"
                .claim("stage", STAGE_FULL)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 24h
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    // Issued right after password validation, before 2FA is completed. Deliberately carries
    // no role/authority claims and expires quickly — it only proves "this username passed
    // password check", and JwtAuthFilter never grants it any GrantedAuthority.
    public String generatePreAuthToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .claim("stage", STAGE_PRE_AUTH)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + preAuthExpiryMinutes * 60_000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    // Extracts the role that was embedded at login time
    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public String extractStage(String token) {
        return getClaims(token).get("stage", String.class);
    }

    public boolean isFullStage(String token) {
        return STAGE_FULL.equals(extractStage(token));
    }

    public boolean isPreAuthStage(String token) {
        return STAGE_PRE_AUTH.equals(extractStage(token));
    }

    public boolean isValid(String token) {
        try {
            getClaims(token); // throws if expired or tampered
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
