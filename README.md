# Token Validation Service

> "Archetype: service. Role: Validates JWTs for NGINX auth_request; security gateway for backend services."
>
> — [AGENTS.md](AGENTS.md#tree-position)

[![Build](https://github.com/budgetanalyzer/token-validation-service/actions/workflows/build.yml/badge.svg)](https://github.com/budgetanalyzer/token-validation-service/actions/workflows/build.yml)

JWT validation service for NGINX `auth_request` directive.

## Overview

The Token Validation Service provides a lightweight, dedicated endpoint for validating JWTs. NGINX uses this service to validate tokens before proxying requests to backend microservices.

## Architecture

```
Browser
  ├─ Authenticates via session-gateway (Auth0 OAuth2 login)
  ├─ Receives internal JWT minted by session-gateway
  ├─ Sends request with Authorization: Bearer <internal-jwt>
  ↓
NGINX Gateway
  ├─ Calls /auth/validate (auth_request)
  │  ├─ Token Validation Service verifies RS256 signature (session-gateway JWKS)
  │  ├─ 200 OK → Forward to backend with X-JWT-User-Id header
  │  └─ 401 Unauthorized → Reject request
  └─ Proxies to backend service
```

## Technology Stack

- **Spring Boot**: Lightweight web application
- **Spring Security OAuth2 Resource Server**: JWT validation
- **session-gateway**: Internal JWT issuer (JWKS for RS256 verification)

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `JWT_JWKS_URI` | JWKS endpoint for verifying internal JWTs | `http://session-gateway:8081/.well-known/jwks.json` |

### Ports

- **8088**: Token Validation Service (internal, called by NGINX)

## JWT Validation

The service validates internal JWTs minted by session-gateway:

1. **Signature**: Verifies RS256 signature using session-gateway's JWKS endpoint
2. **Expiration**: Ensures token is not expired

## API Endpoints

### `GET /auth/validate`

Validates JWT in Authorization header.

**Request:**
```http
GET /auth/validate HTTP/1.1
Authorization: Bearer <jwt>
```

**Response:**
- `200 OK`: JWT is valid
- `401 Unauthorized`: JWT is invalid, expired, or missing

**Usage by NGINX:**
```nginx
location /api/ {
    auth_request /internal/auth/validate;
    proxy_pass http://backend-service;
}

location = /internal/auth/validate {
    internal;
    proxy_pass http://token-validation-service:8088/auth/validate;
    proxy_pass_request_body off;
    proxy_set_header Authorization $http_authorization;
}
```

## Running Locally

### Prerequisites

- Java 24
- session-gateway accessible (or override `JWT_JWKS_URI`)

### Start the Service

```bash
./gradlew bootRun
```

### Health Check

```bash
curl http://localhost:8088/actuator/health
```

### Test JWT Validation

```bash
# With valid JWT
curl -H "Authorization: Bearer <valid-jwt>" http://localhost:8090/auth/validate

# Expected: 200 OK

# Without JWT
curl http://localhost:8088/auth/validate

# Expected: 401 Unauthorized
```

## Implementation Status

### Phase 1: Infrastructure Setup ✅
- [x] Basic Spring Boot setup
- [x] OAuth2 Resource Server configuration
- [x] JWT decoder with session-gateway JWKS
- [x] `/auth/validate` endpoint
- [x] Health check endpoint

## Security Features

### JWT Validation
- **Signature Verification**: Verifies RS256 signature using session-gateway's JWKS endpoint
- **Expiration Check**: Rejects expired tokens

### Performance
- Lightweight endpoint optimized for NGINX auth_request
- No request body processing (proxy_pass_request_body off)
- Fast JWT validation using cached public keys

## Development

### Build

```bash
./gradlew build
```

### Run Tests

```bash
./gradlew test
```

### Code Formatting

```bash
./gradlew clean spotlessApply
```

## References

- [Authentication Implementation Plan](../orchestration/docs/architecture/authentication-implementation-plan.md)
- [Security Architecture](../orchestration/docs/architecture/security-architecture.md)
- [NGINX auth_request Module](http://nginx.org/en/docs/http/ngx_http_auth_request_module.html)
