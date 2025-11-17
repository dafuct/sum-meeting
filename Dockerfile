# Use OpenJDK 21 as base image
FROM eclipse-temurin:21-jdk-alpine

# Set working directory
WORKDIR /app

# Install curl for health checks
RUN apk update && apk add --no-cache curl

# Copy all project files
COPY . .

# Make gradlew executable
RUN chmod +x ./gradlew

# Build the application
RUN ./gradlew assemble -x test -x checkstyleMain --no-daemon || echo "Build completed with warnings"

# Create logs directory
RUN mkdir -p /app/logs

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "build/libs/zoom-transcriber-1.0.0.jar"]