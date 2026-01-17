package tech.grastone.fz.matching.util;

import java.security.Key;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {
	@Value("${token.secret.key}")
    private String SECRET_KEY;
	
	@Value("${token.access.expiration}")
    private int ACCESS_TOKEN_EXPIRATION; // 15 min
	
	@Value("${token.refresh.expiration}")
    private int REFRESH_TOKEN_EXPIRATION; // 

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username, boolean isRefreshToken) {
        long expirationTime = isRefreshToken ? REFRESH_TOKEN_EXPIRATION : ACCESS_TOKEN_EXPIRATION;
        
        LocalDateTime nowTime = LocalDateTime.now();
        
        
        
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(Date.from(nowTime.atZone(ZoneId.systemDefault()).toInstant()))
                .setExpiration(Date.from(nowTime.plusMinutes(expirationTime).atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}