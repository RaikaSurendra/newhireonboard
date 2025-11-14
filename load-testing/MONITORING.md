# Monitoring Guide for Load & Chaos Testing

## Overview

This guide covers monitoring strategies during load and chaos testing to identify bottlenecks and issues.

## Key Metrics to Monitor

### Application Metrics

#### Response Time
- **p50 (Median)**: 50% of requests complete within this time
- **p95**: 95% of requests complete within this time
- **p99**: 99% of requests complete within this time
- **Max**: Slowest request

**Targets:**
- p50 < 200ms
- p95 < 500ms
- p99 < 1000ms
- Max < 5000ms

#### Throughput
- **Requests per second (RPS)**: Number of requests handled per second
- **Concurrent users**: Number of simultaneous users

**Targets:**
- RPS > 100 for normal load
- Support 1000+ concurrent users

#### Error Rate
- **4xx errors**: Client errors (bad requests, auth failures)
- **5xx errors**: Server errors (crashes, timeouts)

**Targets:**
- Total error rate < 0.1%
- 5xx errors < 0.01%

### System Metrics

#### CPU Usage
```bash
# Monitor CPU usage
top -l 1 | grep "CPU usage"

# Per process
ps aux | grep java | awk '{print $3}'
```

**Targets:**
- Average CPU < 70%
- Peak CPU < 90%

#### Memory Usage
```bash
# System memory
vm_stat

# Java heap usage
jstat -gc <pid> 1000

# Check for memory leaks
jmap -heap <pid>
```

**Targets:**
- Heap usage < 80%
- No memory leaks (stable over time)
- GC pause time < 100ms

#### Database Metrics
```sql
-- Active connections
SHOW STATUS LIKE 'Threads_connected';

-- Max connections
SHOW VARIABLES LIKE 'max_connections';

-- Slow queries
SHOW FULL PROCESSLIST;

-- Query cache
SHOW STATUS LIKE 'Qcache%';
```

**Targets:**
- Connection pool utilization < 80%
- Query execution time < 100ms
- No connection leaks

### Network Metrics
```bash
# Active connections
netstat -an | grep 8080 | wc -l

# Connection states
netstat -an | grep 8080 | awk '{print $6}' | sort | uniq -c

# Network throughput
nettop -P -L 1
```

## Monitoring Tools

### 1. Real-time Monitoring

#### Application Logs
```bash
# Tail application logs
tail -f backend/logs/application.log

# Filter errors
tail -f backend/logs/application.log | grep ERROR

# Watch specific patterns
tail -f backend/logs/application.log | grep -E "timeout|exception|error"
```

#### System Monitoring
```bash
# CPU, Memory, Processes
htop

# Disk I/O
iostat -x 1

# Network
iftop
```

### 2. JVM Monitoring

#### JConsole
```bash
# Start JConsole
jconsole <pid>
```

Monitor:
- Heap memory usage
- Thread count
- CPU usage
- Garbage collection

#### VisualVM
```bash
# Start VisualVM
jvisualvm
```

Features:
- CPU profiling
- Memory profiling
- Thread analysis
- Heap dump analysis

#### JVM Flags for Monitoring
```bash
java -jar app.jar \
  -XX:+PrintGCDetails \
  -XX:+PrintGCDateStamps \
  -Xloggc:gc.log \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/tmp/heapdump.hprof
```

### 3. Database Monitoring

#### MySQL Workbench
- Connection monitoring
- Query performance
- Server status

#### Command Line
```bash
# Watch queries in real-time
mysql -u root -p -e "SHOW FULL PROCESSLIST" --table

# Performance schema
mysql -u root -p performance_schema

# Slow query log
tail -f /var/log/mysql/slow-query.log
```

### 4. Load Test Dashboards

#### Gatling Reports
- Automatically generated HTML reports
- Location: `backend/target/gatling/*/index.html`
- Includes: response time distribution, requests per second, errors

#### Artillery Reports
```bash
# Generate HTML report
artillery report results/artillery/api-test.json

# Real-time dashboard (requires plugin)
artillery run --output results.json scenarios/api-load-test.yml
```

#### k6 Output
```bash
# JSON output
k6 run --out json=results.json scripts/api-load-test.js

# InfluxDB + Grafana (advanced)
k6 run --out influxdb=http://localhost:8086/k6 scripts/api-load-test.js
```

## Monitoring Checklist

### Before Tests
- [ ] Application is running and healthy
- [ ] Database is accessible
- [ ] Monitoring tools are ready
- [ ] Baseline metrics recorded
- [ ] Disk space available for logs

### During Tests
- [ ] Watch response times
- [ ] Monitor error rates
- [ ] Check CPU/Memory usage
- [ ] Observe database connections
- [ ] Watch for exceptions in logs
- [ ] Monitor GC activity

### After Tests
- [ ] Review test reports
- [ ] Analyze error logs
- [ ] Check for memory leaks
- [ ] Review slow queries
- [ ] Compare with baseline
- [ ] Document findings

## Common Issues and Solutions

### High Response Times

**Symptoms:**
- p95/p99 response times increasing
- Requests timing out

**Investigation:**
```bash
# Check slow queries
mysql -u root -p -e "SHOW FULL PROCESSLIST"

# Check thread dumps
jstack <pid> > thread-dump.txt

# Check CPU usage
top -p <pid>
```

**Solutions:**
- Add database indexes
- Optimize queries
- Increase connection pool
- Add caching
- Scale horizontally

### Memory Leaks

**Symptoms:**
- Heap usage continuously increasing
- OutOfMemoryError
- Frequent GC pauses

**Investigation:**
```bash
# Heap dump
jmap -dump:live,format=b,file=heap.bin <pid>

# Analyze with Eclipse MAT or VisualVM

# Check for connection leaks
netstat -an | grep CLOSE_WAIT
```

**Solutions:**
- Close resources properly (try-with-resources)
- Fix connection leaks
- Review object lifecycle
- Tune GC parameters

### Database Bottlenecks

**Symptoms:**
- High database CPU
- Slow query execution
- Connection pool exhausted

**Investigation:**
```sql
-- Slow queries
SELECT * FROM information_schema.processlist 
WHERE time > 5;

-- Table locks
SHOW OPEN TABLES WHERE In_use > 0;

-- Index usage
SHOW INDEX FROM table_name;
```

**Solutions:**
- Add missing indexes
- Optimize queries (EXPLAIN)
- Increase connection pool
- Consider read replicas
- Implement caching

### High Error Rates

**Symptoms:**
- 5xx errors increasing
- Timeout errors
- Connection refused

**Investigation:**
```bash
# Check error logs
grep -i error backend/logs/application.log

# Check connection limits
ulimit -n

# Check port availability
netstat -an | grep 8080
```

**Solutions:**
- Increase timeout values
- Add retry logic
- Implement circuit breakers
- Scale resources
- Fix application bugs

## Alerting Thresholds

### Critical Alerts
- Error rate > 5%
- p99 response time > 5000ms
- CPU usage > 95%
- Memory usage > 95%
- Database connections > 95% of pool

### Warning Alerts
- Error rate > 1%
- p99 response time > 2000ms
- CPU usage > 80%
- Memory usage > 80%
- Database connections > 80% of pool

## Performance Optimization Tips

### Application Level
1. **Connection Pooling**: Use HikariCP with optimal settings
2. **Caching**: Implement Redis/Memcached for frequent queries
3. **Async Processing**: Use async for long-running tasks
4. **Batch Operations**: Batch database inserts/updates
5. **Lazy Loading**: Load data only when needed

### Database Level
1. **Indexes**: Add indexes on frequently queried columns
2. **Query Optimization**: Use EXPLAIN to optimize queries
3. **Connection Pooling**: Tune pool size based on load
4. **Partitioning**: Partition large tables
5. **Read Replicas**: Distribute read load

### Infrastructure Level
1. **Horizontal Scaling**: Add more application instances
2. **Load Balancing**: Distribute traffic evenly
3. **CDN**: Cache static assets
4. **Database Scaling**: Vertical scaling or sharding
5. **Monitoring**: Set up APM tools (New Relic, DataDog)

## Continuous Monitoring

### Production Monitoring Setup

1. **APM Tools**
   - New Relic
   - DataDog
   - Dynatrace
   - AppDynamics

2. **Logging**
   - ELK Stack (Elasticsearch, Logstash, Kibana)
   - Splunk
   - CloudWatch Logs

3. **Metrics**
   - Prometheus + Grafana
   - CloudWatch Metrics
   - StatsD

4. **Alerting**
   - PagerDuty
   - Opsgenie
   - Slack notifications

### Metrics to Track in Production
- Request rate
- Error rate
- Response time (p50, p95, p99)
- Apdex score
- CPU/Memory usage
- Database query time
- Cache hit rate
- Queue depth
- Active users

## Resources

- [Gatling Metrics Documentation](https://gatling.io/docs/current/general/reports/)
- [k6 Metrics Guide](https://k6.io/docs/using-k6/metrics/)
- [MySQL Performance Schema](https://dev.mysql.com/doc/refman/8.0/en/performance-schema.html)
- [JVM Monitoring Guide](https://docs.oracle.com/javase/8/docs/technotes/guides/management/)
