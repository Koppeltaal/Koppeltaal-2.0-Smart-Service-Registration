package nl.koppeltaal.smartserviceregistration.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

public class LoggedInInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

    final String path = request.getServletPath();
    if("/login".equals(path) || "/code_response".equals(path)) {
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
}
