# Quick Start Guide

This guide will help you get the AgriConnect backend up and running in under 10 minutes.

## Prerequisites

Ensure you have the following installed:
- **Java 21+** - [Download](https://adoptium.net/)
- **MySQL 8.0+** - [Download](https://dev.mysql.com/downloads/)
- **Maven 3.8+** - [Download](https://maven.apache.org/download.cgi)
- **Git** - [Download](https://git-scm.com/downloads)

## Step 1: Clone the Repository

```bash
git clone <your-repo-url>
cd Backend
```

## Step 2: Set Up Database

1. Start MySQL server
2. Create database:

```sql
CREATE DATABASE IF NOT EXISTS `agri-connect`;
```

3. Verify connection:

```bash
mysql -u root -p agri-connect
```

## Step 3: Configure Environment Variables

### Quick Setup (Development)

For each service, copy the example file and update values:

```bash
# Main-Backend
cp Main-Backend/.env.example Main-Backend/.env

# Contract-Farming-App
cp Contract-Farming-App/.env.example Contract-Farming-App/.env

# Generate-Agreement-App
cp Generate-Agreement-App/.env.example Generate-Agreement-App/.env

# Market-Access-App
cp Market-Access-App/.env.example Market-Access-App/.env

# Eureka-Main-Server
cp Eureka-Main-Server/.env.example Eureka-Main-Server/.env
```

### Minimal Configuration

Edit each `.env` file with at least these values:

**Main-Backend/.env:**
```properties
DB_URL=jdbc:mysql://localhost:3306/agri-connect?createDatabaseIfNotExist=true
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
PORT=2525
JWT_SECRET=your_secret_key_minimum_256_bits_long_for_security
TWILIO_ACCOUNT_SID=your_twilio_sid_or_skip_for_test_numbers
TWILIO_AUTH_TOKEN=your_twilio_token_or_skip_for_test_numbers
TWILIO_PHONE_NUMBER=your_twilio_number_or_skip_for_test_numbers
```

**Contract-Farming-App/.env:**
```properties
DB_URL=jdbc:mysql://localhost:3306/agri-connect?createDatabaseIfNotExist=true
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
PORT=2526
RAZORPAY_KEY_ID=your_razorpay_key_or_dummy_value
RAZORPAY_KEY_SECRET=your_razorpay_secret_or_dummy_value
CONTRACT_ADDRESS=0x0000000000000000000000000000000000000000
PRIVATE_KEY=0000000000000000000000000000000000000000000000000000000000000000
API_URL=http://localhost:8545
```

**Generate-Agreement-App/.env:**
```properties
DB_URL=jdbc:mysql://localhost:3306/agri-connect?createDatabaseIfNotExist=true
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
PORT=2529
MAIL_USERNAME=your_email@gmail.com
MAIL_PASS=your_app_password
GOOGLE_MAP_API_KEY=your_google_maps_key_or_dummy
```

**Market-Access-App/.env:**
```properties
DB_URL=jdbc:mysql://localhost:3306/agri-connect?createDatabaseIfNotExist=true
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
PORT=2527
```

**Eureka-Main-Server/.env:**
```properties
PORT=8761
SPRING_PROFILES_ACTIVE=dev
```

## Step 4: Build All Services

```bash
mvn clean install
```

This will:
- Download all dependencies
- Compile all services
- Run tests
- Create executable JARs

## Step 5: Start Services

### Option A: Using Maven (Recommended for Development)

Open 5 separate terminals and run:

**Terminal 1 - Eureka Server (Start First!)**
```bash
cd Eureka-Main-Server
mvn spring-boot:run
```

Wait for Eureka to fully start (check http://localhost:8761), then start others:

**Terminal 2 - Main Backend**
```bash
cd Main-Backend
mvn spring-boot:run
```

**Terminal 3 - Contract Farming**
```bash
cd Contract-Farming-App
mvn spring-boot:run
```

**Terminal 4 - Market Access**
```bash
cd Market-Access-App
mvn spring-boot:run
```

**Terminal 5 - Generate Agreement**
```bash
cd Generate-Agreement-App
mvn spring-boot:run
```

### Option B: Using JAR Files

After building, you can run JARs directly:

```bash
# Terminal 1
java -jar Eureka-Main-Server/target/Eureka-Main-Server-0.0.1-SNAPSHOT.jar

# Terminal 2
java -jar Main-Backend/target/Main-Backend-0.0.1-SNAPSHOT.jar

# Terminal 3
java -jar Contract-Farming-App/target/Contract-Farming-App-0.0.1-SNAPSHOT.jar

# Terminal 4
java -jar Market-Access-App/target/Market-Access-App-0.0.1-SNAPSHOT.jar

# Terminal 5
java -jar Generate-Agreement-App/target/Generate-Agreement-App-0.0.1-SNAPSHOT.jar
```

## Step 6: Verify Services

### Check Eureka Dashboard
Open browser: http://localhost:8761

You should see all 4 services registered:
- MAIN-BACKEND
- CONTRACT-FARMING-APP
- MARKET-ACCESS-APP
- GENERATE-AGREEMENT-APP

### Check Health Endpoints

```bash
curl http://localhost:2525/actuator/health  # Main-Backend
curl http://localhost:2526/actuator/health  # Contract-Farming-App
curl http://localhost:2527/actuator/health  # Market-Access-App
curl http://localhost:2529/actuator/health  # Generate-Agreement-App
```

All should return: `{"status":"UP"}`

## Step 7: Test API Endpoints

### Register a User

```bash
curl -X POST http://localhost:2525/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "John Doe",
    "phoneNumber": "9876543210",
    "address": "123 Farm Road, Village, District, State"
  }'
```

**Response:** `{"message":"OTP sent successfully"}`

**Note:** For testing without Twilio, use test numbers: `8780850751` or `9924111980`

### Verify OTP and Register

```bash
curl -X POST http://localhost:2525/auth/r/verify-otp/9876543210/123456 \
  -H "Content-Type: application/json" \
  -d '{
    "username": "John Doe",
    "phoneNumber": "9876543210",
    "address": "123 Farm Road, Village, District, State"
  }'
```

### Login

```bash
curl -X POST http://localhost:2525/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "9876543210"
  }'
```

### Verify OTP and Get JWT Token

```bash
curl -X POST http://localhost:2525/auth/verify-otp/9876543210/123456 \
  -c cookies.txt
```

**Response:**
```json
{
  "jwtToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "role": "USER"
}
```

### Get Current User (Authenticated)

```bash
curl -X GET http://localhost:2525/auth/current-user \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Common Issues & Solutions

### Issue: "Communications link failure"
**Solution:** Ensure MySQL is running and credentials in .env are correct

```bash
# Check MySQL status
systemctl status mysql  # Linux
brew services list      # macOS
net start MySQL         # Windows
```

### Issue: Service not registering with Eureka
**Solution:** 
1. Ensure Eureka server is running first
2. Check EUREKA_SERVER_URL in .env
3. Wait 30 seconds for registration

### Issue: "Port already in use"
**Solution:** 
1. Check if another service is using the port
2. Change PORT in .env file
3. Kill the process using the port:

```bash
# Linux/Mac
lsof -ti:2525 | xargs kill -9

# Windows
netstat -ano | findstr :2525
taskkill /PID <PID> /F
```

### Issue: Build fails
**Solution:**
```bash
# Clean and rebuild
mvn clean install -U

# Skip tests if needed
mvn clean install -DskipTests
```

### Issue: JWT Token Invalid
**Solution:** Ensure JWT_SECRET is same across service restarts and is at least 256 bits

## Development Tips

### Hot Reload
Spring DevTools is included - changes to code will auto-reload

### Debug Mode
Add to VM arguments:
```
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
```

### View Logs
Logs are written to:
- Console (stdout)
- `logs/` directory in each service folder

### Database GUI
Use tools like:
- MySQL Workbench
- DBeaver
- TablePlus
- phpMyAdmin

### API Testing
Use tools like:
- Postman
- Insomnia
- cURL
- HTTPie

## Next Steps

1. **Read the Documentation**
   - `README.md` - Complete documentation
   - `REFACTORING_SUMMARY.md` - What was refactored
   - `PRODUCTION_CHECKLIST.md` - Production deployment guide

2. **Explore the Code**
   - Start with Controllers
   - Then Services
   - Then Repositories

3. **Test the APIs**
   - Import Postman collection (if available)
   - Test all endpoints
   - Understand the flow

4. **Configure External Services** (Optional)
   - Twilio - For real OTP
   - Razorpay - For payments
   - Google Maps - For location services

## Support

If you encounter any issues:
1. Check the logs in `logs/` directory
2. Verify all services are running
3. Check database connectivity
4. Verify environment variables
5. Consult the troubleshooting section in README.md

---

**You're all set! Happy coding! 🚀**
