# Zoom Meeting Transcriber - Web Application

A modern web application that automatically detects Zoom meetings, transcribes audio in real-time, and generates AI-powered summaries. Built with Angular frontend and Spring Boot backend.

## 🚀 Features

### Core Functionality
- **Automatic Meeting Detection**: Detects when Zoom meetings start and begin transcription without manual intervention
- **Real-time Transcription**: Displays live transcription of meeting audio as it happens via WebSocket connections
- **AI-Powered Summaries**: Generates intelligent summaries of meetings with key points, decisions, and action items
- **Web-based Interface**: Accessible through any modern web browser with responsive design
- **Real-time Updates**: Live updates using WebSocket technology for instant transcription and meeting status
- **Cross-Platform Support**: Works on any device with a web browser (desktop, tablet, mobile)

### Technical Stack
- **Frontend**: Angular 17 with Angular Material, TypeScript, RxJS, NgRx for state management
- **Backend**: Spring Boot 3.2.0 with Spring Web, Spring WebFlux, Spring Security
- **Database**: MySQL for production, H2 for development/testing
- **Real-time Communication**: WebSocket support with Spring's WebSocket module
- **AI Processing**: Ollama with qwen2.5:0.5b model for AI processing
- **Authentication**: JWT-based authentication and authorization
- **Monitoring**: Micrometer for metrics collection and Prometheus integration
- **Testing**: JUnit 5, Mockito (backend), Jasmine, Karma (frontend)

## 📋 Requirements

### System Requirements
- **Operating System**: Windows 10+, macOS 10.15+, or Ubuntu 20.04+
- **Java**: Java 21 Runtime (for backend)
- **Node.js**: Node.js 18+ and npm 9+ (for frontend development)
- **Memory**: Minimum 4GB RAM, 8GB recommended
- **Storage**: 2GB free space for application and models
- **Network**: Internet connection for initial model download and real-time communication

### Development Prerequisites
```bash
# Verify Java installation
java -version

# Verify Node.js installation
node --version
npm --version

# Optional: Verify MySQL installation
mysql --version
```

## 🎯 Quick Start

### Option 1: Development Setup (Recommended)

1. **Clone the Repository**:
```bash
git clone https://github.com/your-org/zoom-transcriber.git
cd zoom-transcriber
```

2. **Backend Setup**:
```bash
# Navigate to project root
cd zoom-transcriber

# Build and run backend
./gradlew bootRun
```
The backend will start on `http://localhost:8080`

3. **Frontend Setup**:
```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Start development server
npm start
```
The frontend will start on `http://localhost:4200`

4. **Access the Application**:
Open your browser and navigate to `http://localhost:4200`

### Option 2: Production Deployment

See the [Deployment Section](#-deployment) for production deployment instructions.

## 📖 User Interface

### Web Application Features
- **Dashboard**: Overview of active meetings, recent transcriptions, and system status
- **Meeting Detection**: Automatic detection of Zoom meetings with real-time status updates
- **Live Transcription**: Real-time transcription display with speaker identification and confidence scores
- **Summary Generation**: AI-powered summaries with customizable focus areas
- **Configuration Management**: Web-based settings for audio, transcription, AI models, and privacy
- **User Authentication**: Secure login system with JWT tokens
- **Responsive Design**: Works seamlessly on desktop, tablet, and mobile devices

### Key Web Features
- **Real-time Updates**: WebSocket-based live updates without page refresh
- **Progressive Web App**: Offline capabilities and app-like experience
- **Dark/Light Theme**: Customizable interface themes
- **Export Options**: Save transcriptions and summaries in various formats (TXT, PDF, JSON)
- **Search & Filter**: Find past meetings and transcriptions quickly
- **Collaboration**: Share summaries and transcriptions with team members

## 🔧 Configuration

### Backend Configuration
Configuration is managed through `application.yml` and environment variables:

```yaml
# application.yml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/zoom_transcriber
    username: ${DB_USERNAME:zoom_user}
    password: ${DB_PASSWORD:zoom_pass}
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false

  security:
    jwt:
      secret: ${JWT_SECRET:your-secret-key}
      expiration: 86400000

ollama:
  base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
  model: qwen2.5:0.5b
```

### Frontend Configuration
Environment-specific configuration in `frontend/src/environments/`:

```typescript
// environment.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  wsUrl: 'ws://localhost:8080/ws',
  enableDebug: true
};

// environment.prod.ts
export const environment = {
  production: true,
  apiUrl: '/api',
  wsUrl: '/ws',
  enableDebug: false
};
```

### Audio Settings
- **Input Device**: Choose microphone or audio input source (browser permissions required)
- **Quality Settings**: Adjust audio quality for optimal transcription accuracy
- **Noise Reduction**: Browser-based noise filtering for clearer audio

### Transcription Settings
- **Language**: Auto-detect or manually select transcription language
- **Confidence Threshold**: Adjust minimum confidence score for transcription display
- **Real-time Display**: Configure live transcription updates frequency

### AI Settings
- **Model Selection**: Choose available AI models (qwen2.5:0.5b, llama3, etc.)
- **Summary Types**: Configure types of summaries to generate
- **Processing Options**: Adjust AI processing parameters

## 📊 Architecture

### Modern Web Architecture
- **Frontend**: Single Page Application (SPA) with Angular 17
- **Backend**: RESTful API with Spring Boot 3.2.0
- **Real-time Communication**: WebSocket connections for live updates
- **State Management**: NgRx for client-side state management
- **Database Layer**: JPA entities with Spring Data for database operations
- **Security**: JWT-based authentication with Spring Security
- **Monitoring**: Comprehensive health checks and metrics collection

### System Architecture Flow
1. **Frontend (Angular)**: User interface and client-side state management
2. **API Gateway**: Spring Boot REST endpoints for HTTP requests
3. **WebSocket Layer**: Real-time communication for live transcription
4. **Business Logic**: Spring services for core functionality
5. **Data Layer**: JPA repositories and MySQL database
6. **External Services**: Ollama for AI processing, system APIs for meeting detection

### Technology Integration
```
┌─────────────────┐    HTTP/WebSocket    ┌──────────────────┐
│   Angular SPA   │ ◄──────────────────► │  Spring Boot     │
│   (Port 4200)   │                      │  (Port 8080)     │
└─────────────────┘                      └──────────────────┘
         │                                        │
         │                                        ▼
         │                              ┌──────────────────┐
         │                              │   MySQL DB       │
         │                              │   (Port 3306)    │
         │                              └──────────────────┘
         │
         ▼
┌─────────────────┐
│   User Browser  │
│   (Any Device)  │
└─────────────────┘
```

## 🔒 Development

### Backend Development
```bash
# Prerequisites
Java 21+
Gradle 8.5+
MySQL 8.0+

# Clone and build
git clone <repository-url>
cd zoom-transcriber

# Run tests
./gradlew test

# Build application
./gradlew build

# Run in development mode
./gradlew bootRun

# Run with specific profile
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Frontend Development
```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Run development server
npm start

# Run tests
npm test

# Run tests with coverage
npm run test:ci

# Build for production
npm run build:prod

# Analyze bundle size
npm run analyze

# Lint code
npm run lint
```

### Project Structure
```
zoom-transcriber/
├── frontend/                          # Angular frontend
│   ├── src/
│   │   ├── app/
│   │   │   ├── core/                  # Core services and utilities
│   │   │   ├── features/              # Feature modules
│   │   │   │   ├── dashboard/         # Dashboard component
│   │   │   │   ├── detection/         # Meeting detection
│   │   │   │   ├── authentication/    # User authentication
│   │   │   │   └── transcription/     # Transcription interface
│   │   │   ├── shared/                # Shared components and modules
│   │   │   └── store/                 # NgRx store setup
│   │   ├── assets/                    # Static assets
│   │   └── environments/              # Environment configurations
│   ├── package.json                   # Frontend dependencies
│   ├── angular.json                   # Angular CLI configuration
│   └── tsconfig.json                  # TypeScript configuration
├── src/main/java/com/zoomtranscriber/ # Spring Boot backend
│   ├── api/                          # REST controllers
│   │   ├── AuthenticationController.java
│   │   ├── MeetingController.java
│   │   ├── TranscriptionController.java
│   │   └── SummaryController.java
│   ├── config/                       # Configuration classes
│   │   ├── SecurityConfiguration.java
│   │   ├── WebSocketConfiguration.java
│   │   └── DatabaseConfig.java
│   ├── core/                         # Business logic
│   │   ├── audio/                    # Audio capture and processing
│   │   ├── transcription/            # Speech recognition
│   │   ├── detection/               # Meeting detection
│   │   ├── ai/                      # AI services
│   │   └── storage/                 # JPA entities
│   ├── security/                     # JWT authentication
│   └── websocket/                    # WebSocket handlers
├── build.gradle                      # Backend build configuration
└── README.md                         # This file
```

## 🧪 Testing

### Backend Testing
```bash
# Run all tests
./gradlew test

# Run integration tests
./gradlew integrationTest

# Run with coverage report
./gradlew test jacocoTestReport

# Run specific test class
./gradlew test --tests "*MeetingControllerTest"
```

### Frontend Testing
```bash
# Navigate to frontend directory
cd frontend

# Run unit tests
npm test

# Run tests in CI mode (headless)
npm run test:ci

# Run end-to-end tests
npm run e2e

# Generate coverage report
npm run test:ci -- --code-coverage
```

### Test Coverage Targets
- **Backend**: >85% code coverage with JaCoCo
- **Frontend**: >80% code coverage with Istanbul
- **Integration Tests**: Complete workflow coverage
- **E2E Tests**: Critical user journey coverage

## 📚 API Documentation

### Authentication Endpoints
```http
POST   /api/auth/login           # User authentication
POST   /api/auth/refresh         # Token refresh
POST   /api/auth/logout          # User logout
GET    /api/auth/profile         # User profile
```

### Meeting Management
```http
GET    /api/meetings             # List all meetings
POST   /api/meetings             # Create new meeting
GET    /api/meetings/{id}        # Get meeting details
PUT    /api/meetings/{id}        # Update meeting
DELETE /api/meetings/{id}        # Delete meeting
GET    /api/meetings/active      # Get active meeting
```

### Transcription Endpoints
```http
GET    /api/transcriptions       # List transcriptions
GET    /api/transcriptions/{id}  # Get transcription by ID
POST   /api/transcriptions/start # Start transcription
POST   /api/transcriptions/stop  # Stop transcription
```

### Summary Endpoints
```http
GET    /api/summaries            # List summaries
POST   /api/summaries            # Generate summary
GET    /api/summaries/{id}       # Get summary by ID
PUT    /api/summaries/{id}       # Update summary
```

### Health and Monitoring
```http
GET    /api/health               # Application health
GET    /actuator/metrics         # Application metrics
GET    /actuator/info           # Application info
```

### WebSocket Endpoints
```javascript
// Connect to WebSocket
const ws = new WebSocket('ws://localhost:8080/ws/transcription');

// Receive real-time transcription
ws.onmessage = function(event) {
  const data = JSON.parse(event.data);
  console.log('Transcription:', data);
};
```

## 🔐 Security & Privacy

### Authentication & Authorization
- **JWT Tokens**: Stateless authentication with refresh tokens
- **Role-based Access Control**: User roles and permissions
- **Password Security**: BCrypt encryption for password storage
- **Session Management**: Secure session handling and timeout

### Data Protection
- **Encryption**: Data encrypted in transit (HTTPS/WSS) and at rest
- **CORS Configuration**: Proper cross-origin resource sharing setup
- **Input Validation**: Comprehensive input sanitization and validation
- **SQL Injection Prevention**: Parameterized queries and ORM usage

### Privacy Features
- **Local Processing Options**: Configure AI processing location
- **Data Retention Policies**: Automatic cleanup of old meeting data
- **User Consent**: Granular privacy controls and user consent management
- **Audit Logging**: Comprehensive logging for security and compliance

## 🚀 Deployment

### Development Deployment
```bash
# Start backend
./gradlew bootRun

# Start frontend (in separate terminal)
cd frontend && npm start
```

### Production Deployment

#### Option 1: Docker Deployment
```dockerfile
# Dockerfile (Backend)
FROM openjdk:21-jdk-slim
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]

# Dockerfile (Frontend)
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build:prod

FROM nginx:alpine
COPY --from=build /app/dist/zoom-transcriber-web /usr/share/nginx/html
EXPOSE 80
```

```yaml
# docker-compose.yml
version: '3.8'
services:
  backend:
    build: .
    ports:
      - "8080:8080"
    environment:
      - DB_URL=jdbc:mysql://mysql:3306/zoom_transcriber
      - DB_USERNAME=zoom_user
      - DB_PASSWORD=zoom_pass
    depends_on:
      - mysql

  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend

  mysql:
    image: mysql:8.0
    environment:
      - MYSQL_DATABASE=zoom_transcriber
      - MYSQL_USER=zoom_user
      - MYSQL_PASSWORD=zoom_pass
      - MYSQL_ROOT_PASSWORD=root_password
    volumes:
      - mysql_data:/var/lib/mysql

volumes:
  mysql_data:
```

#### Option 2: Cloud Deployment
```bash
# Build frontend for production
cd frontend
npm run build:prod

# Build backend JAR
cd ..
./gradlew build

# Deploy to cloud platform (AWS, GCP, Azure)
# Example for AWS Elastic Beanstalk:
eb create zoom-transcriber-prod
```

### Environment Variables
```bash
# Required for production
export DB_URL=jdbc:mysql://your-db-host:3306/zoom_transcriber
export DB_USERNAME=your_db_user
export DB_PASSWORD=your_db_password
export JWT_SECRET=your_jwt_secret_key
export OLLAMA_BASE_URL=http://your-ollama-host:11434
```

### Monitoring & Logging
- **Application Logs**: Structured logging with Logback
- **Metrics**: Prometheus metrics via Micrometer
- **Health Checks**: Spring Boot Actuator endpoints
- **Error Tracking**: Centralized error collection
- **Performance Monitoring**: APM integration support

## 🔧 Troubleshooting

### Common Issues

#### Backend Issues
**Problem**: Backend fails to start on port 8080
```bash
# Check if port is in use
lsof -i :8080

# Kill process using port 8080
kill -9 <PID>

# Or use different port
./gradlew bootRun --args='--server.port=8081'
```

**Problem**: Database connection issues
```bash
# Check MySQL status
systemctl status mysql

# Verify database exists
mysql -u root -p -e "SHOW DATABASES;"

# Check connection
./gradlew bootRun --args='--spring.datasource.url=jdbc:mysql://localhost:3306/zoom_transcriber'
```

**Problem**: Ollama service not available
```bash
# Check Ollama status
ollama list

# Start Ollama service
ollama serve

# Pull required model
ollama pull qwen2.5:0.5b
```

#### Frontend Issues
**Problem**: Frontend cannot connect to backend
```bash
# Check CORS configuration
# Verify proxy configuration in angular.json

# Temporarily disable CORS in development
# Add to application.yml:
# spring:
#   web:
#     cors:
#       allowed-origins: "*"
```

**Problem**: WebSocket connection issues
```bash
# Check WebSocket endpoint
curl -i -N -H "Connection: Upgrade" \
     -H "Upgrade: websocket" \
     -H "Sec-WebSocket-Key: test" \
     -H "Sec-WebSocket-Version: 13" \
     http://localhost:8080/ws/transcription
```

**Problem**: Browser microphone permissions
- Ensure HTTPS in production (microphone requires secure context)
- Check browser settings for microphone permissions
- Test microphone on https://webcammictest.com/

### Performance Issues
**Problem**: Slow transcription response
```bash
# Check system resources
top
htop

# Monitor application metrics
curl http://localhost:8080/actuator/metrics

# Check database performance
mysql -u root -p -e "SHOW PROCESSLIST;"
```

### Getting Help
- **Logs**: Check `logs/` directory for application logs
- **Health Check**: Visit `http://localhost:8080/api/health`
- **Metrics**: Visit `http://localhost:8080/actuator/metrics`
- **GitHub Issues**: Report bugs via GitHub Issues
- **Documentation**: Check the project wiki for detailed guides

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Workflow
1. **Fork the repository** from GitHub
2. **Create a feature branch**: `git checkout -b feature/amazing-feature`
3. **Follow coding standards** and testing requirements
4. **Run all tests**: `./gradlew test && cd frontend && npm test`
5. **Commit changes**: `git commit -m 'Add amazing feature'`
6. **Push to branch**: `git push origin feature/amazing-feature`
7. **Submit Pull Request** with comprehensive tests and documentation

### Code Style
- **Backend**: Follow Google Java Style Guide
- **Frontend**: Follow Angular Style Guide
- **Testing**: Maintain >85% test coverage
- **Documentation**: Update README and API docs for new features

### License
This project is licensed under the MIT License - see [LICENSE](LICENSE) for details.

---

## 📞 Support

For issues, questions, or contributions:
- **Documentation**: Check this README and our wiki
- **Issues**: Report bugs via GitHub Issues
- **Discussions**: Use GitHub Discussions for questions and ideas
- **Email**: Contact our development team at support@zoomtranscriber.com

### Quick Links
- **Live Demo**: [https://demo.zoomtranscriber.com](https://demo.zoomtranscriber.com)
- **API Documentation**: [https://docs.zoomtranscriber.com](https://docs.zoomtranscriber.com)
- **Status Page**: [https://status.zoomtranscriber.com](https://status.zoomtranscriber.com)

---

*Built with ❤️ for productive meetings*