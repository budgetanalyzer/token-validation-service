Claude’s Plan
Replace RequestLoggingFilter with service-common HttpLoggingFilter
Summary
YES, replace with HttpLoggingFilter from service-common. It's more secure, better tested, and provides consistency across all Budget Analyzer services.
Comparison Highlights
Current: Shows Authorization: Bearer <token-length: 123 chars> (exposes metadata)
service-common: Shows Authorization: ******** (complete masking)
service-common: Masks ALL sensitive headers (Cookie, X-API-Key, etc.) not just Authorization
service-common: Uses Spring Boot autoconfiguration - zero component scanning needed
service-common: Highly configurable via application.yml
Implementation Plan
1. Add service-web version to gradle/libs.versions.toml
In [versions] section, add:
serviceCommon = "0.0.1-SNAPSHOT"
In [libraries] section, add:
# Service dependencies
service-web = { module = "org.budgetanalyzer:service-web", version.ref = "serviceCommon" }
2. Add service-web dependency to build.gradle.kts
In dependencies block, add as first dependency:
dependencies {
    // Service-web provides: web, data-jpa, springdoc, jackson, slf4j, opencsv, actuator
    // Plus: exception handling, HTTP logging, OpenAPI config
    implementation(libs.service.web)
    
    // Existing dependencies...
    implementation(libs.spring.boot.starter.web)  // Still needed for some web features
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.boot.starter.actuator)
    // ...
}
3. Configure HttpLoggingFilter in application.yml
Add at end of file:
budgetanalyzer:
  service:
    http-logging:
      enabled: true
      log-level: DEBUG
      include-request-body: false  # No request bodies for validation service
      include-response-body: false # No response bodies needed
      exclude-patterns:
        - /actuator/**  # Don't log health checks
4. Delete RequestLoggingFilter.java
Remove src/main/java/org/budgetanalyzer/tokenvalidation/config/RequestLoggingFilter.java No changes to TokenValidationServiceApplication.java needed - Spring Boot autoconfiguration handles everything via ServiceWebAutoConfiguration registered in META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
5. Publish service-common to Maven Local (first time setup)
Only needed if not already published:
cd /workspace/service-common
./gradlew clean spotlessApply build publishToMavenLocal
cd /workspace/token-validation-service
6. Build and test token-validation-service
./gradlew clean spotlessApply
./gradlew clean build
./gradlew bootRun
# Test with: curl -H "Authorization: Bearer test" http://localhost:8088/auth/validate
How Autoconfiguration Works
service-web JAR contains META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
This registers ServiceWebAutoConfiguration which scans org.budgetanalyzer.service.* packages
HttpLoggingConfig is discovered and conditionally creates HttpLoggingFilter bean when enabled
Zero manual component scanning needed - it "just works"
Benefits
✅ More secure: Complete sensitive header masking (not partial)
✅ Consistent: Same logging as currency-service and other services
✅ Maintainable: Shared code, centralized fixes
✅ Configurable: Adjust via application.yml without code changes
✅ Well-tested: Comprehensive test coverage in service-common
✅ Future-proof: Standard for all Budget Analyzer Spring Boot services