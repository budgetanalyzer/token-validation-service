package org.budgetanalyzer.tokenvalidation.config;

import java.util.List;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Custom JWT validator to verify the audience claim.
 *
 * <p>Ensures that the JWT was intended for this API by validating the 'aud' claim matches one of
 * the expected audience values.
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

  private final List<String> audiences;

  public AudienceValidator(List<String> audiences) {
    this.audiences = audiences;
  }

  @Override
  public OAuth2TokenValidatorResult validate(Jwt jwt) {
    var tokenAudiences = jwt.getAudience();

    // Check if any of the expected audiences match the JWT's audience claim
    if (tokenAudiences != null && tokenAudiences.stream().anyMatch(audiences::contains)) {
      return OAuth2TokenValidatorResult.success();
    }

    OAuth2Error error = new OAuth2Error("invalid_token", "Required audience not found", null);
    return OAuth2TokenValidatorResult.failure(error);
  }
}
