package com.zoomtranscriber.core.exceptions;

import java.net.SocketTimeoutException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.time.LocalDateTime;

/**
 * Exception thrown when network-related errors occur in the Zoom Transcriber application.
 * This includes connection failures, timeouts, and DNS resolution issues.
 */
public class NetworkException extends RuntimeException {
    
    private final String errorCode;
    private final String host;
    private final int port;
    private final LocalDateTime timestamp;
    private final long timeoutMs;
    
    /**
     * Constructs a new NetworkException with the specified detail message.
     * 
     * @param message the detail message explaining the exception
     */
    public NetworkException(String message) {
        super(message);
        this.errorCode = "NETWORK_001";
        this.host = null;
        this.port = 0;
        this.timestamp = LocalDateTime.now();
        this.timeoutMs = 0;
    }
    
    /**
     * Constructs a new NetworkException with the specified detail message and cause.
     * 
     * @param message the detail message explaining the exception
     * @param cause the cause of this exception
     */
    public NetworkException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "NETWORK_001";
        this.host = null;
        this.port = 0;
        this.timestamp = LocalDateTime.now();
        this.timeoutMs = 0;
    }
    
    /**
     * Constructs a new NetworkException with the specified detail message and connection details.
     * 
     * @param message the detail message explaining the exception
     * @param host the host name or IP address
     * @param port the port number
     * @param timeoutMs the timeout duration in milliseconds
     */
    public NetworkException(String message, String host, int port, long timeoutMs) {
        super(message);
        this.errorCode = "NETWORK_001";
        this.host = host;
        this.port = port;
        this.timestamp = LocalDateTime.now();
        this.timeoutMs = timeoutMs;
    }
    
    /**
     * Constructs a new NetworkException with full context information.
     * 
     * @param message the detail message explaining the exception
     * @param cause the cause of this exception
     * @param host the host name or IP address
     * @param port the port number
     * @param timeoutMs the timeout duration in milliseconds
     * @param errorCode the specific error code for this exception
     */
    public NetworkException(String message, Throwable cause, String host, int port, long timeoutMs, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
        this.host = host;
        this.port = port;
        this.timestamp = LocalDateTime.now();
        this.timeoutMs = timeoutMs;
    }
    
    /**
     * Gets the error code associated with this exception.
     * 
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Gets the host name or IP address associated with this exception.
     * 
     * @return the host or null if not applicable
     */
    public String getHost() {
        return host;
    }
    
    /**
     * Gets the port number associated with this exception.
     * 
     * @return the port number or 0 if not applicable
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Gets the timestamp when this exception occurred.
     * 
     * @return the timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * Gets the timeout duration associated with this exception.
     * 
     * @return the timeout duration in milliseconds
     */
    public long getTimeoutMs() {
        return timeoutMs;
    }
    
    /**
     * Creates a NetworkException for connection timeout errors.
     * 
     * @param host the host that timed out
     * @param port the port that timed out
     * @param timeoutMs the timeout duration
     * @return new NetworkException instance
     */
    public static NetworkException connectionTimeout(String host, int port, long timeoutMs) {
        return new NetworkException(
            "Connection timeout to " + host + ":" + port + " after " + timeoutMs + "ms",
            host,
            port,
            timeoutMs
        );
    }
    
    /**
     * Creates a NetworkException for connection refused errors.
     * 
     * @param host the host that refused the connection
     * @param port the port that refused the connection
     * @return new NetworkException instance
     */
    public static NetworkException connectionRefused(String host, int port) {
        return new NetworkException(
            "Connection refused to " + host + ":" + port,
            null,
            host,
            port,
            0,
            "NETWORK_002"
        );
    }
    
    /**
     * Creates a NetworkException for DNS resolution failures.
     * 
     * @param hostname the hostname that could not be resolved
     * @return new NetworkException instance
     */
    public static NetworkException dnsResolutionFailed(String hostname) {
        return new NetworkException(
            "DNS resolution failed for hostname: " + hostname,
            null,
            hostname,
            0,
            0,
            "NETWORK_003"
        );
    }
    
    /**
     * Creates a NetworkException from a generic network cause.
     * 
     * @param cause the underlying network exception
     * @param host the host that was being accessed
     * @param port the port that was being accessed
     * @return new NetworkException instance
     */
    public static NetworkException fromNetworkCause(Throwable cause, String host, int port) {
        if (cause instanceof SocketTimeoutException) {
            return new NetworkException(
                "Socket timeout: " + cause.getMessage(),
                cause,
                host,
                port,
                0,
                "NETWORK_004"
            );
        } else if (cause instanceof ConnectException) {
            return new NetworkException(
                "Connection failed: " + cause.getMessage(),
                cause,
                host,
                port,
                0,
                "NETWORK_005"
            );
        } else if (cause instanceof UnknownHostException) {
            return new NetworkException(
                "Unknown host: " + cause.getMessage(),
                cause,
                host,
                port,
                0,
                "NETWORK_006"
            );
        } else {
            return new NetworkException(
                "Network error: " + cause.getMessage(),
                cause,
                host,
                port,
                0,
                "NETWORK_007"
            );
        }
    }
    
    /**
     * Creates a NetworkException for SSL/TLS handshake failures.
     * 
     * @param host the host with SSL issues
     * @param port the port with SSL issues
     * @param cause the underlying SSL exception
     * @return new NetworkException instance
     */
    public static NetworkException sslHandshakeFailed(String host, int port, Throwable cause) {
        return new NetworkException(
            "SSL/TLS handshake failed with " + host + ":" + port + ": " + cause.getMessage(),
            cause,
            host,
            port,
            0,
            "NETWORK_008"
        );
    }
    
    /**
     * Creates a NetworkException for network unreachable errors.
     * 
     * @param host the host that is unreachable
     * @param cause the underlying exception
     * @return new NetworkException instance
     */
    public static NetworkException networkUnreachable(String host, Throwable cause) {
        return new NetworkException(
            "Network unreachable to host: " + host,
            cause,
            host,
            0,
            0,
            "NETWORK_009"
        );
    }
}