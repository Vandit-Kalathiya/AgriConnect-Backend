# AgriConnect Backend - Microservices Architecture

A production-ready microservices architecture for the AgriConnect agricultural platform, built with Spring Boot and Spring Cloud.

## Architecture Overview

The backend consists of 5 microservices:

1. **Eureka-Main-Server** (Port 8761) - Service Discovery & Registry
2. **Main-Backend** (Port 2525) - Authentication & User Management
3. **Contract-Farming-App** (Port 2526) - Contract Farming & Blockchain
4. **Market-Access-App** (Port 2527) - Product Listings & Marketplace
5. **Generate-Agreement-App** (Port 2529) - Contract Generation & Email

## Technology Stack

- **Framework**: Spring Boot 3.x
- **Cloud**: Spring Cloud (Eureka, Config)
- **Security**: Spring Security, JWT
- **Database**: MySQL 8.0
- **ORM**: JPA/Hibernate
- **Build Tool**: Maven
- **Java Version**: 21-24
- **Additional**: Twilio SMS, Razorpay Payments, Web3j Blockchain, iTextPDF

## Prerequisites

- Java 21 or higher
- MySQL 8.0+
- Maven 3.8+
- Git

## Environment Setup

Each microservice requires its own `.env` file. Example templates are provided as `.env.example` files.

### 1. Copy environment templates

```bash
# For each service, copy the .env.example to .env
cp Main-Backend/.env.example Main-Backend/.env
cp Contract-Farming-App/.env.example Contract-Farming-App/.env
cp Generate-Agreement-App/.env.example Generate-Agreement-App/.env
cp Market-Access-App/.env.example Market-Access-App/.env
cp Eureka-Main-Server/.env.example Eureka-Main-Server/.env
```

### 2. Configure environment variables

Edit each `.env` file with your actual configuration values:

#### Main-Backend (.env)
```properties
DB_URL=jdbc:mysql://localhost:3306/agri-connect?createDatabaseIfNotExist=true
DB_USERNAME=root
DB_PASSWORD=your_password
JWT_SECRET=your_jwt_secret_min_256_bits
TWILIO_ACCOUNT_SID=your_twilio_sid
TWILIO_AUTH_TOKEN=your_twilio_token
TWILIO_PHONE_NUMBER=your_twilio_number
```

#### Contract-Farming-App (.env)
```properties
RAZORPAY_KEY_ID=your_razorpay_key
RAZORPAY_KEY_SECRET=your_razorpay_secret
CONTRACT_ADDRESS=your_blockchain_contract_address
PRIVATE_KEY=your_private_key
API_URL=http://localhost:8545
```

#### Generate-Agreement-App (.env)
```properties
MAIL_USERNAME=your_email@gmail.com
MAIL_PASS=your_app_password
GOOGLE_MAP_API_KEY=your_google_maps_key
```

### 3. Database Setup

```sql
CREATE DATABASE IF NOT EXISTS `agri-connect`;
```

The tables will be auto-created on first run (JPA DDL auto=update).

## Building the Project

### Build all services

```bash
mvn clean install
```

### Build individual service

```bash
cd Main-Backend
mvn clean install
```

## Running the Services

### Recommended Startup Order

1. **Start Eureka Server first**
```bash
cd Eureka-Main-Server
mvn spring-boot:run
```
Wait for Eureka to fully start (check http://localhost:8761)

2. **Start other services in any order**
```bash
# Terminal 2
cd Main-Backend
mvn spring-boot:run

# Terminal 3
cd Contract-Farming-App
mvn spring-boot:run

# Terminal 4
cd Market-Access-App
mvn spring-boot:run

# Terminal 5
cd Generate-Agreement-App
mvn spring-boot:run
```

## Service Endpoints

### Eureka Server
- Dashboard: http://localhost:8761

### Main-Backend
- Base URL: http://localhost:2525
- Health: http://localhost:2525/actuator/health
- Metrics: http://localhost:2525/actuator/prometheus

### Contract-Farming-App
- Base URL: http://localhost:2526
- Health: http://localhost:2526/actuator/health

### Market-Access-App
- Base URL: http://localhost:2527
- Health: http://localhost:2527/actuator/health

### Generate-Agreement-App
- Base URL: http://localhost:2529
- Health: http://localhost:2529/actuator/health

## API Documentation

### Authentication (Main-Backend)

#### Register User
```http
POST /auth/register
Content-Type: application/json

{
  "username": "John Doe",
  "phoneNumber": "9876543210",
  "address": "123 Farm Road"
}
```

#### Send OTP
```http
POST /auth/send-login-otp
Content-Type: application/json

{
  "mobileNumber": "9876543210"
}
```

#### Verify OTP & Login
```http
POST /auth/verify-and-login
Content-Type: application/json

{
  "mobileNumber": "9876543210",
  "otp": "123456"
}
```

## Production Configuration

### Environment Profiles

Set `SPRING_PROFILES_ACTIVE=prod` for production deployment.

### Production Checklist

- [ ] Change default passwords in `.env` files
- [ ] Use strong JWT secret (256 bits minimum)
- [ ] Enable HTTPS (set `jwtCookie.setSecure(true)`)
- [ ] Configure proper CORS origins
- [ ] Set `JPA_DDL_AUTO=validate` (never use `update` in production)
- [ ] Enable authentication for actuator endpoints
- [ ] Configure proper database connection pooling
- [ ] Set up log aggregation
- [ ] Configure Eureka for production (multiple instances)
- [ ] Set up monitoring and alerting

### Security Best Practices

1. **Never commit `.env` files** - They're already in `.gitignore`
2. **Use environment variables** for all sensitive data
3. **Rotate secrets regularly** (JWT, API keys, passwords)
4. **Enable HTTPS** in production
5. **Use strong password policies**
6. **Implement rate limiting** on public endpoints
7. **Keep dependencies updated** regularly

## Monitoring & Observability

### Health Checks

All services expose health endpoints via Spring Boot Actuator:

```bash
curl http://localhost:2525/actuator/health
```

### Metrics

Prometheus metrics are available at:

```bash
curl http://localhost:2525/actuator/prometheus
```

### Logging

Logs are configured per service:
- Main-Backend: `logs/main-backend.log`
- Contract-Farming-App: `logs/contract-farming.log`
- Market-Access-App: `logs/market-access.log`
- Generate-Agreement-App: `logs/generate-agreement.log`
- Eureka-Server: `logs/eureka-server.log`

## Error Handling

All services implement standardized error responses:

```json
{
  "timestamp": "2026-01-25T10:30:00",
  "status": 404,
  "error": "Not Found",
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "User not found with phoneNumber: '9876543210'",
  "path": "/api/users/9876543210"
}
```

## Common Issues & Troubleshooting

### Database Connection Issues
```
Error: Communications link failure
Solution: Ensure MySQL is running and credentials are correct in .env
```

### Eureka Registration Issues
```
Error: Service not registering with Eureka
Solution: 
1. Ensure Eureka server is running
2. Check EUREKA_SERVER_URL in .env
3. Verify network connectivity
```

### JWT Token Issues
```
Error: JWT signature does not match
Solution: Ensure JWT_SECRET is same across restarts
```

## Development

### Code Structure

```
Backend/
├── Eureka-Main-Server/          # Service Discovery
├── Main-Backend/                # Auth & Users
│   ├── src/main/java/.../
│   │   ├── config/             # Configuration classes
│   │   ├── Controller/         # REST Controllers
│   │   ├── Service/            # Business logic
│   │   ├── Repository/         # Data access
│   │   ├── Entity/             # JPA entities
│   │   ├── DTO/                # Data Transfer Objects
│   │   ├── exception/          # Exception handling
│   │   ├── jwt/                # JWT utilities
│   │   └── security/           # Security config
│   └── .env                    # Environment variables
├── Contract-Farming-App/        # Contracts & Blockchain
├── Market-Access-App/           # Marketplace
├── Generate-Agreement-App/      # PDF Generation
└── README.md
```

### Adding a New Microservice

1. Create Spring Boot project with Eureka Client dependency
2. Add `.env` and `.env.example` files
3. Configure `application.yml` with environment variables
4. Implement exception handling (copy from existing services)
5. Add health check endpoints via Actuator
6. Register with Eureka server
7. Update this README

## Testing

### Unit Tests

```bash
mvn test
```

### Integration Tests

```bash
mvn verify
```

## Deployment

### Docker Deployment (Future)

```bash
# Build Docker images
docker-compose build

# Start all services
docker-compose up -d
```

### Cloud Deployment

Services can be deployed to:
- AWS (ECS, EKS, Elastic Beanstalk)
- Google Cloud (GKE, Cloud Run)
- Azure (AKS, App Service)
- Heroku
- Railway

## Contributing

1. Create a feature branch
2. Follow existing code structure and naming conventions
3. Add appropriate exception handling
4. Include logging for important operations
5. Update `.env.example` if adding new environment variables
6. Test thoroughly before committing
7. Create a pull request with clear description

## License

[Your License Here]

## Support

For issues and questions:
- Create an issue in the repository
- Contact the development team

---

**Last Updated**: January 2026
**Version**: 1.0.0
