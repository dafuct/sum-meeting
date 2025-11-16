package com.zoomtranscriber.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT token provider for authentication and authorization.
 * Handles token generation, validation, and management.
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwt.secret:zoomTranscriberSecretKeyThatShouldBeChangedInProduction}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-in-ms:3600000}") // 1 hour default
    private long jwtExpirationInMs;

    @Value("${app.jwt.refresh-expiration-in-ms:604800000}") // 7 days default
    private long refreshExpirationInMs;

    private Key key;
    private Set<String> invalidatedTokens = ConcurrentHashMap.newKeySet();

    /**
     * Initializes the JWT signing key.
     */
    public JwtTokenProvider() {
        // Key will be initialized in @PostConstruct after properties are set
    }

    /**
     * Initializes the signing key.
     */
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Generates a JWT access token.
     *
     * @param authentication Spring Security authentication object
     * @return JWT token string
     */
    public String generateToken(Authentication authentication) {
        if (key == null) {
            init();
        }

        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        Instant now = Instant.now();
        Instant expiryDate = now.plus(jwtExpirationInMs, ChronoUnit.MILLIS);

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiryDate))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Generates a JWT refresh token.
     *
     * @param authentication Spring Security authentication object
     * @return refresh token string
     */
    public String generateRefreshToken(Authentication authentication) {
        if (key == null) {
            init();
        }

        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        Instant now = Instant.now();
        Instant expiryDate = now.plus(refreshExpirationInMs, ChronoUnit.MILLIS);

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiryDate))
                .claim("type", "refresh")
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Gets username from JWT token.
     *
     * @param token JWT token string
     * @return username
     */
    public String getUsernameFromToken(String token) {
        if (key == null) {
            init();
        }

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    /**
     * Gets expiration date from JWT token.
     *
     * @param token JWT token string
     * @return expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        if (key == null) {
            init();
        }

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getExpiration();
    }

    /**
     * Validates JWT token.
     *
     * @param token JWT token string
     * @return true if token is valid
     */
    public boolean validateToken(String token) {
        try {
            if (key == null) {
                init();
            }

            // Check if token is invalidated
            if (invalidatedTokens.contains(token)) {
                logger.warn("Token is invalidated: {}", token.substring(0, Math.min(10, token.length())));
                return false;
            }

            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);

            return true;
        } catch (SecurityException ex) {
            logger.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty: {}", ex.getMessage());
        }

        return false;
    }

    /**
     * Gets token expiration in milliseconds.
     *
     * @return expiration time in milliseconds
     */
    public long getTokenExpirationInMs() {
        return jwtExpirationInMs;
    }

    /**
     * Gets refresh token expiration in milliseconds.
     *
     * @return refresh token expiration time in milliseconds
     */
    public long getRefreshTokenExpirationInMs() {
        return refreshExpirationInMs;
    }

    /**
     * Invalidates a token (adds to blacklist).
     *
     * @param token JWT token to invalidate
     */
    public void invalidateToken(String token) {
        if (token != null) {
            invalidatedTokens.add(token);
            logger.info("Token invalidated: {}", token.substring(0, Math.min(10, token.length())));
        }
    }

    /**
     * Checks if a token is invalidated.
     *
     * @param token JWT token to check
     * @return true if token is invalidated
     */
    public boolean isTokenInvalidated(String token) {
        return invalidatedTokens.contains(token);
    }

    /**
     * Removes expired tokens from the invalidation list.
     */
    public void cleanupExpiredTokens() {
        if (key == null) {
            init();
        }

        invalidatedTokens.removeIf(token -> {
            try {
                Date expiration = getExpirationDateFromToken(token);
                return expiration.before(new Date());
            } catch (Exception e) {
                logger.debug("Error checking token expiration during cleanup", e);
                return true; // Remove tokens that cause errors
            }
        });
    }

    /**
     * Generates a temporary token for password reset.
     *
     * @param username username for password reset
     * @return password reset token
     */
    public String generatePasswordResetToken(String username) {
        if (key == null) {
            init();
        }

        Instant now = Instant.now();
        Instant expiryDate = now.plus(24, ChronoUnit.HOURS); // 24 hours for password reset

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiryDate))
                .claim("type", "password-reset")
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Validates password reset token.
     *
     * @param token password reset token
     * @return username if valid, null otherwise
     */
    public String validatePasswordResetToken(String token) {
        try {
            if (key == null) {
                init();
            }

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String tokenType = claims.get("type", String.class);
            if (!"password-reset".equals(tokenType)) {
                return null;
            }

            return claims.getSubject();
        } catch (Exception e) {
            logger.error("Invalid password reset token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Gets all claims from a token.
     *
     * @param token JWT token
     * @return claims
     */
    public Claims getClaimsFromToken(String token) {
        if (key == null) {
            init();
        }

        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Checks if token is a refresh token.
     *
     * @param token JWT token
     * @return true if refresh token
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if token is a password reset token.
     *
     * @param token JWT token
     * @return true if password reset token
     */
    public boolean isPasswordResetToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return "password-reset".equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets remaining time until token expiration.
     *
     * @param token JWT token
     * @return remaining time in milliseconds, or 0 if expired
     */
    public long getRemainingTimeInMs(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return Math.max(0, expiration.getTime() - System.currentTimeMillis());
        } catch (Exception e) {
            return 0;
        }
    }
}