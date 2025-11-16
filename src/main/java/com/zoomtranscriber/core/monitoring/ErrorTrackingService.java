package com.zoomtranscriber.core.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for tracking and analyzing errors in the Zoom Transcriber application.
 * Provides comprehensive error tracking, analysis, and alerting capabilities.
 * Supports error aggregation, trend analysis, and automatic issue detection.
 */
@Service
public class ErrorTrackingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ErrorTrackingService.class);
    
    // Error tracking storage
    private final ConcurrentHashMap<String, ErrorInfo> errorMap = new ConcurrentHashMap<>();
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong criticalErrors = new AtomicLong(0);
    private final AtomicLong warningErrors = new AtomicLong(0);
    
    // Error thresholds for alerting
    private static final int ERROR_THRESHOLD_PER_MINUTE = 10;
    private static final int CRITICAL_ERROR_THRESHOLD_PER_HOUR = 5;
    private static final double ERROR_RATE_WARNING_THRESHOLD = 0.05; // 5%
    private static final double ERROR_RATE_CRITICAL_THRESHOLD = 0.10; // 10%
    
    /**
     * Records an error occurrence for tracking.
     * 
     * @param component the component where the error occurred
     * @param errorType the type of error
     * @param errorMessage the error message
     * @param severity the error severity
     */
    public void recordError(String component, String errorType, String errorMessage, ErrorSeverity severity) {
        String errorKey = component + ":" + errorType;
        
        ErrorInfo errorInfo = errorMap.compute(errorKey, (key, existing) -> {
            if (existing == null) {
                return new ErrorInfo(component, errorType, errorMessage, severity);
            } else {
                return existing.addOccurrence();
            }
        });
        
        totalErrors.incrementAndGet();
        
        // Update severity counters
        switch (severity) {
            case CRITICAL -> criticalErrors.incrementAndGet();
            case WARNING -> warningErrors.incrementAndGet();
        }
        
        // Add MDC context for structured logging
        MDC.put("component", component);
        MDC.put("errorType", errorType);
        MDC.put("severity", severity.name());
        MDC.put("occurrenceCount", String.valueOf(errorInfo.occurrenceCount()));
        
        logger.error("Error tracked: {} - {} - {}", component, errorType, errorMessage);
        
        // Check for immediate alerting conditions
        checkImmediateAlerts(errorInfo);
        
        MDC.clear();
    }
    
    /**
     * Records an error with throwable details.
     * 
     * @param component the component where the error occurred
     * @param throwable the error exception
     * @param severity the error severity
     */
    public void recordError(String component, Throwable throwable, ErrorSeverity severity) {
        String errorType = throwable.getClass().getSimpleName();
        String errorMessage = throwable.getMessage();
        
        recordError(component, errorType, errorMessage != null ? errorMessage : throwable.toString(), severity);
    }
    
    /**
     * Gets error statistics for a specific component.
     * 
     * @param component the component name
     * @return error statistics for the component
     */
    public ErrorStatistics getComponentErrorStatistics(String component) {
        int componentErrors = 0;
        int componentCriticalErrors = 0;
        int componentWarningErrors = 0;
        
        for (ErrorInfo errorInfo : errorMap.values()) {
            if (errorInfo.component().equals(component)) {
                componentErrors += errorInfo.occurrenceCount();
                if (errorInfo.severity() == ErrorSeverity.CRITICAL) {
                    componentCriticalErrors += errorInfo.occurrenceCount();
                } else if (errorInfo.severity() == ErrorSeverity.WARNING) {
                    componentWarningErrors += errorInfo.occurrenceCount();
                }
            }
        }
        
        return new ErrorStatistics(
            component,
            componentErrors,
            componentCriticalErrors,
            componentWarningErrors,
            ErrorSeverity.determineOverallSeverity(componentCriticalErrors, componentWarningErrors)
        );
    }
    
    /**
     * Gets overall error statistics for the application.
     * 
     * @return overall error statistics
     */
    public ErrorStatistics getOverallErrorStatistics() {
        return new ErrorStatistics(
            "APPLICATION",
            totalErrors.get(),
            criticalErrors.get(),
            warningErrors.get(),
            ErrorSeverity.determineOverallSeverity(criticalErrors.get(), warningErrors.get())
        );
    }
    
    /**
     * Gets recent error trends.
     * 
     * @param duration the duration to analyze
     * @return error trend information
     */
    public ErrorTrends getErrorTrends(java.time.Duration duration) {
        Instant cutoff = Instant.now().minus(duration);
        
        int recentErrors = 0;
        int recentCriticalErrors = 0;
        
        for (ErrorInfo errorInfo : errorMap.values()) {
            if (errorInfo.lastOccurrence().isAfter(cutoff)) {
                recentErrors += errorInfo.occurrenceCount();
                if (errorInfo.severity() == ErrorSeverity.CRITICAL) {
                    recentCriticalErrors += errorInfo.occurrenceCount();
                }
            }
        }
        
        return new ErrorTrends(
            duration,
            recentErrors,
            recentCriticalErrors,
            calculateErrorRate(recentErrors, duration)
        );
    }
    
    /**
     * Gets the most frequent errors.
     * 
     * @param limit the maximum number of errors to return
     * @return list of most frequent errors
     */
    public java.util.List<ErrorInfo> getMostFrequentErrors(int limit) {
        return errorMap.values().stream()
            .sorted((e1, e2) -> Long.compare(e2.occurrenceCount(), e1.occurrenceCount()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Gets the most recent errors.
     * 
     * @param limit the maximum number of errors to return
     * @return list of most recent errors
     */
    public java.util.List<ErrorInfo> getMostRecentErrors(int limit) {
        return errorMap.values().stream()
            .sorted((e1, e2) -> e2.lastOccurrence().compareTo(e1.lastOccurrence()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Scheduled method for error analysis and alerting.
     * Runs every 5 minutes to analyze error patterns and trigger alerts.
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void analyzeErrorsAndAlert() {
        try {
            logger.debug("Starting scheduled error analysis");
            
            // Check error rate trends
            checkErrorRateTrends();
            
            // Check for error patterns
            checkErrorPatterns();
            
            // Clean up old error data
            cleanupOldErrors();
            
            logger.debug("Scheduled error analysis completed");
            
        } catch (Exception e) {
            logger.error("Error during scheduled error analysis", e);
        }
    }
    
    /**
     * Checks for immediate alerting conditions.
     * 
     * @param errorInfo the error information
     */
    private void checkImmediateAlerts(ErrorInfo errorInfo) {
        // Check for critical errors
        if (errorInfo.severity() == ErrorSeverity.CRITICAL) {
            logger.error("CRITICAL ERROR ALERT: {} - {} - {} (occurrences: {})",
                errorInfo.component(), errorInfo.errorType(), errorInfo.errorMessage(), errorInfo.occurrenceCount());
        }
        
        // Check for high frequency errors
        if (errorInfo.occurrenceCount() >= ERROR_THRESHOLD_PER_MINUTE) {
            logger.error("HIGH FREQUENCY ERROR ALERT: {} - {} has occurred {} times in the last minute",
                errorInfo.component(), errorInfo.errorType(), errorInfo.occurrenceCount());
        }
    }
    
    /**
     * Checks error rate trends for alerting.
     */
    private void checkErrorRateTrends() {
        // Check last hour error rate
        ErrorTrends lastHourTrends = getErrorTrends(java.time.Duration.ofHours(1));
        
        if (lastHourTrends.errorRate() >= ERROR_RATE_CRITICAL_THRESHOLD) {
            logger.error("CRITICAL ERROR RATE ALERT: Error rate over last hour: {}%",
                String.format("%.2f", lastHourTrends.errorRate() * 100));
        } else if (lastHourTrends.errorRate() >= ERROR_RATE_WARNING_THRESHOLD) {
            logger.warn("WARNING ERROR RATE ALERT: Error rate over last hour: {}%",
                String.format("%.2f", lastHourTrends.errorRate() * 100));
        }
    }
    
    /**
     * Checks for error patterns.
     */
    private void checkErrorPatterns() {
        // Check for same error occurring in multiple components
        java.util.Map<String, Long> errorTypes = errorMap.values().stream()
            .collect(java.util.stream.Collectors.groupingBy(
                ErrorInfo::errorType,
                java.util.stream.Collectors.counting()
            ));
        
        errorTypes.entrySet().stream()
            .filter(entry -> entry.getValue() > 2) // Error type in 3+ components
            .forEach(entry -> logger.warn("PATTERN ALERT: Error type {} occurs in {} components",
                entry.getKey(), entry.getValue()));
    }
    
    /**
     * Cleans up old error data to prevent memory leaks.
     */
    private void cleanupOldErrors() {
        Instant cutoff = Instant.now().minus(java.time.Duration.ofHours(24));
        
        errorMap.entrySet().removeIf(entry -> {
            boolean shouldRemove = entry.getValue().lastOccurrence().isBefore(cutoff);
            if (shouldRemove) {
                logger.debug("Removing old error entry: {}", entry.getKey());
            }
            return shouldRemove;
        });
    }
    
    /**
     * Calculates error rate for a time period.
     * 
     * @param errorCount number of errors
     * @param duration time period
     * @return error rate per minute
     */
    private double calculateErrorRate(int errorCount, java.time.Duration duration) {
        long minutes = duration.toMinutes();
        return minutes > 0 ? (double) errorCount / minutes : 0.0;
    }
    
    /**
     * Error information record.
     */
    public record ErrorInfo(
        String component,
        String errorType,
        String errorMessage,
        ErrorSeverity severity,
        Instant firstOccurrence,
        Instant lastOccurrence,
        long occurrenceCount
    ) {
        public ErrorInfo(String component, String errorType, String errorMessage, ErrorSeverity severity) {
            this(component, errorType, errorMessage, severity, Instant.now(), Instant.now(), 1);
        }
        
        public ErrorInfo addOccurrence() {
            return new ErrorInfo(
                component,
                errorType,
                errorMessage,
                severity,
                firstOccurrence,
                Instant.now(),
                occurrenceCount + 1
            );
        }
    }
    
    /**
     * Error statistics record.
     */
    public record ErrorStatistics(
        String component,
        long totalErrors,
        long criticalErrors,
        long warningErrors,
        ErrorSeverity overallSeverity
    ) {
        /**
         * Gets the error rate as a percentage of total operations.
         * 
         * @param totalOperations total operations for the period
         * @return error rate percentage
         */
        public double getErrorRatePercentage(long totalOperations) {
            return totalOperations > 0 ? ((double) totalErrors / totalOperations) * 100.0 : 0.0;
        }
    }
    
    /**
     * Error trends record.
     */
    public record ErrorTrends(
        java.time.Duration duration,
        int totalErrors,
        int criticalErrors,
        double errorRate
    ) {
        /**
         * Checks if the error trend is concerning.
         * 
         * @return true if trend is concerning
         */
        public boolean isConcerning() {
            return errorRate >= ERROR_RATE_WARNING_THRESHOLD || criticalErrors >= CRITICAL_ERROR_THRESHOLD_PER_HOUR;
        }
    }
    
    /**
     * Error severity enumeration.
     */
    public enum ErrorSeverity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL;
        
        /**
         * Determines overall severity based on critical and warning counts.
         * 
         * @param criticalCount number of critical errors
         * @param warningCount number of warning errors
         * @return overall severity
         */
        public static ErrorSeverity determineOverallSeverity(long criticalCount, long warningCount) {
            if (criticalCount > 0) {
                return CRITICAL;
            } else if (warningCount > 0) {
                return WARNING;
            } else {
                return INFO;
            }
        }
    }
}