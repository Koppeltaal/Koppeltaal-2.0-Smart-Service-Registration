package nl.koppeltaal.smartserviceregistration.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoggedInInterceptor implements HandlerInterceptor {

  private final String authorizationEndpointToken;

  public LoggedInInterceptor(@Value("${authorizations.endpoint.token:}") String authorizationEndpointToken) {
    this.authorizationEndpointToken = authorizationEndpointToken;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

    final String path = request.getServletPath();
    if("/login".equals(path) || "/code_response".equals(path)) {
      return HandlerInterceptor.super.preHandle(request, response, handler);

    // accessed by applications, using a "secret" to approve the request
    } else if("/authorization".equals(path)) {
      final String authToken = request.getHeader("X-Auth-Token");

      if(!StringUtils.equals(authorizationEndpointToken, authToken)) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      }
    }

    final HttpSession session = request.getSession();
    final Object user = session.getAttribute("user");

    if(user == null) {
      response.sendRedirect("/login");
      return false;
    }

    return HandlerInterceptor.super.preHandle(request, response, handler);
  }
}
