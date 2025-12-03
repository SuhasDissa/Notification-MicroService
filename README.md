# Notification Microservice

A production-ready notification microservice built with Kotlin and Ktor framework, supporting Email, SMS, and Push notifications with comprehensive logging, monitoring, and DevOps best practices.

## Features

- ✉️ **Multi-channel notifications**: Email, SMS, Push notifications
- 🔄 **Retry mechanism**: Exponential backoff with configurable retry delays
- 🔌 **Circuit breaker**: Prevent cascading failures with provider-specific circuit breakers
- 📊 **Monitoring**: Prometheus metrics and Grafana dashboards
- 📝 **Structured logging**: JSON logging with MDC for correlation
- 🔐 **Security**: JWT authentication, CORS support
- 🗄️ **Database**: PostgreSQL with connection pooling
- 📨 **Message queue**: Kafka support for async processing
- 🐳 **Containerization**: Docker and Kubernetes ready
- 🧪 **Testing**: Unit and integration tests
- 📈 **Observability**: Health checks, metrics, distributed tracing ready

## Technology Stack

- **Language**: Kotlin 1.9.x
- **Framework**: Ktor 2.3.x
- **Build Tool**: Gradle 8.x
- **Database**: PostgreSQL with Exposed ORM
- **Message Queue**: Apache Kafka
- **Monitoring**: Prometheus + Grafana
- **Containerization**: Docker, Kubernetes

## Getting Started

### Prerequisites

- JDK 17 or higher
- Docker and Docker Compose
- PostgreSQL 14+ (or use Docker Compose)
- Apache Kafka 3.x (or use Docker Compose)

### Local Development

1. **Clone the repository**

```bash
git clone <repository-url>
cd Notification-MicroService
```

2. **Start dependencies with Docker Compose**

```bash
docker-compose up -d postgres kafka zookeeper
```

3. **Build the project**

```bash
./gradlew build
```

4. **Run the application**

```bash
./gradlew run
```

The service will start on `http://localhost:8080`

### Using Docker

**Build and run with Docker Compose:**

```bash
docker-compose up --build
```

This will start:
- Notification Service (port 8080)
- PostgreSQL (port 5432)
- Kafka + Zookeeper (port 9092)
- Prometheus (port 9090)
- Grafana (port 3000)

## API Documentation

### Send Notification

```http
POST /api/v1/notifications
Content-Type: application/json

{
  "type": "EMAIL",
  "recipient": "user@example.com",
  "subject": "Welcome!",
  "body": "Welcome to our service!",
  "priority": "NORMAL"
}
```

**Response:**
```json
{
  "notificationId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "QUEUED",
  "createdAt": "2024-01-01T12:00:00Z"
}
```

### Get Notification Status

```http
GET /api/v1/notifications/{id}
```

**Response:**
```json
{
  "notificationId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "EMAIL",
  "status": "DELIVERED",
  "attempts": 1,
  "lastAttemptAt": "2024-01-01T12:00:05Z",
  "deliveredAt": "2024-01-01T12:00:05Z"
}
```

### Batch Send

```http
POST /api/v1/notifications/batch
Content-Type: application/json

{
  "notifications": [
    {
      "type": "EMAIL",
      "recipient": "user1@example.com",
      "body": "Message 1"
    },
    {
      "type": "SMS",
      "recipient": "+1234567890",
      "body": "Message 2"
    }
  ]
}
```

### Health Check

```http
GET /health
```

**Response:**
```json
{
  "status": "healthy",
  "checks": {
    "database": "up",
    "emailProvider": "up",
    "smsProvider": "down",
    "pushProvider": "down"
  }
}
```

### Metrics

```http
GET /metrics
```

Returns Prometheus-formatted metrics.

## Configuration

Configuration is managed through `application.yaml`:

```yaml
ktor:
  deployment:
    port: 8080

database:
  url: "jdbc:postgresql://localhost:5432/notifications"
  user: "notif_user"
  password: "notif_pass"

providers:
  email:
    enabled: true
    host: "smtp.gmail.com"
    port: 587
    username: "your-email@gmail.com"
    password: "your-password"
```

### Environment Variables

You can override configuration using environment variables:

- `DATABASE_URL`: Database connection URL
- `DATABASE_USER`: Database username
- `DATABASE_PASSWORD`: Database password
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers

## Testing

**Run unit tests:**

```bash
./gradlew test
```

**Run integration tests:**

```bash
./gradlew integrationTest
```

**Generate test coverage report:**

```bash
./gradlew jacocoTestReport
```

## Deployment

### Kubernetes

1. **Create secrets:**

```bash
kubectl create secret generic notification-secrets \
  --from-literal=database-url='jdbc:postgresql://...' \
  --from-literal=database-user='user' \
  --from-literal=database-password='pass'
```

2. **Apply manifests:**

```bash
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml
```

3. **Verify deployment:**

```bash
kubectl get pods
kubectl get svc
```

## Monitoring

### Prometheus Metrics

Access Prometheus at `http://localhost:9090`

Key metrics:
- `http_server_requests_seconds`: Request duration
- `jvm_memory_used_bytes`: JVM memory usage
- `notifications_sent_total`: Total notifications sent
- `notifications_failed_total`: Failed notifications

### Grafana Dashboards

Access Grafana at `http://localhost:3000` (default credentials: admin/admin)

Import the provided dashboards for:
- Service health overview
- Notification throughput
- Error rates
- Provider performance

## Architecture

```
┌─────────────────────────────────────────┐
│          API Layer (Routes)             │
│  - REST endpoints                       │
│  - Request validation                   │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│        Service Layer (Business)         │
│  - Notification orchestration           │
│  - Template processing                  │
│  - Retry logic                          │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│       Provider Layer (Adapters)         │
│  - Email provider (SMTP)                │
│  - SMS provider (Twilio)                │
│  - Push notification (FCM)              │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│      Persistence Layer (Repository)     │
│  - Notification history                 │
│  - Templates                            │
│  - Delivery status tracking             │
└─────────────────────────────────────────┘
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License.

## Support

For issues and questions, please create an issue in the GitHub repository.