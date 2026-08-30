package com.tns.mes.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JwtService {
    @Value("${mes.security.jwt-secret}")
    private String secret;
    @Value("${mes.security.token-validity-seconds:28800}")
    private long validitySeconds;
    private Key signingKey;

    @PostConstruct
    public void initialize() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            bytes = padded;
        }
        signingKey = Keys.hmacShaKeyFor(bytes);
    }

    public String issue(UserDetails user) {
        Instant now = Instant.now();
        List<String> authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toList());
        return Jwts.builder().setSubject(user.getUsername()).claim("authorities", authorities)
                .setIssuedAt(Date.from(now)).setExpiration(Date.from(now.plusSeconds(validitySeconds)))
                .signWith(signingKey, SignatureAlgorithm.HS256).compact();
    }

    public String subject(String token) { return claims(token).getSubject(); }

    @SuppressWarnings("unchecked")
    public List<String> authorities(String token) {
        Object value = claims(token).get("authorities");
        return value instanceof List ? (List<String>) value : java.util.Collections.emptyList();
    }

    public boolean valid(String token) {
        try {
            return claims(token).getExpiration().after(new Date());
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private Claims claims(String token) {
        return Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token).getBody();
    }
}

