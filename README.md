# Zoom Meeting Transcriber

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
- **Database**: MySQL for production
- **Real-time Communication**: WebSocket support with Spring's WebSocket module
- **AI Processing**: Ollama with qwen2.5:0.5b model for AI processing
- **Authentication**: JWT-based authentication and authorization
- **Monitoring**: Micrometer for metrics collection and Prometheus integration
- **Testing**: JUnit 5, Mockito (backend), Jasmine, Karma (frontend)

## 📋 Requirements

### System Requirements
- **Operating System**: Windows 10+, macOS 10.15+, or Ubuntu 20.04+
- **Docker**: Docker and Docker Compose installed
- **Memory**: Minimum 4GB RAM, 8GB recommended
- **Storage**: 2GB free space for application and models
- **Network**: Internet connection for initial model download and real-time communication

### Platform-Specific Requirements
- **macOS**: Ollama installed locally (`brew install ollama`)
- **Linux/Windows**: Ollama provided by Docker container

## 🎯 Quick Start with Docker

### Prerequisites
```bash
# Verify Docker installation
docker --version
docker-compose --version

# For macOS: Install Ollama locally
brew install ollama
ollama pull qwen2.5:0.5b
ollama serve
```

### Production Environment

#### macOS (using local Ollama)
```bash
# Set environment for macOS
export OLLAMA_URL=http://host.docker.internal:11434

# Start production stack
docker-compose up --build -d
```

#### Linux/Windows (using Docker Ollama)
```bash
# Start full production stack (includes Ollama container)
docker-compose up --build -d

# Download model in container
docker-compose exec ollama ollama pull qwen2.5:0.5b
```

### Environment Variables

Create a `.env` file to customize settings:

```bash
# Copy template
cp .env .env.local

# Edit for your environment
OLLAMA_URL=http://host.docker.internal:11434  # for macOS
```

## 🌐 Services Overview

| Service | Port | Description |
|----------|-------|-------------|
| Frontend | 4200 | Angular Web App |
| Backend | 8080 | Spring Boot API |
| MySQL | 3306 | Production Database |
| Redis | 6379 | Cache & Session Store |
| Ollama | 11434 | AI Model Service |
| Prometheus | 9090 | Metrics Collection |
| Grafana | 3000 | Monitoring Dashboard |

## 🔗 Access Points

After starting services:

- **Frontend**: http://localhost:4200
- **Backend API**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)

## 📊 Architecture

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Frontend  │    │   Backend   │    │   MySQL    │
│   (Angular) │◄──►│ (Spring Boot)│◄──►│             │
│   :4200      │    │   :8080     │    │   :3306     │
└─────────────┘    └─────────────┘    └─────────────┘
                          │                   │
                          ▼                   ▼
                   ┌─────────────┐    ┌─────────────┐
                   │    Redis    │    │   Ollama    │
                   │   :6379     │    │   :11434    │
                   └─────────────┘    └─────────────┘
                          │                   │
                          ▼                   ▼
                   ┌─────────────┐    ┌─────────────┐
                   │ Prometheus  │    │   Grafana   │
                   │   :9090     │    │   :3000     │
                   └─────────────┘    └─────────────┘
```

## 🔧 Configuration

### Environment Variables
- `OLLAMA_URL`: Ollama service URL (auto-detects platform)
- `DB_USERNAME`: Database user (default: `zoom_user`)
- `DB_PASSWORD`: Database password (default: `zoom_pass`)
- `API_URL`: Backend API URL for frontend

### Application Configuration
Configuration is managed through `src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  application:
    name: zoom-transcriber
  profiles:
    active: prod

  datasource:
    url: jdbc:mysql://mysql:3306/zoom_transcriber?useSSL=true&requireSSL=true
    username: ${DB_USERNAME:zoom_user}
    password: ${DB_PASSWORD:zoom_pass}

  data:
    redis:
      host: redis
      port: 6379

zoom:
  transcriber:
    ollama:
      host: localhost
      port: 11434
      default-model: qwen2.5:0.5b
```

## 📝 Data Persistence

- **MySQL**: Stored in `mysql_data` volume
- **Redis**: Stored in `redis_data` volume
- **Ollama**: Stored in `ollama_data` volume
- **Prometheus**: Stored in `prometheus_data` volume
- **Grafana**: Stored in `grafana_data` volume
- **Logs**: Mounted to `./logs` directory

## 🔍 Monitoring

### Prometheus Metrics
- Application metrics: http://localhost:9090/targets
- Custom metrics: http://localhost:8080/actuator/prometheus

### Grafana Dashboards
- Login: admin/admin
- Pre-configured dashboards for application monitoring
- Real-time performance metrics and health status

## 🛠️ Management Commands

### Docker Compose Operations
```bash
# Start all services
docker-compose up --build -d

# View logs
docker-compose logs -f backend
docker-compose logs -f frontend

# Check service status
docker-compose ps

# Stop services
docker-compose down

# Stop with volume cleanup
docker-compose down -v

# Rebuild specific service
docker-compose up --build -d backend

# Scale services
docker-compose up --scale backend=2
```

### Service-Specific Commands
```bash
# Access backend container
docker-compose exec backend bash

# Access MySQL database
docker-compose exec mysql mysql -u zoom_user -p zoom_transcriber

# Manage Ollama models
docker-compose exec ollama ollama list
docker-compose exec ollama ollama pull qwen2.5:0.5b

# View application logs
docker-compose exec backend tail -f /app/logs/zoom-transcriber.log
```

## 🔒 Security

### Authentication & Authorization
- **JWT Tokens**: Stateless authentication with refresh tokens
- **Role-based Access Control**: User roles and permissions
- **Password Security**: BCrypt encryption for password storage
- **CORS Configuration**: Proper cross-origin resource sharing setup

### Data Protection
- **Encryption**: Data encrypted in transit (HTTPS/WSS) and at rest
- **Input Validation**: Comprehensive input sanitization and validation
- **SQL Injection Prevention**: Parameterized queries and ORM usage

## 🚨 Troubleshooting

### Common Issues

#### Services Won't Start
```bash
# Check Docker Compose configuration
docker-compose config

# View service logs
docker-compose logs

# Check port conflicts
netstat -tulpn | grep :8080
```

#### Backend Connection Issues
```bash
# Check backend health
curl http://localhost:8080/actuator/health

# Verify database connection
docker-compose exec mysql mysql -u zoom_user -p zoom_transcriber

# Check Redis connection
docker-compose exec redis redis-cli ping
```

#### Ollama Connection Issues
```bash
# Check Ollama status (Linux/Windows)
docker-compose exec ollama ollama list

# For macOS, check local installation
ollama list
curl http://localhost:11434/api/tags
```

#### Frontend Build Issues
```bash
# Rebuild frontend
docker-compose build --no-cache frontend

# Clear Docker cache
docker system prune -f
```

### Performance Issues

#### High Memory Usage
```bash
# Monitor resource usage
docker stats

# Adjust JVM memory limits
docker-compose up -d --memory=2g backend
```

#### Slow Database Queries
```bash
# Enable slow query log
docker-compose exec mysql mysql -e "SET GLOBAL slow_query_log = 'ON';"

# Check database performance
docker-compose exec mysql mysql -e "SHOW PROCESSLIST;"
```

## 🚀 Production Deployment

### Environment Setup
1. **Update environment variables** in `.env` file
2. **Set strong passwords** for MySQL and Grafana
3. **Configure SSL** certificates
4. **Set up backup** strategies for volumes
5. **Monitor resource usage** and adjust limits

### Security Hardening
```bash
# Use production secrets
echo "DB_PASSWORD=your-secure-password" >> .env.local
echo "GF_SECURITY_ADMIN_PASSWORD=your-secure-admin-password" >> .env.local

# Enable HTTPS (configure reverse proxy)
# Example with nginx or traefik
```

### Backup Strategies
```bash
# Backup volumes
docker run --rm -v mysql_data:/data -v $(pwd):/backup alpine tar czf /backup/mysql-backup.tar.gz -C /data .

# Automated backup script
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
docker run --rm -v mysql_data:/data -v /backups:/backup alpine tar czf /backup/mysql-$DATE.tar.gz -C /data .
```

## 📈 Scaling

### Horizontal Scaling
```bash
# Scale backend services
docker-compose up --scale backend=3

# Load balancing with nginx
# Configure nginx upstream block
```

### Resource Optimization
```bash
# Limit resource usage
docker-compose up -d --memory=1g --cpus=0.5 backend

# Monitor performance
docker-compose exec backend curl http://localhost:8080/actuator/metrics
```

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

### Health and Monitoring
```http
GET    /actuator/health               # Application health
GET    /actuator/metrics         # Application metrics
GET    /actuator/info           # Application info
GET    /actuator/prometheus       # Prometheus metrics
```

## 🤝 Contributing

### Development Setup
```bash
# Clone repository
git clone https://github.com/your-org/zoom-transcriber.git
cd zoom-transcriber

# Start development environment
docker-compose up --build

# Run tests
docker-compose exec backend ./gradlew test
```

### Code Quality
- **Backend**: SpotBugs, PMD, Checkstyle analysis
- **Frontend**: ESLint, TypeScript strict mode
- **Testing**: >85% code coverage requirement
- **Documentation**: Comprehensive API documentation

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

For support and questions:
- Create an issue in the GitHub repository
- Check the troubleshooting section above
- Review the logs for error details

---

**Built with ❤️ using modern web technologies**