package com.zoomtranscriber.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Authentication service for user management.
 * Handles user registration, authentication, and password management.
 */
@Service
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    // In-memory user storage (replace with database in production)
    private final Map<String, UserInfo> users = new ConcurrentHashMap<>();
    private final Map<String, String> passwordResetTokens = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> passwordResetTokenExpiry = new ConcurrentHashMap<>();

    public AuthenticationService(PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;

        // Initialize with default admin user for development
        initializeDefaultUsers();
    }

    /**
     * Checks if a user exists.
     *
     * @param username username to check
     * @return true if user exists
     */
    public boolean userExists(String username) {
        return users.containsKey(username);
    }

    /**
     * Checks if an email exists.
     *
     * @param email email to check
     * @return true if email exists
     */
    public boolean emailExists(String email) {
        return users.values().stream()
                .anyMatch(user -> user.getEmail().equalsIgnoreCase(email));
    }

    /**
     * Registers a new user.
     *
     * @param username username
     * @param email email
     * @param password password
     * @param firstName first name
     * @param lastName last name
     */
    public void registerUser(String username, String email, String password, String firstName, String lastName) {
        if (userExists(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (emailExists(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        String encodedPassword = passwordEncoder.encode(password);
        UserInfo userInfo = new UserInfo(
                username,
                email,
                encodedPassword,
                firstName,
                lastName,
                Set.of("ROLE_USER"),
                LocalDateTime.now(),
                null
        );

        users.put(username, userInfo);
        logger.info("User registered successfully: {}", username);
    }

    /**
     * Gets user details for authentication.
     *
     * @param username username
     * @return user details
     * @throws UsernameNotFoundException if user not found
     */
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserInfo userInfo = users.get(username);
        if (userInfo == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        // Convert Set<String> roles to Collection<GrantedAuthority>
        Collection<GrantedAuthority> authorities = userInfo.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return User.builder()
                .username(userInfo.getUsername())
                .password(userInfo.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    /**
     * Gets user information.
     *
     * @param username username
     * @return user information
     */
    public UserInfo getUserInfo(String username) {
        UserInfo userInfo = users.get(username);
        if (userInfo == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return userInfo;
    }

    /**
     * Changes user password.
     *
     * @param username username
     * @param currentPassword current password
     * @param newPassword new password
     * @return true if password changed successfully
     */
    public boolean changePassword(String username, String currentPassword, String newPassword) {
        UserInfo userInfo = users.get(username);
        if (userInfo == null) {
            return false;
        }

        if (!passwordEncoder.matches(currentPassword, userInfo.getPassword())) {
            return false;
        }

        String encodedNewPassword = passwordEncoder.encode(newPassword);
        userInfo.setPassword(encodedNewPassword);
        users.put(username, userInfo);

        logger.info("Password changed successfully for user: {}", username);
        return true;
    }

    /**
     * Generates password reset token.
     *
     * @param email email address
     * @return reset token or null if email not found
     */
    public String generatePasswordResetToken(String email) {
        Optional<UserInfo> userOpt = users.values().stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(email))
                .findFirst();

        if (userOpt.isEmpty()) {
            return null;
        }

        String resetToken = tokenProvider.generatePasswordResetToken(userOpt.get().getUsername());
        passwordResetTokens.put(resetToken, email);
        passwordResetTokenExpiry.put(resetToken, LocalDateTime.now().plusHours(24));

        return resetToken;
    }

    /**
     * Resets password using token.
     *
     * @param token password reset token
     * @param newPassword new password
     * @return true if password reset successfully
     */
    public boolean resetPassword(String token, String newPassword) {
        String email = passwordResetTokens.get(token);
        LocalDateTime expiry = passwordResetTokenExpiry.get(token);

        if (email == null || expiry == null || expiry.isBefore(LocalDateTime.now())) {
            return false;
        }

        // Find user by email
        Optional<UserInfo> userOpt = users.values().stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(email))
                .findFirst();

        if (userOpt.isEmpty()) {
            return false;
        }

        String encodedNewPassword = passwordEncoder.encode(newPassword);
        UserInfo userInfo = userOpt.get();
        userInfo.setPassword(encodedNewPassword);
        users.put(userInfo.getUsername(), userInfo);

        // Clean up reset token
        passwordResetTokens.remove(token);
        passwordResetTokenExpiry.remove(token);

        logger.info("Password reset successful for user: {}", userInfo.getUsername());
        return true;
    }

    /**
     * Updates user's last login time.
     *
     * @param username username
     */
    public void updateLastLogin(String username) {
        UserInfo userInfo = users.get(username);
        if (userInfo != null) {
            userInfo.setLastLogin(LocalDateTime.now());
            users.put(username, userInfo);
        }
    }

    /**
     * Gets all users (for admin purposes).
     *
     * @return list of all users
     */
    public List<UserInfo> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    /**
     * Deletes a user.
     *
     * @param username username to delete
     * @return true if user deleted
     */
    public boolean deleteUser(String username) {
        UserInfo removed = users.remove(username);
        if (removed != null) {
            logger.info("User deleted: {}", username);
            return true;
        }
        return false;
    }

    /**
     * Updates user information.
     *
     * @param username username
     * @param email new email
     * @param firstName new first name
     * @param lastName new last name
     * @return true if user updated
     */
    public boolean updateUser(String username, String email, String firstName, String lastName) {
        UserInfo userInfo = users.get(username);
        if (userInfo == null) {
            return false;
        }

        // Check if email is being changed to one that already exists
        if (!userInfo.getEmail().equalsIgnoreCase(email) && emailExists(email)) {
            return false;
        }

        userInfo.setEmail(email);
        userInfo.setFirstName(firstName);
        userInfo.setLastName(lastName);
        users.put(username, userInfo);

        logger.info("User updated: {}", username);
        return true;
    }

    /**
     * Gets user roles.
     *
     * @param username username
     * @return set of user roles
     */
    public Set<String> getUserRoles(String username) {
        UserInfo userInfo = users.get(username);
        return userInfo != null ? userInfo.getRoles() : Set.of();
    }

    /**
     * Checks if user has specific role.
     *
     * @param username username
     * @param role role to check
     * @return true if user has role
     */
    public boolean userHasRole(String username, String role) {
        Set<String> roles = getUserRoles(username);
        return roles.contains(role);
    }

    /**
     * Updates user roles.
     *
     * @param username username
     * @param roles new set of roles
     * @return true if roles updated
     */
    public boolean updateUserRoles(String username, Set<String> roles) {
        UserInfo userInfo = users.get(username);
        if (userInfo == null) {
            return false;
        }

        userInfo.setRoles(roles);
        users.put(username, userInfo);

        logger.info("User roles updated for: {} -> {}", username, roles);
        return true;
    }

    /**
     * Initializes default users for development.
     */
    private void initializeDefaultUsers() {
        // Default admin user
        String adminPassword = passwordEncoder.encode("admin123");
        UserInfo admin = new UserInfo(
                "admin",
                "admin@zoomtranscriber.com",
                adminPassword,
                "Admin",
                "User",
                Set.of("ROLE_USER", "ROLE_ADMIN"),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        users.put("admin", admin);

        // Default regular user
        String userPassword = passwordEncoder.encode("user123");
        UserInfo regularUser = new UserInfo(
                "user",
                "user@zoomtranscriber.com",
                userPassword,
                "Regular",
                "User",
                Set.of("ROLE_USER"),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        users.put("user", regularUser);

        logger.info("Default users initialized for development");
    }

    /**
     * User information data class.
     */
    public static class UserInfo {
        private String username;
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        private Set<String> roles;
        private LocalDateTime createdAt;
        private LocalDateTime lastLogin;

        public UserInfo(String username, String email, String password, String firstName, String lastName,
                      Set<String> roles, LocalDateTime createdAt, LocalDateTime lastLogin) {
            this.username = username;
            this.email = email;
            this.password = password;
            this.firstName = firstName;
            this.lastName = lastName;
            this.roles = new HashSet<>(roles);
            this.createdAt = createdAt;
            this.lastLogin = lastLogin;
        }

        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public Set<String> getRoles() { return new HashSet<>(roles); }
        public void setRoles(Set<String> roles) { this.roles = new HashSet<>(roles); }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getLastLogin() { return lastLogin; }
        public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

        public String getFullName() {
            return firstName + " " + lastName;
        }
    }
}