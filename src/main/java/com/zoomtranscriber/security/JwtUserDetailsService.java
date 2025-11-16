package com.zoomtranscriber.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * JWT user details service implementation.
 * Loads user-specific data for authentication.
 */
@Service
public class JwtUserDetailsService implements UserDetailsService {

    private final AuthenticationService authenticationService;

    public JwtUserDetailsService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Loads user details by username.
     *
     * @param username username to load
     * @return user details
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return authenticationService.loadUserByUsername(username);
    }
}