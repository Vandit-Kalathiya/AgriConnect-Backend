# AgriConnect Memory Monitoring Guide

## Overview
Complete monitoring setup with Prometheus alerts and Grafana dashboards to track memory usage across all services.

## Components

### 1. **cAdvisor** (Container Metrics)
- **Purpose**: Collects Docker container resource usage metrics
- **Access**: http://localhost:8081
- **Memory**: 256MB limit
- **Metrics Provided**:
  - Container memory usage
  - Container memory limits
  - CPU usage
  - Network I/O
  - Disk I/O

### 2. **Prometheus** (Metrics Storage & Alerts)
- **Purpose**: Time-series database and alerting engine
- **Access**: http://localhost:9090
- **Memory**: 768MB limit
- **Configuration**:
  - 7-day retention
  - 512MB storage limit
  - 15-second scrape interval
  - Alert evaluation every 30 seconds

### 3. **Grafana** (Visualization)
- **Purpose**: Dashboard and visualization platform
- **Access**: http://localhost:3000
- **Credentials**: admin / admin (change on first login)
- **Memory**: 384MB limit

## Alerts Configured

### Memory Alerts

#### 1. **HighContainerMemoryUsage** (Warning)
- **Trigger**: Container using >80% of memory limit
- **Duration**: 2 minutes
- **Severity**: Warning
- **Action**: Investigate which service is consuming memory

#### 2. **CriticalContainerMemoryUsage** (Critical)
- **Trigger**: Container using >90% of memory limit
- **Duration**: 1 minute
- **Severity**: Critical
- **Action**: Immediate intervention required - OOM kill imminent

#### 3. **HighJVMHeapUsage** (Warning)
- **Trigger**: JVM heap >80% full
- **Duration**: 2 minutes
- **Severity**: Warning
- **Action**: Check for memory leaks or increase heap size

#### 4. **ContainerRestartingFrequently** (Warning)
- **Trigger**: Container restarting multiple times in 5 minutes
- **Duration**: 5 minutes
- **Severity**: Warning
- **Action**: Check logs for OOM kills or application errors

### Service Health Alerts

#### 5. **ServiceDown** (Critical)
- **Trigger**: Spring Boot service unreachable
- **Duration**: 1 minute
- **Severity**: Critical
- **Action**: Check service logs and health

#### 6. **KafkaDown** (Critical)
- **Trigger**: Kafka broker unreachable
- **Duration**: 2 minutes
- **Severity**: Critical
- **Action**: Restart Kafka or check logs

## Grafana Dashboard

### "AgriConnect - Memory Monitoring" Dashboard

The dashboard includes:

#### **Panel 1: Container Memory Usage Timeline**
- Line graph showing memory usage % for all containers
- Color-coded thresholds:
  - Green: <80%
  - Yellow: 80-90%
  - Red: >90%
- Shows last value and max value

#### **Panel 2-7: Individual Service Gauges**
- Real-time gauge for each service:
  - API Gateway
  - Contract Farming
  - Market Access
  - Generate Agreement
  - Notification Service
  - Kafka
- Color changes based on usage percentage

#### **Panel 8: Absolute Memory Usage**
- Shows actual bytes used by each container
- Useful for comparing memory consumption

#### **Panel 9: JVM Heap Usage**
- Tracks JVM heap percentage for all Spring Boot services
- Helps identify heap pressure before container limits are hit

#### **Panel 10: Active Memory Alerts**
- Shows currently firing memory-related alerts
- Red background when alerts are active

## Access URLs

| Service | URL | Purpose |
|---------|-----|---------|
| cAdvisor | http://localhost:8081 | Container metrics UI |
| Prometheus | http://localhost:9090 | Query metrics & view alerts |
| Grafana | http://localhost:3000 | Dashboards |
| Kafka UI | http://localhost:8090 | Kafka monitoring |

## Using the Monitoring Stack

### 1. **View Alerts in Prometheus**

```bash
# Access Prometheus
http://localhost:9090/alerts

# View active alerts
http://localhost:9090/alerts?search=memory
```

### 2. **Query Metrics in Prometheus**

Useful queries:

```promql
# Container memory usage percentage
(container_memory_usage_bytes{name=~"agriconnect-.*"} / container_spec_memory_limit_bytes{name=~"agriconnect-.*"}) * 100

# JVM heap usage
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100

# Total memory used by all containers
sum(container_memory_usage_bytes{name=~"agriconnect-.*"})

# Services using >80% memory
(container_memory_usage_bytes{name=~"agriconnect-.*"} / container_spec_memory_limit_bytes{name=~"agriconnect-.*"}) * 100 > 80
```

### 3. **View Dashboard in Grafana**

1. Navigate to http://localhost:3000
2. Login with admin/admin
3. Go to **Dashboards** → **AgriConnect** → **AgriConnect - Memory Monitoring**
4. Dashboard auto-refreshes every 10 seconds

### 4. **Customize Dashboard**

- Click **⚙️ Dashboard Settings** → **Variables** to add filters
- Click **Add Panel** to create custom visualizations
- Click **💾 Save** to persist changes

## Troubleshooting

### Alert Not Firing

**Check Prometheus targets:**
```bash
# Access targets page
http://localhost:9090/targets

# All targets should show "UP"
```

**Verify alert rules:**
```bash
# Check if rules are loaded
http://localhost:9090/rules

# Look for "memory_alerts" group
```

### Dashboard Not Showing Data

**1. Check Prometheus datasource:**
- Go to Grafana → Configuration → Data Sources
- Click "Prometheus"
- Click "Test" - should show "Data source is working"

**2. Verify metrics are being collected:**
```bash
# Query in Prometheus
container_memory_usage_bytes{name=~"agriconnect-.*"}

# Should return data for all containers
```

### cAdvisor Not Collecting Metrics

**On Windows (Docker Desktop):**
cAdvisor may have limited functionality. If metrics aren't showing:

1. Check cAdvisor logs:
```bash
docker logs agriconnect-cadvisor
```

2. Alternative: Use Docker stats API directly
```bash
docker stats --no-stream --format "json"
```

## Memory Optimization Workflow

### When Alert Fires

1. **Check Grafana Dashboard**
   - Identify which service is high
   - Check if it's a spike or sustained increase

2. **View Container Stats**
   ```bash
   docker stats agriconnect-<service-name>
   ```

3. **Check Service Logs**
   ```bash
   docker logs agriconnect-<service-name> --tail 100
   ```

4. **Check for Memory Leaks**
   - Look for steadily increasing memory
   - Check JVM heap usage trend
   - Review recent code changes

5. **Take Action**
   - **Temporary**: Restart the service
     ```bash
     docker restart agriconnect-<service-name>
     ```
   - **Permanent**: Adjust memory limits or optimize code

### Preventive Monitoring

**Daily:**
- Check Grafana dashboard for trends
- Review any alerts that fired

**Weekly:**
- Analyze memory usage patterns
- Identify services that consistently run high
- Review and adjust limits if needed

**Monthly:**
- Export Prometheus data for analysis
- Review alert thresholds
- Update documentation

## Alert Notification Setup (Optional)

To receive alerts via email/Slack:

### 1. **Configure Alertmanager**

Create `observability/alertmanager.yml`:
```yaml
global:
  resolve_timeout: 5m

route:
  receiver: 'email-notifications'
  group_by: ['alertname', 'severity']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 12h

receivers:
  - name: 'email-notifications'
    email_configs:
      - to: 'your-email@example.com'
        from: 'alerts@agriconnect.com'
        smarthost: 'smtp.gmail.com:587'
        auth_username: 'your-email@gmail.com'
        auth_password: 'your-app-password'
```

### 2. **Add Alertmanager to docker-compose.yml**

```yaml
alertmanager:
  image: prom/alertmanager:latest
  container_name: agriconnect-alertmanager
  ports:
    - "9093:9093"
  volumes:
    - ./observability/alertmanager.yml:/etc/alertmanager/alertmanager.yml
  deploy:
    resources:
      limits:
        memory: 128M
  networks:
    - agriconnect
```

### 3. **Update Prometheus config**

Add to `prometheus.yml`:
```yaml
alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']
```

## Metrics Retention

**Current Settings:**
- **Prometheus**: 7 days, 512MB max
- **Grafana**: Persistent (uses Prometheus as source)

**To increase retention:**

Edit `docker-compose.yml` Prometheus command:
```yaml
- "--storage.tsdb.retention.time=30d"  # Change from 7d
- "--storage.tsdb.retention.size=2GB"  # Change from 512MB
```

**Note**: Longer retention requires more memory and disk space.

## Performance Impact

The monitoring stack adds:
- **Memory**: ~1.4GB (cAdvisor: 256MB, Prometheus: 768MB, Grafana: 384MB)
- **CPU**: Minimal (<5% on average)
- **Disk**: ~512MB for Prometheus data
- **Network**: Negligible (internal scraping only)

## Best Practices

1. **Set up alerts before problems occur** ✅ Done
2. **Review dashboards regularly** - Check daily
3. **Tune alert thresholds** - Adjust based on your traffic patterns
4. **Document incidents** - Keep notes on what caused alerts
5. **Test alerts** - Manually trigger to verify notifications work
6. **Keep retention reasonable** - 7 days is usually sufficient
7. **Monitor the monitors** - Ensure Prometheus/Grafana stay healthy

## Quick Reference Commands

```bash
# View all alerts
curl http://localhost:9090/api/v1/alerts | jq

# Query current memory usage
curl 'http://localhost:9090/api/v1/query?query=container_memory_usage_bytes' | jq

# Reload Prometheus config (after changes)
curl -X POST http://localhost:9090/-/reload

# Export Grafana dashboard
curl -u admin:admin http://localhost:3000/api/dashboards/uid/agriconnect-memory

# Check cAdvisor metrics
curl http://localhost:8081/metrics | grep container_memory
```

## Summary

You now have a complete monitoring solution:

✅ **Real-time metrics** from all containers  
✅ **Automated alerts** at 80% and 90% thresholds  
✅ **Visual dashboards** for easy monitoring  
✅ **Historical data** for trend analysis  
✅ **JVM-specific metrics** for Spring Boot services  

The system will automatically alert you before memory issues cause problems, giving you time to take corrective action.
