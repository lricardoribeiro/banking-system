package com.banking.system.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Criação e validação de tokens JWT.
 *
 * Considerações de segurança:
 * - Usa HS256 (HMAC-SHA256) com chave secreta de 256 bits.
 *   Para produção, prefira RS256 (RSA) para que a chave pública possa ser
 *   compartilhada com outros serviços sem expor o segredo de assinatura.
 * - Expiração do token: tokens de acesso de curta duração (15-60 min) + tokens de refresh.
 * - Nunca armazene dados sensíveis (senhas, PAN) em claims JWT.
 * - Valide assinatura, expiração e emissor em TODA requisição.
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${banking.security.jwt.secret}")
    private String jwtSecret;

    @Value("${banking.security.jwt.expiration-ms:3600000}")
    private long jwtExpirationMs;

    @Value("${banking.security.jwt.issuer:banking-system}")
    private String issuer;

    /** Gera um token JWT para o usuário autenticado. */
    public String generateToken(Authentication authentication) {
        String subject = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(subject)
                .issuer(issuer)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /** Extrai o nome de usuário do token. */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /** Extrai as roles do token. */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        return (List<String>) parseClaims(token).get("roles");
    }

    /** Valida o token JWT. Retorna false se expirado, malformado ou com assinatura inválida. */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token JWT expirado: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Token JWT malformado: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
