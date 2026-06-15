package chemos.chem_os.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {

    private final String secret = "hellobabyhowareyoumyfriend123456";

    public String generateToken(String username){

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+86400000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS256).compact();
    }

    public String extractUsername(String token){
        return Jwts.parserBuilder()
                .setSigningKey(
                        Keys.hmacShaKeyFor(
                                secret.getBytes()
                        )
                )
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean isValid(String token){
        try {

            Jwts.parserBuilder()
                    .setSigningKey(
                            Keys.hmacShaKeyFor(secret.getBytes())

                    )
                    .build()
                    .parseClaimsJws(token);
            return true;
        }catch (Exception e){
            return false;
        }
    }

}
