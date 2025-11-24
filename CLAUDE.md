# Token Validation Service

## Project Overview

The Token Validation Service is a dedicated microservice that validates JWTs (JSON Web Tokens) for the Budget Analyzer application. It implements the authentication layer for NGINX's `auth_request` directive, acting as a security gateway between the NGINX reverse proxy and backend microservices.

**Purpose**: Provide fast, reliable JWT validation to enable NGINX to authenticate requests before proxying them to backend services.

**Role**: Security microservice that decouples authentication from business logic, allowing backend services to trust pre-validated requests.

## Repository Scope

**Boundary**: This repository only.

**Allowed**:
- Read `../service-common/` and `../orchestration/docs/`
- All operations within this repository

**Forbidden**:
- Writing outside this repository

Cross-service changes: coordinate through orchestration or service-common.

## Architecture Principles

- **Single Responsibility**: Validates JWTs and returns 200 OK or 401 Unauthorized - nothing more
- **Stateless**: No session storage, no state management - pure validation service
- **Fast Validation**: Optimized for NGINX auth_request with minimal overhead
- **Security-First**: OAuth2 Resource Server pattern with Auth0 integration
- **Decoupled Authentication**: Backend services receive only validated requests

## Authentication Flow

```
Client Request
    ↓
NGINX Gateway (receives Authorization: Bearer <jwt>)
    ↓
NGINX calls /auth/validate (internal auth_request)
    ↓
Token Validation Service
    ├─ Validates JWT signature (Auth0 JWKS)
    ├─ Validates expiration
    ├─ Validates issuer
    └─ Validates audience
    ↓
200 OK → NGINX proxies to backend service
401 Unauthorized → NGINX rejects request
```

## Technology Stack

**Discovery**:
```bash
# Check versions
cat gradle/libs.versions.toml

# List all dependencies
cat build.gradle.kts | grep implementation

# Check Java version
./gradlew -version
```

**Stack**:
- **Java**: 24 (see gradle/libs.versions.toml)
- **Spring Boot**: 3.5.7 (see gradle/libs.versions.toml)
- **Spring Security**: OAuth2 Resource Server for JWT validation
- **Build System**: Gradle with wrapper (./gradlew)
- **Code Quality**: Spotless (Google Java Format), Checkstyle

**Key Dependencies**:
- `spring-boot-starter-web` - REST endpoints
- `spring-boot-starter-oauth2-resource-server` - JWT validation
- `spring-boot-starter-actuator` - Health checks

## API Endpoints

**Discovery**:
```bash
# Find all controllers
find src/main/java -name "*Controller.java"

# Search for request mappings
grep -r "@GetMapping\|@PostMapping" src/main/java

# View application port
grep "port:" src/main/resources/application.yml
```

**Endpoints**:

### `GET /auth/validate`
Primary validation endpoint called by NGINX.

**Request**:
```http
GET /auth/validate HTTP/1.1
Authorization: Bearer <jwt>
X-Original-URI: /api/transactions
```

**Response**:
- `200 OK` with `X-JWT-User-Id` header → JWT is valid
- `401 Unauthorized` → JWT is invalid, expired, or missing

**Implementation**: [src/main/java/org/budgetanalyzer/tokenvalidation/api/AuthValidationController.java](src/main/java/org/budgetanalyzer/tokenvalidation/api/AuthValidationController.java)

### `GET /actuator/health`
Health check endpoint for monitoring.

**Response**:
```json
{
  "status": "UP"
}
```

## Configuration

**Discovery**:
```bash
# View full configuration
cat src/main/resources/application.yml

# Check environment variables used
grep '\${' src/main/resources/application.yml
```

**Key Settings**:

| Variable | Description | Default | Where Used |
|----------|-------------|---------|------------|
| `AUTH0_ISSUER_URI` | Auth0 tenant issuer URI | `https://dev-gcz1r8453xzz0317.us.auth0.com/` | JWT issuer validation |
| `AUTH0_AUDIENCE` | Expected audience claim (API identifier) | `https://api.budgetanalyzer.org` | JWT audience validation |
| `SERVER_PORT` | Service port | `8088` | Server binding |

**Configuration Files**:
- [src/main/resources/application.yml](src/main/resources/application.yml) - Main configuration
- [gradle/libs.versions.toml](gradle/libs.versions.toml) - Dependency versions
- [build.gradle.kts](build.gradle.kts) - Build configuration

## JWT Validation Details

The service validates four key JWT aspects:

1. **Signature**: Verifies JWT signature using Auth0 public keys (JWKS endpoint)
2. **Expiration**: Ensures token is not expired (exp claim)
3. **Issuer**: Validates iss claim matches Auth0 tenant
4. **Audience**: Validates aud claim matches API identifier

**Implementation**: [src/main/java/org/budgetanalyzer/tokenvalidation/config/SecurityConfig.java](src/main/java/org/budgetanalyzer/tokenvalidation/config/SecurityConfig.java)

**Custom Validators**:
- [AudienceValidator.java](src/main/java/org/budgetanalyzer/tokenvalidation/config/AudienceValidator.java) - Validates audience claim
- Supports both `JWT` and `at+jwt` token types (OAuth 2.0 RFC 9068)
- Supports `RS256` and `PS256` algorithms (Auth0 standards)

## Code Structure

**Discovery**:
```bash
# View source structure
find src/main/java -type f -name "*.java" | sort

# Count lines of code
find src/main/java -name "*.java" -exec wc -l {} + | tail -1

# View test structure
find src/test/java -type f -name "*.java"
```

**Package Organization**:
```
src/main/java/org/budgetanalyzer/tokenvalidation/
├── TokenValidationServiceApplication.java  # Spring Boot main class
├── api/
│   └── AuthValidationController.java       # /auth/validate endpoint
└── config/
    ├── SecurityConfig.java                 # OAuth2 Resource Server setup
    └── AudienceValidator.java              # Custom audience validation
```

**Key Classes**:
- **AuthValidationController**: Main validation endpoint
- **SecurityConfig**: JWT decoder configuration, security filter chain
- **AudienceValidator**: Validates audience claim against expected value

**Shared Components from service-common**:
- **HttpLoggingFilter**: Logs incoming validation requests for debugging
- **DefaultApiExceptionHandler**: Handles exceptions and returns proper HTTP responses (404, 400, 500)

## Development Workflow

### Prerequisites
- Java 24+ (JDK installed)
- Docker (for running with orchestration)
- Auth0 tenant (or use default placeholders)

### Build and Test

**Format code:**
```bash
./gradlew clean spotlessApply
```

**Build and test:**
```bash
./gradlew clean build
```

The build includes:
- Spotless code formatting checks
- Checkstyle rule enforcement
- All unit and integration tests
- JAR file creation

**Run locally:**
```bash
./gradlew bootRun
```

**Health check:**
```bash
# Check service health
curl http://localhost:8088/actuator/health

# Expected: {"status":"UP"}
```

**Test JWT validation:**
```bash
# With valid JWT (requires real Auth0 token)
curl -H "Authorization: Bearer <valid-jwt>" http://localhost:8088/auth/validate

# Expected: 200 OK with X-JWT-User-Id header

# Without JWT
curl http://localhost:8088/auth/validate

# Expected: 401 Unauthorized
```

### Testing

**Discovery**:
```bash
# Find all tests
find src/test/java -name "*Test.java"

# Run tests with verbose output
./gradlew test --info
```

**Run specific tests**:
```bash
# Run specific test class
./gradlew test --tests TokenValidationServiceApplicationTests

# Generate test report
./gradlew test
# View report at: build/reports/tests/test/index.html
```

## Integration with Budget Analyzer

### Docker Compose

The service is defined in the orchestration repository's docker-compose.yml:

```yaml
token-validation-service:
  build: ../token-validation-service
  ports:
    - "8088:8088"
  environment:
    - AUTH0_ISSUER_URI=${AUTH0_ISSUER_URI}
    - AUTH0_AUDIENCE=${AUTH0_AUDIENCE}
```

**Discovery**:
```bash
# View orchestration setup (from orchestration repo)
cd ../orchestration
grep -A 10 "token-validation-service:" docker-compose.yml

# Start with all services
docker compose up -d

# Check logs
docker compose logs -f token-validation-service
```

### NGINX Integration

NGINX uses the `auth_request` directive to validate tokens before proxying requests:

```nginx
location /api/ {
    auth_request /internal/auth/validate;
    auth_request_set $jwt_user_id $upstream_http_x_jwt_user_id;
    proxy_set_header X-JWT-User-Id $jwt_user_id;
    proxy_pass http://backend-service;
}

location = /internal/auth/validate {
    internal;
    proxy_pass http://token-validation-service:8088/auth/validate;
    proxy_pass_request_body off;
    proxy_set_header Content-Length "";
    proxy_set_header Authorization $http_authorization;
    proxy_set_header X-Original-URI $request_uri;
}
```

**How it works**:
1. NGINX receives request with `Authorization: Bearer <jwt>`
2. NGINX calls `/internal/auth/validate` (internal, not exposed)
3. This proxies to `token-validation-service:8088/auth/validate`
4. Service validates JWT and returns 200 OK or 401 Unauthorized
5. If 200 OK, NGINX extracts `X-JWT-User-Id` header and forwards to backend
6. If 401, NGINX rejects the request immediately

**References**:
- NGINX configuration: `../orchestration/nginx/nginx.dev.conf`
- Authentication architecture: `../orchestration/docs/architecture/security-architecture.md`
- Implementation plan: `../orchestration/docs/architecture/authentication-implementation-plan.md`

## Debugging and Troubleshooting

### Logging

**View logs in development**:
```bash
# Gradle bootRun shows logs in console
./gradlew bootRun

# Docker Compose
cd ../orchestration
docker compose logs -f token-validation-service
```

**Log levels** (see [application.yml](src/main/resources/application.yml)):
- `org.budgetanalyzer`: `TRACE` (detailed service logs)
- `org.springframework.security`: `DEBUG` (Spring Security logs)
- Root: `WARN` (minimal noise)

**Key log patterns to look for**:
- `=== JWT Decoder Configuration ===` - Startup configuration
- `=== Token Validation Request ===` - Incoming validation requests
- `JWT VALID - User: ...` - Successful validation
- `=== Authentication Failed ===` - Failed validation with details

### Common Issues

**Issue**: 401 Unauthorized for valid tokens

**Diagnosis**:
```bash
# Check issuer URI configuration
grep issuer-uri src/main/resources/application.yml

# Check Auth0 audience
grep audience src/main/resources/application.yml

# View detailed validation logs
# (logs will show exact issuer/audience mismatch)
```

**Issue**: Cannot reach Auth0 JWKS endpoint

**Diagnosis**:
```bash
# Test connectivity to JWKS endpoint
curl https://<your-auth0-domain>/.well-known/jwks.json

# Check OIDC configuration
curl https://<your-auth0-domain>/.well-known/openid-configuration
```

**Issue**: Service not responding

**Diagnosis**:
```bash
# Check service is running
curl http://localhost:8088/actuator/health

# Check port binding
lsof -i :8088  # or: ss -tlnp | grep 8088

# View Gradle bootRun logs for startup errors
./gradlew bootRun
```

## Best Practices

### Security
1. **Never log full JWTs** - Only log token metadata (subject, expiration, etc.)
2. **Use environment variables** - Never hardcode Auth0 credentials
3. **Validate audience** - Always validate the audience claim to prevent token misuse
4. **Trust Auth0 signatures** - Let Auth0 JWKS handle key rotation automatically

### Performance
1. **Keep it lightweight** - No business logic in this service
2. **Cache JWKS keys** - Spring Security caches Auth0 public keys automatically
3. **Disable request body** - NGINX should use `proxy_pass_request_body off`
4. **Monitor latency** - JWT validation should be sub-100ms

### Development
1. **Follow build sequence** - Always run `./gradlew clean spotlessApply` then `./gradlew clean build`
2. **Treat checkstyle warnings as errors** - Fix all warnings before committing
3. **Follow naming conventions** - Controller → api/, Config → config/, Models → model/
4. **Test with real tokens** - Use Auth0 test tokens for integration testing
5. **Keep docs updated** - Update README.md and CLAUDE.md when adding features

### Testing
1. **Unit tests** - Test validators and config in isolation
2. **Integration tests** - Test with mock JWTs
3. **End-to-end tests** - Test NGINX → Token Service → Backend flow
4. **Load testing** - Ensure validation can handle expected request volume

## Repository Context

This service is part of the Budget Analyzer microservices architecture:

**Main Repository**: https://github.com/budgetanalyzer/token-validation-service

**Related Repositories**:
- **orchestration**: https://github.com/budgetanalyzer/orchestration (Docker Compose, NGINX config)
- **service-common**: https://github.com/budgetanalyzer/service-common (Shared build configuration)
- **session-gateway**: https://github.com/budgetanalyzer/session-gateway (Session management)

**Development Setup**: All repositories should be cloned side-by-side in `/workspace/` for cross-repo documentation links to work.

## Notes for Claude Code

**CRITICAL - Prerequisites First**: Before implementing any plan or feature:
1. Check for prerequisites in documentation (e.g., "Prerequisites: service-common Enhancement")
2. If prerequisites are NOT satisfied, STOP immediately and inform the user
3. Do NOT attempt to hack around missing prerequisites - this leads to broken implementations that must be deleted
4. Complete prerequisites first, then return to the original task

### Critical Rules

**Always run build commands in sequence:**
```bash
./gradlew clean spotlessApply
./gradlew clean build
```

**Fix Checkstyle warnings** - Treat warnings as errors requiring immediate resolution

### Service-Specific Reminders

When working on this service:
- This is a **security-critical service** - be extra careful with JWT validation logic
- Keep the service **lightweight and fast** - it's called on every authenticated request
- Follow the **OAuth2 Resource Server pattern** - don't reinvent JWT validation
- **Test with real Auth0 tokens** when making changes to validation logic
- All configuration should use **environment variables** for production deployability
- The service should **never store state** - purely stateless validation
- When adding features, ensure they don't add latency to the critical validation path
