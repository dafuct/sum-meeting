package com.zoomtranscriber.api;

import com.zoomtranscriber.security.AuthenticationService;
import com.zoomtranscriber.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API controller for authentication operations.
 * Provides endpoints for login, logout, registration, and token management.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationService authenticationService;

    public AuthenticationController(
            AuthenticationManager authenticationManager,
            JwtTokenProvider tokenProvider,
            AuthenticationService authenticationService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.authenticationService = authenticationService;
    }

    /**
     * Authenticates a user and returns JWT token.
     *
     * @param loginRequest login credentials
     * @return authentication response with JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("Authentication attempt for user: {}", loginRequest.username());

        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.username(),
                    loginRequest.password()
                )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String token = tokenProvider.generateToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(authentication);

            logger.info("User authenticated successfully: {}", loginRequest.username());

            return ResponseEntity.ok(new LoginResponse(
                token,
                refreshToken,
                "Bearer",
                tokenProvider.getTokenExpirationInMs(),
                authentication.getName(),
                authentication.getAuthorities().stream()
                    .map(Object::toString)
                    .toList()
            ));

        } catch (BadCredentialsException e) {
            logger.warn("Authentication failed for user: {}", loginRequest.username());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new LoginResponse(null, null, null, 0L, null, List.of()));
        }
    }

    /**
     * Refreshes an access token using a refresh token.
     *
     * @param request refresh token request
     * @return new access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        logger.info("Token refresh request received");

        try {
            if (!tokenProvider.validateToken(request.refreshToken())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RefreshTokenResponse(null, 0L, "Invalid refresh token"));
            }

            String username = tokenProvider.getUsernameFromToken(request.refreshToken());
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, "")
            );

            String newToken = tokenProvider.generateToken(authentication);

            return ResponseEntity.ok(new RefreshTokenResponse(
                newToken,
                tokenProvider.getTokenExpirationInMs(),
                "Token refreshed successfully"
            ));

        } catch (Exception e) {
            logger.error("Token refresh failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new RefreshTokenResponse(null, 0L, "Token refresh failed: " + e.getMessage()));
        }
    }

    /**
     * Logs out a user by invalidating the token.
     *
     * @param request logout request
     * @return logout response
     */
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@RequestBody LogoutRequest request) {
        logger.info("Logout request received");

        try {
            String token = extractTokenFromRequest(request.token());
            if (token != null) {
                tokenProvider.invalidateToken(token);
            }

            return ResponseEntity.ok(new LogoutResponse(true, "Logged out successfully"));

        } catch (Exception e) {
            logger.error("Logout failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new LogoutResponse(false, "Logout failed: " + e.getMessage()));
        }
    }

    /**
     * Registers a new user.
     *
     * @param registrationRequest user registration data
     * @return registration response
     */
    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest registrationRequest) {
        logger.info("Registration attempt for user: {}", registrationRequest.username());

        try {
            // Check if user already exists
            if (authenticationService.userExists(registrationRequest.username())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RegistrationResponse(false, "Username already exists"));
            }

            if (authenticationService.emailExists(registrationRequest.email())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RegistrationResponse(false, "Email already exists"));
            }

            // Create new user
            authenticationService.registerUser(
                registrationRequest.username(),
                registrationRequest.email(),
                registrationRequest.password(),
                registrationRequest.firstName(),
                registrationRequest.lastName()
            );

            logger.info("User registered successfully: {}", registrationRequest.username());

            return ResponseEntity.ok(new RegistrationResponse(
                true,
                "User registered successfully"
            ));

        } catch (Exception e) {
            logger.error("Registration failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new RegistrationResponse(false, "Registration failed: " + e.getMessage()));
        }
    }

    /**
     * Validates a JWT token.
     *
     * @param request token validation request
     * @return validation response
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidateTokenResponse> validateToken(@RequestBody ValidateTokenRequest request) {
        logger.info("Token validation request received");

        try {
            String token = extractTokenFromRequest(request.token());
            boolean isValid = tokenProvider.validateToken(token);

            if (isValid) {
                String username = tokenProvider.getUsernameFromToken(token);
                return ResponseEntity.ok(new ValidateTokenResponse(
                    true,
                    username,
                    "Token is valid"
                ));
            } else {
                return ResponseEntity.ok(new ValidateTokenResponse(
                    false,
                    null,
                    "Token is invalid"
                ));
            }

        } catch (Exception e) {
            logger.error("Token validation failed", e);
            return ResponseEntity.ok(new ValidateTokenResponse(
                false,
                null,
                "Token validation failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Gets current user information.
     *
     * @return user information
     */
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String username = authentication.getName();
            var userInfo = authenticationService.getUserInfo(username);

            return ResponseEntity.ok(new UserInfoResponse(
                userInfo.getUsername(),
                userInfo.getEmail(),
                userInfo.getFirstName(),
                userInfo.getLastName(),
                userInfo.getRoles().stream().toList(),
                userInfo.getLastLogin(),
                userInfo.getCreatedAt()
            ));

        } catch (Exception e) {
            logger.error("Failed to get current user info", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Changes user password.
     *
     * @param request password change request
     * @return response indicating success or failure
     */
    @PostMapping("/change-password")
    public ResponseEntity<ChangePasswordResponse> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            boolean success = authenticationService.changePassword(
                username,
                request.currentPassword(),
                request.newPassword()
            );

            if (success) {
                logger.info("Password changed successfully for user: {}", username);
                return ResponseEntity.ok(new ChangePasswordResponse(
                    true,
                    "Password changed successfully"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ChangePasswordResponse(
                        false,
                        "Current password is incorrect"
                    ));
            }

        } catch (Exception e) {
            logger.error("Password change failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ChangePasswordResponse(
                    false,
                    "Password change failed: " + e.getMessage()
                ));
        }
    }

    /**
     * Requests password reset.
     *
     * @param request password reset request
     * @return response indicating success or failure
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            String resetToken = authenticationService.generatePasswordResetToken(request.email());
            
            if (resetToken != null) {
                logger.info("Password reset token generated for email: {}", request.email());
                
                // In a real implementation, send email with reset token
                // For now, just return the token (for development)
                return ResponseEntity.ok(new ForgotPasswordResponse(
                    true,
                    "Password reset token sent to email",
                    resetToken // Remove this in production
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ForgotPasswordResponse(
                        false,
                        "Email not found",
                        null
                    ));
            }

        } catch (Exception e) {
            logger.error("Password reset request failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ForgotPasswordResponse(
                    false,
                    "Password reset request failed: " + e.getMessage(),
                    null
                ));
        }
    }

    /**
     * Resets password using reset token.
     *
     * @param request password reset confirmation request
     * @return response indicating success or failure
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            boolean success = authenticationService.resetPassword(
                request.token(),
                request.newPassword()
            );

            if (success) {
                logger.info("Password reset successful using token");
                return ResponseEntity.ok(new ResetPasswordResponse(
                    true,
                    "Password reset successful"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResetPasswordResponse(
                        false,
                        "Invalid or expired reset token"
                    ));
            }

        } catch (Exception e) {
            logger.error("Password reset failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ResetPasswordResponse(
                    false,
                    "Password reset failed: " + e.getMessage()
                ));
        }
    }

    /**
     * Gets authentication status.
     *
     * @return authentication status
     */
    @GetMapping("/status")
    public ResponseEntity<AuthStatusResponse> getAuthStatus() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            boolean isAuthenticated = authentication != null && authentication.isAuthenticated();

            return ResponseEntity.ok(new AuthStatusResponse(
                isAuthenticated,
                isAuthenticated ? authentication.getName() : null,
                isAuthenticated ? authentication.getAuthorities().stream()
                    .map(Object::toString)
                    .toList() : List.of()
            ));

        } catch (Exception e) {
            logger.error("Failed to get auth status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Extracts token from request.
     */
    private String extractTokenFromRequest(String tokenHeader) {
        if (tokenHeader != null && tokenHeader.startsWith("Bearer ")) {
            return tokenHeader.substring(7);
        }
        return tokenHeader;
    }

    // Request/Response DTOs

    public record LoginRequest(
            String username,
            String password,
            Boolean rememberMe
    ) {}

    public record LoginResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            Long expiresIn,
            String username,
            java.util.List<String> roles
    ) {}

    public record RefreshTokenRequest(
            String refreshToken
    ) {}

    public record RefreshTokenResponse(
            String accessToken,
            Long expiresIn,
            String message
    ) {}

    public record LogoutRequest(
            String token
    ) {}

    public record LogoutResponse(
            boolean success,
            String message
    ) {}

    public record RegistrationRequest(
            String username,
            String email,
            String password,
            String firstName,
            String lastName
    ) {}

    public record RegistrationResponse(
            boolean success,
            String message
    ) {}

    public record ValidateTokenRequest(
            String token
    ) {}

    public record ValidateTokenResponse(
            boolean valid,
            String username,
            String message
    ) {}

    public record UserInfoResponse(
            String username,
            String email,
            String firstName,
            String lastName,
            java.util.List<String> roles,
            java.time.LocalDateTime lastLogin,
            java.time.LocalDateTime createdAt
    ) {}

    public record ChangePasswordRequest(
            String currentPassword,
            String newPassword
    ) {}

    public record ChangePasswordResponse(
            boolean success,
            String message
    ) {}

    public record ForgotPasswordRequest(
            String email
    ) {}

    public record ForgotPasswordResponse(
            boolean success,
            String message,
            String resetToken
    ) {}

    public record ResetPasswordRequest(
            String token,
            String newPassword
    ) {}

    public record ResetPasswordResponse(
            boolean success,
            String message
    ) {}

    public record AuthStatusResponse(
            boolean authenticated,
            String username,
            java.util.List<String> authorities
    ) {}
}