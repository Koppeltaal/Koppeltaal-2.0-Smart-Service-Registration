package nl.koppeltaal.smartserviceregistration.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoggedInInterceptor implements HandlerInterceptor {

  private final String authorizationEndpointToken;
  private final String deactivationEndpointToken;

  public LoggedInInterceptor(@Value("${authorizations.endpoint.token:}") String authorizationEndpointToken,
                             @Value("${deactivation.endpoint.token:}") String deactivationEndpointToken) {
    this.authorizationEndpointToken = authorizationEndpointToken;
    this.deactivationEndpointToken = deactivationEndpointToken;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

    final String path = request.getServletPath();
    if("/login".equals(path) || "/code_response".equals(path) || "/.well-known/jwks.json".equals(path) || "/error".equals(path)) {
      return HandlerInterceptor.super.preHandle(request, response, handler);

    // accessed by applications, using a "secret" to approve the request
    } else if(path.startsWith("/authorization")) {
      final String authToken = request.getHeader("X-Auth-Token");

      if(!StringUtils.equals(authorizationEndpointToken, authToken)) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
      } else {
        return HandlerInterceptor.super.preHandle(request, response, handler);
      }

    // accessed by the FHIR server; without a configured secret nobody gets in
    } else if ("/deactivations".equals(path)) {
      if (!matchesConfiguredToken(deactivationEndpointToken, request.getHeader("X-Deactivation-Token"))) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
      }
      return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    final HttpSession session = request.getSession();
    final Object user = session.getAttribute("user");

    if(user == null) {
      response.sendRedirect("/login");
      return false;
    }

    return HandlerInterceptor.super.preHandle(request, response, handler);
  }

  private static boolean matchesConfiguredToken(String configured, String presented) {
    return StringUtils.isNotBlank(configured) && presented != null
        && MessageDigest.isEqual(configured.getBytes(StandardCharsets.UTF_8), presented.getBytes(StandardCharsets.UTF_8));
  }
}
