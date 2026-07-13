package com.promptstudio.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

/**
 * Utility component responsible for all JSON Web Token operations:
 * generation, parsing, validation, and claim extraction. Consolidates
 * what would otherwise be a separate token provider, entry point
 * constants holder, and validator into a single cohesive class, since
 * all three concerns are tightly coupled around the same secret key
 * and token structure.
 */
@Component
public class JwtUtil {

    /** HMAC-SHA secret key used to sign and verify tokens, loaded from application config. */
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    /** Access token validity duration in milliseconds. */
    @Value("${app.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    /** Token issuer name, embedded as a claim for traceability. */
    @Value("${app.jwt.issuer}")
    private String issuer;

    /** Claim key used to store the authenticated user's ID within the token. */
    public static final String CLAIM_USER_ID = "userId";

    /** Claim key used to store the authenticated user's role within the token. */
    public static final String CLAIM_ROLE = "role";

    /** HTTP header name the JWT is expected to arrive in. */
    public static final String AUTH_HEADER = "Authorization";

    /** Prefix expected before the raw token in the Authorization header. */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * Builds the signing key from the configured secret string.
     * The secret must be long enough (256+ bits) for HS256 signing.
     *
     * @return the SecretKey used for signing/verifying tokens
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a signed JWT access token for an authenticated user.
     *
     * @param email  the user's email, used as the token subject
     * @param userId the user's unique ID, embedded as a custom claim
     * @param role   the user's role name (e.g., "USER", "ADMIN"), embedded as a custom claim
     * @return the signed, compact JWT string
     */
    public String generateToken(String email, String userId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE, role)
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the email (subject) from a valid token.
     *
     * @param token the JWT string
     * @return the email embedded in the token
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the user ID custom claim from a valid token.
     *
     * @param token the JWT string
     * @return the user ID embedded in the token
     */
    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_USER_ID, String.class));
    }

    /**
     * Extracts the role custom claim from a valid token.
     *
     * @param token the JWT string
     * @return the role name embedded in the token
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_ROLE, String.class));
    }

    /**
     * Extracts the expiration date from a valid token.
     *
     * @param token the JWT string
     * @return the expiration Date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic claim extraction helper. Parses and verifies the token's
     * signature, then applies the given resolver function to its claims.
     *
     * @param token          the JWT string
     * @param claimsResolver function to pull a specific value out of the claims
     * @param <T>            the type of value being extracted
     * @return the resolved claim value
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    /**
     * Validates a token's signature and expiration, and confirms it
     * belongs to the given email. Catches all JJWT exceptions internally
     * and returns false rather than propagating them, so callers (the
     * authentication filter) can handle invalid tokens uniformly.
     *
     * @param token the JWT string to validate
     * @param email the expected subject email to match against
     * @return true if the token is valid, unexpired, and matches the email
     */
    public boolean validateToken(String token, String email) {
        try {
            String extractedEmail = extractEmail(token);
            return extractedEmail.equals(email) && !isTokenExpired(token);
        } catch (ExpiredJwtException | MalformedJwtException | SignatureException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Checks whether a token's expiration timestamp has already passed.
     *
     * @param token the JWT string
     * @return true if the token is expired
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Strips the "Bearer " prefix from a raw Authorization header value.
     *
     * @param authHeader the full Authorization header value
     * @return the raw token string, or null if the header is missing/malformed
     */
    public String resolveToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith(TOKEN_PREFIX)) {
            return authHeader.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}