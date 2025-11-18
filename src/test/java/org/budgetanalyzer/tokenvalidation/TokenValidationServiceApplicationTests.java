package org.budgetanalyzer.tokenvalidation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(
    properties = {"AUTH0_ISSUER_URI=https://test.auth0.com", "AUTH0_AUDIENCE=budget-analyzer-api"})
class TokenValidationServiceApplicationTests {

  @Test
  void contextLoads() {
    // This test verifies that the Spring application context loads successfully
  }
}
