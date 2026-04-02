# Production Deployment Checklist

> **Last updated:** April 2026 — includes Kafka, Schema Registry, Notification-Service, Ws-Gateway, and WebSocket items.

## Pre-Deployment Security

### Environment Variables
- [ ] All `.env` files have production values (not development/test values)
- [ ] JWT_SECRET is strong (minimum 256 bits, randomly generated)
- [ ] Database password is strong and unique
- [ ] Twilio credentials are for production account
- [ ] Razorpay keys are for live mode (not test mode)
- [ ] Google Maps API key has production restrictions
- [ ] Email password is using app-specific password (not main password)
- [ ] Blockchain private keys are secured (consider using KMS)
- [ ] No `.env` files are committed to Git (verify with `git ls-files | grep .env`)
- [ ] `GATEWAY_URL` is set to the production public URL (e.g. `https://api.yourdomain.com`) in root `.env` and all service environments
- [ ] `KAFKA_BOOTSTRAP_SERVERS` is set to production Kafka broker addresses in all producer/consumer services
- [ ] `SCHEMA_REGISTRY_URL` is set to production Schema Registry URL in all producer/consumer services
- [ ] `KAFKA_CONSUMER_GROUP_ID` is set correctly in `Notification-Service`
- [ ] `WS_GATEWAY_CORS_ALLOWED_ORIGINS` includes production frontend domain in `Ws-Gateway/.env`

### Database Configuration
- [ ] Database is set up with proper user permissions
- [ ] Database has automated backups enabled
- [ ] `JPA_DDL_AUTO` is set to `validate` (NOT `update` or `create`)
- [ ] Database connection pool size is optimized for production load
- [ ] Database connection timeout is configured appropriately
- [ ] Database SSL/TLS is enabled if required

### Application Configuration
- [ ] `SPRING_PROFILES_ACTIVE` is set to `prod`
- [ ] Log level is set to `INFO` or `WARN` for production
- [ ] `JPA_SHOW_SQL` is set to `false`
- [ ] CORS allowed origins are restricted to production domains
- [ ] File upload size limits are appropriate
- [ ] Session timeout is configured appropriately

## Security Hardening

### HTTPS/TLS
- [ ] SSL/TLS certificates are installed and valid
- [ ] HTTP to HTTPS redirect is configured
- [ ] JWT cookie `Secure` flag is set to `true`
- [ ] JWT cookie `HttpOnly` flag is set to `true`
- [ ] HSTS (HTTP Strict Transport Security) header is enabled

### Authentication & Authorization
- [ ] Strong password policies are enforced
- [ ] JWT token expiration is set appropriately
- [ ] Refresh token mechanism is implemented (if needed)
- [ ] Session management is configured properly
- [ ] Failed login attempt limiting is implemented
- [ ] Account lockout policy is configured

### API Security
- [ ] Rate limiting is enabled on public endpoints
- [ ] API endpoints are protected with authentication
- [ ] Input validation is enabled on all endpoints
- [ ] SQL injection protection is verified
- [ ] XSS protection is enabled
- [ ] CSRF protection is configured for state-changing operations
- [ ] Sensitive endpoints have additional authorization checks

## Infrastructure

### Server Configuration
- [ ] Firewall rules are configured (allow only necessary ports)
- [ ] **Only port 8080 (Api-Gateway) and 8081 (Ws-Gateway) are publicly exposed** — all other service ports are internal only
- [ ] Ports 2530, 2526, 2527, 2529, 8761 are blocked from external access
- [ ] SSH access is secured (key-based authentication, no root login)
- [ ] Server has adequate resources (CPU, RAM, Disk)
- [ ] Swap space is configured
- [ ] Disk space monitoring is set up

### API Gateway
- [ ] Api-Gateway service is running and healthy (`/actuator/health`)
- [ ] All routes are active (`/actuator/gateway/routes`) — expected: main, contract, agreement, market, notifications
- [ ] `GATEWAY_URL` env variable points to the public-facing gateway URL
- [ ] Gateway port (8080) is the **only** backend HTTP port exposed to the public internet
- [ ] Service ports (2526, 2527, 2529, 2530, 8761) are firewall-blocked from external access
- [ ] Circuit breaker thresholds are tuned for production load
- [ ] `DedupeResponseHeader` filter is in `default-filters` (prevents duplicate CORS headers)
- [ ] Swagger UI is either disabled or access-restricted in production (`springdoc.swagger-ui.enabled=false`)
- [ ] CORS `allowed-origins` is restricted to production frontend domains

### Ws-Gateway (WebSocket Gateway)
- [ ] Ws-Gateway service is running and healthy (`/actuator/health`)
- [ ] Ws-Gateway port (8081) is the **only** WebSocket port exposed to the public internet
- [ ] Route `ws://Ws-Gateway/notifications/ws/**` correctly proxies to `Notification-Service`
- [ ] `globalcors` in `application.yml` includes only production frontend domains
- [ ] `CORS_ALLOWED_ORIGINS` env variable is set to production frontend domain

### Kafka and Schema Registry
- [ ] Kafka broker is running and accessible from all producer/consumer services
- [ ] Schema Registry is running and accessible
- [ ] All Avro schemas are registered in Schema Registry (`agriconnect.notifications.*-value`)
- [ ] All four notification topics exist: `auth`, `market`, `contract`, `agreement`
- [ ] DLQ topic `agriconnect.notifications.dlq` exists
- [ ] Kafka is NOT exposed to the public internet (internal network only)
- [ ] Kafka `KAFKA_BOOTSTRAP_SERVERS` in all `.env` files points to production broker

### Notification-Service
- [ ] Notification-Service is running and registered in Eureka
- [ ] Kafka consumer is connected and consuming from all four topics
- [ ] Email dispatcher is configured and can send emails
- [ ] SMS dispatcher is configured (Twilio credentials set)
- [ ] Push dispatcher is configured (Firebase credentials set, or gracefully disabled)
- [ ] In-App dispatcher is saving to PostgreSQL and pushing over WebSocket
- [ ] DLQ topic is being monitored (no unexpected failed events)
- [ ] Notification-Service port (2530) is firewall-blocked from external access

### Eureka Server
- [ ] Eureka server is running and accessible
- [ ] Eureka server has proper authentication (if exposed)
- [ ] Eureka server URL is correctly configured in all services
- [ ] Multiple Eureka instances for high availability (recommended)

### Database Server
- [ ] Database server is running
- [ ] Database server firewall allows only application servers
- [ ] Database is NOT exposed to public internet
- [ ] Database connection pooling is configured
- [ ] Database query performance is optimized

### File Storage
- [ ] File upload directory has proper permissions
- [ ] File storage has sufficient space
- [ ] File backup strategy is in place
- [ ] Image compression is enabled (if applicable)

## Monitoring & Logging

### Application Monitoring
- [ ] Actuator endpoints are exposed but secured
- [ ] Health check endpoints are accessible to load balancer
- [ ] Prometheus metrics are being collected
- [ ] Application performance monitoring (APM) is configured
- [ ] Memory and CPU usage is monitored

### Log Management
- [ ] Log files are being written successfully
- [ ] Log rotation is working (files not growing indefinitely)
- [ ] Logs are being aggregated (ELK stack, CloudWatch, etc.)
- [ ] Log retention policy is implemented
- [ ] Sensitive data is NOT logged (passwords, tokens, etc.)
- [ ] Error logs are being monitored and alerted

### Alerts
- [ ] Alert rules are configured for critical errors
- [ ] Disk space alerts are set up
- [ ] Memory usage alerts are configured
- [ ] CPU usage alerts are configured
- [ ] Database connection pool alerts are set up
- [ ] Service down alerts are configured
- [ ] Alert notification channels are tested (email, Slack, etc.)

## Performance

### Application Optimization
- [ ] Database queries are optimized (no N+1 queries)
- [ ] Database indexes are created on frequently queried columns
- [ ] Caching is implemented for frequently accessed data
- [ ] Connection pooling is optimized
- [ ] HTTP/2 is enabled
- [ ] Gzip compression is enabled
- [ ] Static assets are minified (if applicable)

### Load Testing
- [ ] Load testing has been performed
- [ ] Application handles expected concurrent users
- [ ] Database can handle expected query load
- [ ] No memory leaks detected under load
- [ ] Response times are acceptable under load

### Scalability
- [ ] Application is stateless (can scale horizontally)
- [ ] Session data is externalized (if needed)
- [ ] Service discovery is working properly
- [ ] Load balancer is configured (if using multiple instances)
- [ ] Auto-scaling rules are configured (if on cloud)

## Backup & Recovery

### Database Backup
- [ ] Automated database backups are configured
- [ ] Backup frequency is appropriate (daily, hourly, etc.)
- [ ] Backup retention policy is defined
- [ ] Database restore process has been tested
- [ ] Backup files are encrypted
- [ ] Backups are stored in separate location

### Application Backup
- [ ] Application code is in version control
- [ ] Configuration files are backed up
- [ ] Uploaded files/images are backed up
- [ ] Backup restore procedure is documented

### Disaster Recovery
- [ ] Disaster recovery plan is documented
- [ ] Recovery Time Objective (RTO) is defined
- [ ] Recovery Point Objective (RPO) is defined
- [ ] Disaster recovery has been tested
- [ ] Failover mechanism is in place (if applicable)

## Testing

### Functional Testing
- [ ] All API endpoints have been tested
- [ ] User registration flow is working (welcome email sent, `AUTH_WELCOME` notification fired)
- [ ] User login flow is working (`AUTH_NEW_DEVICE_LOGIN` notification fired)
- [ ] OTP sending and verification is working (registration OTP uses welcome email template)
- [ ] Password reset flow working (`AUTH_PASSWORD_RESET_SUCCESS` notification fired)
- [ ] Profile update notifications firing (`AUTH_PROFILE_UPDATED` in-app notification)
- [ ] Payment integration is working (with test transactions, `CONTRACT_PAYMENT_SUCCESS` notification fired)
- [ ] PDF generation is working
- [ ] Email sending is working (SMTP credentials valid)
- [ ] File upload/download is working
- [ ] Blockchain integration is working (if applicable)
- [ ] Kafka notification events appear in topics after business actions
- [ ] Notification-Service consumes events and saves to PostgreSQL
- [ ] WebSocket push delivered to connected UI clients in real time
- [ ] DLQ topic remains empty during normal operations

### Integration Testing
- [ ] Service-to-service communication is working
- [ ] Eureka registration is working for all services
- [ ] Database connectivity is working
- [ ] External API integrations are working (Twilio, Razorpay, etc.)

### Security Testing
- [ ] Penetration testing has been performed
- [ ] OWASP Top 10 vulnerabilities are checked
- [ ] SQL injection attempts are blocked
- [ ] XSS attempts are blocked
- [ ] Authentication bypass attempts fail
- [ ] Unauthorized access attempts are blocked

## Documentation

### Technical Documentation
- [ ] README.md is up to date
- [ ] API documentation is available
- [ ] Architecture diagram is documented
- [ ] Database schema is documented
- [ ] Environment variables are documented
- [ ] Deployment process is documented

### Operational Documentation
- [ ] Server access procedures are documented
- [ ] Deployment procedures are documented
- [ ] Rollback procedures are documented
- [ ] Troubleshooting guide is available
- [ ] Contact information for on-call support is documented

## Compliance & Legal

### Data Protection
- [ ] GDPR compliance is verified (if applicable)
- [ ] Data encryption at rest is enabled
- [ ] Data encryption in transit is enabled
- [ ] User data deletion process is implemented
- [ ] Privacy policy is available
- [ ] Terms of service are available

### Audit Trail
- [ ] User actions are logged (audit trail)
- [ ] Admin actions are logged
- [ ] Data access is logged
- [ ] Log tampering is prevented

## Post-Deployment

### Post-Deployment Smoke Testing
- [ ] All services are running
- [ ] All services are registered with Eureka
- [ ] Gateway health check returns UP (`/actuator/health`)
- [ ] All gateway routes are active (main, contract, agreement, market, notifications)
- [ ] Sample API requests through the gateway succeed (`/main/auth/login`, `/market/listings/all/active`)
- [ ] Notification REST API responds (`/notifications/api/notifications/unread-count`)
- [ ] WebSocket connection via Ws-Gateway succeeds (`ws://localhost:8081/notifications/ws`)
- [ ] User can register successfully and receives welcome email
- [ ] User can login successfully
- [ ] Critical features work end-to-end
- [ ] Circuit breaker fallback returns 503 JSON when a service is stopped (spot check)
- [ ] Kafka notification event appears in topic after login
- [ ] Notification-Service DLQ topic is empty

### Monitoring Setup
- [ ] Monitoring dashboards are set up
- [ ] Real-time metrics are visible
- [ ] Log aggregation is working
- [ ] Alerts are being received
- [ ] On-call rotation is established

### Documentation Updates
- [ ] Deployment date is documented
- [ ] Deployed version is documented
- [ ] Known issues are documented
- [ ] Contact information is updated

## Rollback Plan

### Preparation
- [ ] Previous stable version is identified
- [ ] Rollback procedure is documented
- [ ] Database rollback script is prepared (if schema changed)
- [ ] Rollback can be executed quickly

### Triggers
- [ ] Criteria for rollback are defined
- [ ] Decision makers are identified
- [ ] Communication plan for rollback is ready

## Communication

### Stakeholder Communication
- [ ] Stakeholders are notified of deployment window
- [ ] Expected downtime is communicated (if any)
- [ ] Success/failure is communicated post-deployment
- [ ] Known issues are communicated

### User Communication
- [ ] Users are notified of maintenance window (if applicable)
- [ ] New features are announced
- [ ] Breaking changes are communicated
- [ ] Support channels are ready

---

## Sign-Off

**Deployment Date:** ___________________

**Deployed By:** ___________________

**Reviewed By:** ___________________

**Approved By:** ___________________

**Notes:**
___________________________________________________________
___________________________________________________________
___________________________________________________________

---

**Remember:** 
- Run through this checklist completely before deployment
- Document any items that cannot be completed with justification
- Keep this checklist updated as requirements change
- Use this checklist for every production deployment
