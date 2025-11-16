package com.zoomtranscriber.core.monitoring;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Aspect for performance monitoring using AOP patterns.
 * Automatically tracks method execution times and records performance metrics.
 * Supports custom monitoring annotations for selective monitoring.
 */
@Aspect
@Component
public class PerformanceAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceAspect.class);
    
    private final PerformanceMonitor performanceMonitor;
    
    /**
     * Constructs a new PerformanceAspect with the specified PerformanceMonitor.
     * 
     * @param performanceMonitor the performance monitor service
     */
    public PerformanceAspect(PerformanceMonitor performanceMonitor) {
        this.performanceMonitor = performanceMonitor;
    }
    
    /**
     * Around advice for methods annotated with @Monitored.
     * Automatically tracks execution time and records performance metrics.
     * 
     * @param joinPoint the join point representing the method execution
     * @param monitored the monitoring annotation
     * @return the result of the method execution
     * @throws Throwable if the method execution throws an exception
     */
    @Around("@annotation(monitored)")
    public Object monitorMethod(ProceedingJoinPoint joinPoint, Monitored monitored) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String operationName = className + "." + methodName;
        
        PerformanceMonitor.PerformanceContext context = null;
        
        try {
            // Start monitoring based on the component type
            switch (monitored.component()) {
                case AUDIO -> context = performanceMonitor.startAudioProcessing(operationName);
                case TRANSCRIPTION -> context = performanceMonitor.startTranscription(operationName);
                case AI_SERVICE -> context = performanceMonitor.startAiService(operationName);
                default -> {
                    logger.warn("Unknown component type: {}", monitored.component());
                    context = performanceMonitor.startAudioProcessing(operationName); // Default to audio
                }
            }
            
            logger.debug("Starting monitoring for: {}", operationName);
            
            // Proceed with method execution
            Object result = joinPoint.proceed();
            
            logger.debug("Completed monitoring for: {}", operationName);
            return result;
            
        } catch (Exception e) {
            // Record error if monitoring annotation specifies it
            if (monitored.recordErrors()) {
                performanceMonitor.recordError(monitored.component().name(), e.getClass().getSimpleName());
            }
            logger.error("Error in monitored method: {}", operationName, e);
            throw e;
        } finally {
            // Always close the performance context
            if (context != null) {
                context.close();
            }
        }
    }
    
    /**
     * Around advice for audio-related service methods.
     * Automatically monitors methods in audio service classes.
     * 
     * @param joinPoint the join point representing the method execution
     * @return the result of the method execution
     * @throws Throwable if the method execution throws an exception
     */
    @Around("execution(* com.zoomtranscriber.core.audio..*(..))")
    public Object monitorAudioMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorGenericMethod(joinPoint, ComponentType.AUDIO);
    }
    
    /**
     * Around advice for transcription-related service methods.
     * Automatically monitors methods in transcription service classes.
     * 
     * @param joinPoint the join point representing the method execution
     * @return the result of the method execution
     * @throws Throwable if the method execution throws an exception
     */
    @Around("execution(* com.zoomtranscriber.core.transcription..*(..))")
    public Object monitorTranscriptionMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorGenericMethod(joinPoint, ComponentType.TRANSCRIPTION);
    }
    
    /**
     * Around advice for AI-related service methods.
     * Automatically monitors methods in AI service classes.
     * 
     * @param joinPoint the join point representing the method execution
     * @return the result of the method execution
     * @throws Throwable if the method execution throws an exception
     */
    @Around("execution(* com.zoomtranscriber.core.ai..*(..))")
    public Object monitorAiMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorGenericMethod(joinPoint, ComponentType.AI_SERVICE);
    }
    
    /**
     * Generic method monitoring implementation.
     * 
     * @param joinPoint the join point representing the method execution
     * @param componentType the component type for categorization
     * @return the result of the method execution
     * @throws Throwable if the method execution throws an exception
     */
    private Object monitorGenericMethod(ProceedingJoinPoint joinPoint, ComponentType componentType) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String operationName = className + "." + methodName;
        
        PerformanceMonitor.PerformanceContext context = null;
        
        try {
            // Start monitoring for the specific component type
            switch (componentType) {
                case AUDIO -> context = performanceMonitor.startAudioProcessing(operationName);
                case TRANSCRIPTION -> context = performanceMonitor.startTranscription(operationName);
                case AI_SERVICE -> context = performanceMonitor.startAiService(operationName);
            }
            
            logger.debug("Starting automatic monitoring for: {}", operationName);
            
            // Proceed with method execution
            Object result = joinPoint.proceed();
            
            return result;
            
        } catch (Exception e) {
            // Record error for automatic monitoring
            performanceMonitor.recordError(componentType.name(), e.getClass().getSimpleName());
            logger.error("Error in automatically monitored method: {}", operationName, e);
            throw e;
        } finally {
            // Always close the performance context
            if (context != null) {
                context.close();
            }
        }
    }
    
    /**
     * Annotation for marking methods for performance monitoring.
     * Allows fine-grained control over monitoring behavior.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Monitored {
        
        /**
         * The component type for categorizing the monitoring.
         * 
         * @return the component type
         */
        ComponentType component() default ComponentType.AUDIO;
        
        /**
         * Whether to record errors that occur in monitored methods.
         * 
         * @return true if errors should be recorded
         */
        boolean recordErrors() default true;
        
        /**
         * Custom operation name for monitoring.
         * If empty, the method name will be used.
         * 
         * @return custom operation name
         */
        String operationName() default "";
    }
    
    /**
     * Enumeration of component types for monitoring categorization.
     */
    public enum ComponentType {
        AUDIO,
        TRANSCRIPTION,
        AI_SERVICE,
        SYSTEM,
        STORAGE
    }
}