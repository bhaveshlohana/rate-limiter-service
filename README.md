# Distributed Rate Limiter as a Service

![CI](https://github.com/bhaveshlohana/rate-limiter-service/actions/workflows/ci.yml/badge.svg)
![Docker](https://img.shields.io/docker/pulls/bhaveshlohana/rate-limiter-service)

A production-grade, distributed rate limiting service built with Spring Boot and Redis. Supports three algorithms, dynamic per-client configuration, atomic Redis Lua scripts, and real-time observability via Prometheus and Grafana.

**Live Demo:** https://rate-limiter-service-y5d7.onrender.com/swagger-ui/index.html  
**Docker Hub:** https://hub.docker.com/repository/docker/bhaveshlohana/rate-limiter-service

---

## Overview

Rate limiting is a critical component of any production API — it protects services from abuse, ensures fair usage across clients, and prevents cascading failures under high load. This project implements rate limiting as a standalone service that any backend application can integrate with via REST API or as a Spring Boot Starter dependency.

**Key features:**
- Three rate limiting algorithms — Fixed Window, Sliding Window Log, Token Bucket
- Atomic Redis Lua scripts — eliminates race conditions under concurrent load
- Dynamic configuration — change limits per client type without restarting
- Default config fallback — unknown client types fall back to a DEFAULT policy
- Admin API — manage configs and inspect client state at runtime
- Real-time observability — Prometheus metrics + Grafana dashboards
- Plug and play — use as a REST service or embed via `@RateLimit` annotation (coming soon)

---

## Architecture
```
┌─────────────────────────────────────────────────────────┐
│                    Your Application                      │
│                                                         │
│   POST /api/rate-limiter/check                          │
│   { clientId, clientType }                              │
└─────────────────┬───────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────┐
│              Rate Limiter Service                       │
│                                                         │
│  RateLimiterController                                  │
│         │                                               │
│         ▼                                               │
│  RateLimiterFactory ──── ClientConfigService            │
│         │                       │                       │
│         ▼                       ▼                       │
│  ┌─────────────┐         ┌─────────────┐                │
│  │  Algorithm  │         │   Config    │                │
│  │  Selection  │         │   Lookup    │                │
│  └─────────────┘         └─────────────┘                │
│         │                                               │
│         ▼                                               │
│  ┌──────────────────────────────────┐                   │
│  │         Redis Lua Script         │                   │
│  │    (atomic read-check-write)     │                   │
│  └──────────────────────────────────┘                   │
└─────────────────┬───────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────┐
│                      Redis                              │
│                                                         │
│  ratelimit:config:PREMIUM    ← client configs           │
│  ratelimit:fixed:user123     ← algorithm state          │
│  ratelimit:token:user456     ← algorithm state          │
└─────────────────────────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────┐
│              Observability Stack                        │
│                                                         │
│  /actuator/prometheus ──► Prometheus ──► Grafana        │
└─────────────────────────────────────────────────────────┘
```

---

## Algorithms

### Comparison

| Algorithm | Memory | Accuracy | Burst Handling | Best For |
|---|---|---|---|---|
| Fixed Window | Low | Low (boundary burst) | Allows boundary burst | Simple APIs, low traffic |
| Sliding Window Log | High | High | Smooth, no bursts | Strict rate limiting |
| Token Bucket | Low | High | Controlled burst | Most production use cases |

### Fixed Window
Divides time into fixed buckets. Counts requests per bucket. Resets when the window expires.

**Redis structure:** `STRING` — integer counter with TTL  
**Known limitation:** Boundary burst — a client can make 2x requests at window boundaries

### Sliding Window Log
Stores a timestamp log of every request in a Sorted Set. On each request, evicts entries older than the window and counts what remains.

**Redis structure:** `ZSET` — members are UUIDs, scores are timestamps  
**Known limitation:** Memory heavy for high-traffic clients

### Token Bucket
Each client has a bucket that refills at a fixed rate. Each request consumes one token. Allows bursts up to bucket capacity while enforcing an average rate.

**Redis structure:** `HASH` — `tokens` and `lastRefillTime`  
**Best for:** Most real-world rate limiting scenarios

### Atomicity
All three algorithms use **Redis Lua scripts** for atomic execution. The read-check-write cycle executes as a single Redis operation, eliminating race conditions under concurrent load. Both naive (non-atomic) and atomic implementations are available for comparison.

---

## Getting Started

### Prerequisites
- Docker and Docker Compose
- Java 21 (for local development)

### Run with Docker Compose
```bash
git clone https://github.com/bhaveshlohana/rate-limiter-service
cd rate-limiter-service
docker compose up -d
```

This starts:
- Rate Limiter Service on `http://localhost:8080`
- Redis on `localhost:6379`
- Prometheus on `http://localhost:9090`
- Grafana on `http://localhost:3000` (admin/admin)

### Run with Docker
```bash
docker run -p 8080:8080 \
  -e SPRING_DATA_REDIS_HOST=host.docker.internal \
  -e SPRING_DATA_REDIS_PORT=6379 \
  bhaveshlohana/rate-limiter-service:latest
```

### Run locally
```bash
./mvnw spring-boot:run
```

Requires Redis running on `localhost:6379`.

---

## API Reference

### Rate Limit Check
```bash
POST /api/rate-limiter/check
```
```json
{
  "clientId": "user123",
  "clientType": "PREMIUM"
}
```

**Responses:**
- `200 OK` — request allowed
- `429 Too Many Requests` — rate limit exceeded
```json
{
  "allowed": true,
  "reason": "Request allowed",
  "remainingRequests": 47
}
```

### Admin — Set Config
```bash
POST /api/admin/config
```
```json
{
  "clientType": "PREMIUM",
  "algorithm": "TOKEN_BUCKET",
  "capacity": 500,
  "refillRatePerSecond": 10.0
}
```

### Admin — Get Config
```bash
GET /api/admin/config/{clientType}
```

### Admin — List All Configs
```bash
GET /api/admin/config
```

### Admin — Delete Config
```bash
DELETE /api/admin/config/{clientType}
```

### Admin — Client Status
```bash
GET /api/admin/status?clientId=user123&clientType=PREMIUM
```
```json
{
  "clientId": "user123",
  "clientType": "PREMIUM",
  "algorithm": "TOKEN_BUCKET",
  "currentTokens": 487.5,
  "remainingRequests": 487
}
```

---

## Configuration

Client configurations are stored dynamically in Redis. No restart required to update limits.

### Configuration Fields

| Field | Type | Required For | Description |
|---|---|---|---|
| `clientType` | String | All | Identifier for the client type |
| `algorithm` | Enum | All | `FIXED_WINDOW`, `SLIDING_WINDOW`, `TOKEN_BUCKET` |
| `limit` | Integer | Fixed/Sliding Window | Max requests per window |
| `windowSizeSeconds` | Integer | Fixed/Sliding Window | Window duration in seconds |
| `capacity` | Integer | Token Bucket | Max bucket size (burst limit) |
| `refillRatePerSecond` | Double | Token Bucket | Tokens added per second |

### Default Config

A `DEFAULT` config is seeded on startup and applies to any unknown client type:
```json
{
  "clientType": "DEFAULT",
  "algorithm": "FIXED_WINDOW",
  "limit": 10,
  "windowSizeSeconds": 60
}
```

### Example Configs
```bash
# Anonymous users — strict
curl -X POST http://localhost:8080/api/admin/config \
  -H "Content-Type: application/json" \
  -d '{
    "clientType": "ANONYMOUS",
    "algorithm": "FIXED_WINDOW",
    "limit": 10,
    "windowSizeSeconds": 60
  }'

# Registered users — moderate
curl -X POST http://localhost:8080/api/admin/config \
  -H "Content-Type: application/json" \
  -d '{
    "clientType": "REGISTERED",
    "algorithm": "SLIDING_WINDOW",
    "limit": 100,
    "windowSizeSeconds": 60
  }'

# Premium users — generous burst
curl -X POST http://localhost:8080/api/admin/config \
  -H "Content-Type: application/json" \
  -d '{
    "clientType": "PREMIUM",
    "algorithm": "TOKEN_BUCKET",
    "capacity": 500,
    "refillRatePerSecond": 10.0
  }'
```

---

## Plug and Play

### Mode 1 — REST Service

Any service can integrate by calling the `/check` endpoint before processing a request:
```java
// In your service
RestTemplate restTemplate = new RestTemplate();
RateLimitRequest request = new RateLimitRequest("user123", "PREMIUM");
ResponseEntity<RateLimitResponse> response = restTemplate.postForEntity(
    "http://rate-limiter-service/api/rate-limiter/check",
    request,
    RateLimitResponse.class
);

if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
    throw new RateLimitExceededException();
}
```

### Mode 2 — Spring Boot Starter

Add the dependency to your Spring Boot project:
```xml
<dependency>
    <groupId>com.bhavesh.lohana</groupId>
    <artifactId>rate-limiter-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

Annotate your endpoints:
```java
@RateLimit(clientType = "PREMIUM")
@GetMapping("/api/data")
public ResponseEntity<?> getData() {
    return ResponseEntity.ok(data);
}
```

---

## Observability

### Metrics

Metrics are exposed at `/actuator/prometheus` and scraped by Prometheus every 5 seconds.

| Metric | Labels | Description |
|---|---|---|
| `ratelimit_request_total` | `clientType`, `algorithm`, `result` | Total requests checked |

**Key queries:**
```promql
# Request rate per second
rate(ratelimit_request_total[1m])

# Rejection rate by client type
rate(ratelimit_request_total{result="rejected"}[1m])

# Allowed vs rejected
ratelimit_request_total
```

### Grafana Dashboard

![Grafana Dashboard](docs/images/grafana-dashboard.png)

Import the dashboard from `grafana/dashboard.json` or connect Grafana to your Prometheus instance.

---

## Design Decisions

**Why Redis for config storage?**  
Configs and rate limit state share the same Redis instance — no extra infrastructure. Config changes are reflected immediately without restarts.

**Why Lua scripts for atomicity?**  
Redis executes Lua scripts atomically — the entire read-check-write cycle runs as a single operation. This eliminates the race condition where two concurrent requests both read the same counter value and both get allowed when only one slot remains. Both naive and atomic implementations are provided for comparison.

**Why fail closed on missing config?**  
If a client type has no config and no DEFAULT exists, requests are rejected. A rate limiter is a security boundary — unknown clients should not get unlimited access by default.

**Why Token Bucket for most use cases?**  
Fixed Window allows boundary bursts. Sliding Window is memory-heavy at scale. Token Bucket provides accurate rate limiting with controlled burst support at O(1) memory per client.

---

## Known Limitations

- `KEYS *` used in `getAllConfigs()` — blocks Redis on large keyspaces. Production replacement: use `SCAN` for incremental iteration.
- No authentication on admin endpoints — add Spring Security before production use.
- Render free tier cold starts — app spins down after 15 minutes of inactivity, causing ~30s delay on first request.
- Single Redis instance — no Redis Cluster support. For high availability, configure Redis Sentinel or Cluster.

---

## Tech Stack

- **Java 21** + **Spring Boot 3.4**
- **Redis** — rate limit state and config storage
- **Lua Scripts** — atomic Redis operations
- **Prometheus** + **Grafana** — observability
- **Docker** + **Docker Compose** — containerization
- **GitHub Actions** — CI/CD
- **Render** — cloud deployment

---

## Running Tests
```bash
./mvnw test
```

Tests use embedded Redis — no external dependencies required.