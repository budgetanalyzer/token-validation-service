# Token Validation Service

[![Build](https://github.com/budgetanalyzer/token-validation-service/actions/workflows/build.yml/badge.svg)](https://github.com/budgetanalyzer/token-validation-service/actions/workflows/build.yml)

JWT validation service for NGINX `auth_request` directive.

## Overview

The Token Validation Service provides a lightweight, dedicated endpoint for validating JWTs. NGINX uses this service to validate tokens before proxying requests to backend microservices.

## Architecture

```
NGINX Gateway
  ├─ Receives request with Authorization: Bearer <jwt>
  ├─ Calls /auth/validate (auth_request)
  │  ├─ 200 OK → Forward to backend
  │  └─ 401 Unauthorized → Reject request
  └─ Proxies to backend service
```

## Technology Stack

- **Spring Boot**: Lightweight web application
- **Spring Security OAuth2 Resource Server**: JWT validation
- **Auth0**: Identity provider (public keys for signature validation)

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `AUTH0_ISSUER_URI` | Auth0 tenant issuer URI | `https://placeholder.auth0.com/` |
| `AUTH0_AUDIENCE` | Expected audience claim (API identifier) | `budget-analyzer-api` |

### Ports

- **8090**: Token Validation Service (internal, called by NGINX)

## JWT Validation

The service validates the following JWT claims:

1. **Signature**: Uses Auth0 public keys (JWKS)
2. **Expiration**: Ensures token is not expired
3. **Issuer**: Validates `iss` claim matches Auth0 tenant
4. **Audience**: Validates `aud` claim matches API identifier

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
    proxy_pass http://token-validation-service:8090/auth/validate;
    proxy_pass_request_body off;
    proxy_set_header Authorization $http_authorization;
}
```

## Running Locally

### Prerequisites

- Java 24
- Auth0 tenant configured (or use placeholders)

### Start the Service

```bash
./gradlew bootRun
```

### Health Check

```bash
curl http://localhost:8090/actuator/health
```

### Test JWT Validation

```bash
# With valid JWT
curl -H "Authorization: Bearer <valid-jwt>" http://localhost:8090/auth/validate

# Expected: 200 OK

# Without JWT
curl http://localhost:8090/auth/validate

# Expected: 401 Unauthorized
```

## Implementation Status

### Phase 1: Infrastructure Setup ✅
- [x] Basic Spring Boot setup
- [x] OAuth2 Resource Server configuration
- [x] JWT decoder with Auth0 issuer
- [x] `/auth/validate` endpoint
- [x] Audience validation
- [x] Health check endpoint

## Security Features

### JWT Validation
- **Signature Verification**: Uses Auth0 JWKS endpoint for public keys
- **Expiration Check**: Rejects expired tokens
- **Issuer Validation**: Ensures token from trusted Auth0 tenant
- **Audience Validation**: Ensures token intended for this API

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
