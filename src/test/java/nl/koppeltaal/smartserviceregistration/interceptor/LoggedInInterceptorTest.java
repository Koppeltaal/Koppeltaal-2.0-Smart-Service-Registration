package nl.koppeltaal.smartserviceregistration.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LoggedInInterceptorTest {

  private static MockHttpServletRequest deactivationRequest(String token) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/deactivations");
    request.setServletPath("/deactivations");
    if (token != null) {
      request.addHeader("X-Deactivation-Token", token);
    }
    return request;
  }

  private static void assertRefused(LoggedInInterceptor interceptor, MockHttpServletRequest request) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
    assertThat(response.getStatus()).isEqualTo(401);
  }

  @Test
  void letsTheFhirServerInWithTheDeactivationToken() throws Exception {
    LoggedInInterceptor interceptor = new LoggedInInterceptor("", "deactivation-secret");

    assertThat(interceptor.preHandle(deactivationRequest("deactivation-secret"), new MockHttpServletResponse(), new Object())).isTrue();
  }

  @Test
  void refusesAWrongOrMissingDeactivationToken() throws Exception {
    LoggedInInterceptor interceptor = new LoggedInInterceptor("", "deactivation-secret");

    assertRefused(interceptor, deactivationRequest("wrong"));
    assertRefused(interceptor, deactivationRequest(""));
    assertRefused(interceptor, deactivationRequest(null));
  }

  @Test
  void refusesEveryCallWithoutAConfiguredDeactivationToken() throws Exception {
    LoggedInInterceptor interceptor = new LoggedInInterceptor("", "");

    assertRefused(interceptor, deactivationRequest(""));
    assertRefused(interceptor, deactivationRequest(null));
  }

  @Test
  void theAuthorizationTokenDoesNotOpenTheDeactivationEndpoint() throws Exception {
    MockHttpServletRequest request = deactivationRequest(null);
    request.addHeader("X-Auth-Token", "authorization-secret");

    assertRefused(new LoggedInInterceptor("authorization-secret", "deactivation-secret"), request);
  }
}
