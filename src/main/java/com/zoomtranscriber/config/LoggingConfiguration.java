package com.zoomtranscriber.config;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Configuration class for structured logging in the Zoom Transcriber application.
 * Sets up JSON structured logging with proper correlation IDs, performance tracking,
 * and integration with monitoring systems.
 */
@Configuration
public class LoggingConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingConfiguration.class);
    
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    
    /**
     * Constructs a new LoggingConfiguration with the specified dependencies.
     * 
     * @param meterRegistry micrometer meter registry for metrics
     */
    public LoggingConfiguration(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Initializes logging configuration after bean construction.
     * Sets up structured logging with correlation IDs and performance tracking.
     */
    @PostConstruct
    public void initializeLogging() {
        logger.info("Initializing structured logging configuration");
        
        // Set up custom log appenders if needed
        setupCustomAppenders();
        
        // Initialize MDC defaults
        initializeMDC();
        
        logger.info("Structured logging configuration completed");
    }
    
    /**
     * Bean for structured logging service.
     * 
     * @return structured logging service bean
     */
    @Bean
    public StructuredLoggingService structuredLoggingService() {
        return new StructuredLoggingService(meterRegistry, objectMapper);
    }
    
    /**
     * Bean for metrics logging service.
     * 
     * @return metrics logging service bean
     */
    @Bean
    public MetricsLoggingService metricsLoggingService() {
        return new MetricsLoggingService(meterRegistry, objectMapper);
    }
    
    /**
     * Sets up custom log appenders for structured logging.
     */
    private void setupCustomAppenders() {
        try {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            
            // Set up JSON structured logging appender for file output
            setupJsonFileAppender(loggerContext);
            
            // Set up performance logging appender
            setupPerformanceAppender(loggerContext);
            
        } catch (Exception e) {
            logger.error("Error setting up custom log appenders", e);
        }
    }
    
    /**
     * Sets up JSON file appender for structured logging.
     * 
     * @param loggerContext the logger context
     */
    private void setupJsonFileAppender(LoggerContext loggerContext) {
        try {
            FileAppender<ILoggingEvent> jsonAppender = new FileAppender<>();
            jsonAppender.setContext(loggerContext);
            jsonAppender.setFile("logs/zoom-transcriber-structured.log");
            jsonAppender.setAppend(true);
            
            // Set up JSON encoder
            JsonLogEncoder jsonEncoder = new JsonLogEncoder(objectMapper);
            jsonEncoder.setContext(loggerContext);
            jsonEncoder.start();
            
            jsonAppender.setEncoder(jsonEncoder);
            jsonAppender.start();
            
            // Add to root logger
            ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
            rootLogger.addAppender(jsonAppender);
            
            logger.info("JSON structured logging appender configured");
            
        } catch (Exception e) {
            logger.error("Error setting up JSON file appender", e);
        }
    }
    
    /**
     * Sets up performance logging appender.
     * 
     * @param loggerContext the logger context
     */
    private void setupPerformanceAppender(LoggerContext loggerContext) {
        try {
            ConsoleAppender<ILoggingEvent> performanceAppender = new ConsoleAppender<>();
            performanceAppender.setContext(loggerContext);
            
            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(loggerContext);
            encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
            encoder.start();
            
            performanceAppender.setEncoder(encoder);
            performanceAppender.addFilter(new PerformanceLogFilter());
            performanceAppender.start();
            
            // Add to performance logger
            ch.qos.logback.classic.Logger performanceLogger = 
                loggerContext.getLogger("com.zoomtranscriber.performance");
            performanceLogger.addAppender(performanceAppender);
            performanceLogger.setAdditive(false);
            
            logger.info("Performance logging appender configured");
            
        } catch (Exception e) {
            logger.error("Error setting up performance appender", e);
        }
    }
    
    /**
     * Initializes MDC with default values.
     */
    private void initializeMDC() {
        MDC.put("application", "zoom-transcriber");
        MDC.put("version", "1.0.0");
        MDC.put("instanceId", UUID.randomUUID().toString());
    }
    
    /**
     * Structured logging service for enhanced log management.
     */
    public static class StructuredLoggingService {
        
        private static final Logger structuredLogger = LoggerFactory.getLogger("com.zoomtranscriber.structured");
        private final MeterRegistry meterRegistry;
        private final ObjectMapper objectMapper;
        
        public StructuredLoggingService(MeterRegistry meterRegistry, ObjectMapper objectMapper) {
            this.meterRegistry = meterRegistry;
            this.objectMapper = objectMapper;
        }
        
        /**
         * Logs a structured event with correlation ID and context.
         * 
         * @param level log level
         * @param message log message
         * @param context additional context information
         */
        public void logStructuredEvent(String level, String message, Map<String, Object> context) {
            try {
                String correlationId = MDC.get("correlationId");
                if (correlationId == null) {
                    correlationId = UUID.randomUUID().toString();
                    MDC.put("correlationId", correlationId);
                }
                
                StructuredLogEntry logEntry = new StructuredLogEntry(
                    Instant.now(),
                    level,
                    message,
                    correlationId,
                    MDC.getCopyOfContextMap(),
                    context
                );
                
                String jsonLog = objectMapper.writeValueAsString(logEntry);
                
                switch (level.toUpperCase()) {
                    case "ERROR" -> structuredLogger.error(jsonLog);
                    case "WARN" -> structuredLogger.warn(jsonLog);
                    case "INFO" -> structuredLogger.info(jsonLog);
                    case "DEBUG" -> structuredLogger.debug(jsonLog);
                    default -> structuredLogger.info(jsonLog);
                }
                
                // Record metrics for structured logging
                meterRegistry.counter("zoom.logging.structured.count", 
                    "level", level.toLowerCase()).increment();
                    
            } catch (Exception e) {
                structuredLogger.error("Error logging structured event: {}", e.getMessage(), e);
            }
        }
        
        /**
         * Logs an error with structured context.
         * 
         * @param message error message
         * @param throwable the error
         * @param context additional context
         */
        public void logError(String message, Throwable throwable, Map<String, Object> context) {
            if (context == null) {
                context = new HashMap<>();
            }
            
            context.put("errorType", throwable.getClass().getSimpleName());
            context.put("errorMessage", throwable.getMessage());
            
            logStructuredEvent("ERROR", message, context);
        }
        
        /**
         * Logs performance metrics.
         * 
         * @param operation operation name
         * @param duration operation duration
         * @param context additional context
         */
        public void logPerformance(String operation, long duration, Map<String, Object> context) {
            if (context == null) {
                context = new HashMap<>();
            }
            
            context.put("operation", operation);
            context.put("duration", duration);
            context.put("category", "performance");
            
            logStructuredEvent("INFO", "Performance metric recorded", context);
            
            // Record performance metrics
            meterRegistry.timer("zoom.operation.duration", "operation", operation)
                .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
        
        /**
         * Structured log entry record.
         */
        public record StructuredLogEntry(
            Instant timestamp,
            String level,
            String message,
            String correlationId,
            Map<String, String> mdcContext,
            Map<String, Object> context
        ) {}
    }
    
    /**
     * Metrics logging service for collecting and exporting metrics.
     */
    public static class MetricsLoggingService {
        
        private static final Logger metricsLogger = LoggerFactory.getLogger("com.zoomtranscriber.metrics");
        private final MeterRegistry meterRegistry;
        private final ObjectMapper objectMapper;
        
        public MetricsLoggingService(MeterRegistry meterRegistry, ObjectMapper objectMapper) {
            this.meterRegistry = meterRegistry;
            this.objectMapper = objectMapper;
        }
        
        /**
         * Logs application metrics in structured format.
         * 
         * @param metricsSummary metrics summary
         */
        public void logMetrics(MetricsSummary metricsSummary) {
            try {
                String jsonMetrics = objectMapper.writeValueAsString(metricsSummary);
                metricsLogger.info(jsonMetrics);
                
                // Record metrics collection event
                meterRegistry.counter("zoom.metrics.collected.count").increment();
                
            } catch (Exception e) {
                metricsLogger.error("Error logging metrics: {}", e.getMessage(), e);
            }
        }
        
        /**
         * Metrics summary record.
         */
        public record MetricsSummary(
            Instant timestamp,
            Map<String, Object> performance,
            Map<String, Object> system,
            Map<String, Object> application
        ) {}
    }
    
    /**
     * Custom JSON encoder for structured logging.
     */
    private static class JsonLogEncoder extends ch.qos.logback.core.encoder.EncoderBase<ILoggingEvent> {
        
        private final ObjectMapper objectMapper;
        
        public JsonLogEncoder(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }
        
        @Override
        public byte[] encode(ILoggingEvent event) {
            try {
                Map<String, Object> logMap = new HashMap<>();
                logMap.put("timestamp", Instant.ofEpochMilli(event.getTimeStamp()));
                logMap.put("level", event.getLevel().toString());
                logMap.put("logger", event.getLoggerName());
                logMap.put("message", event.getFormattedMessage());
                logMap.put("thread", event.getThreadName());
                
                // Add MDC context if available
                Map<String, String> mdcMap = event.getMDCPropertyMap();
                if (mdcMap != null && !mdcMap.isEmpty()) {
                    logMap.put("mdc", mdcMap);
                }
                
                // Add stack trace for errors
                if (event.getThrowableProxy() != null) {
                    logMap.put("throwable", event.getThrowableProxy().getClassName());
                    logMap.put("message", event.getThrowableProxy().getMessage());
                }
                
                String json = objectMapper.writeValueAsString(logMap) + "\n";
                return json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                
            } catch (Exception e) {
                return ("{\"error\":\"Failed to encode log: " + e.getMessage() + "\"}\n")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        
        @Override
        public byte[] footerBytes() {
            return new byte[0];
        }
        
        @Override
        public byte[] headerBytes() {
            return new byte[0];
        }
    }
    
    /**
     * Filter for performance-related logs.
     */
    private static class PerformanceLogFilter extends Filter<ILoggingEvent> {
        
        @Override
        public FilterReply decide(ILoggingEvent event) {
            String loggerName = event.getLoggerName();
            
            // Only allow performance-related logs
            if (loggerName != null && (
                loggerName.contains("performance") ||
                loggerName.contains("metrics") ||
                event.getMDCPropertyMap() != null && 
                "true".equals(event.getMDCPropertyMap().get("performance")))) {
                return FilterReply.ACCEPT;
            }
            
            return FilterReply.DENY;
        }
    }
}