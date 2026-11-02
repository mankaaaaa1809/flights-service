package by.lab.flights.security;

import by.lab.flights.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

/** Пропускает запросы к платным методам только с действующим токеном (заголовок X-Payment-Token). */
@Component
public class PaymentTokenInterceptor implements HandlerInterceptor {

  private final TokenService tokens;

  public PaymentTokenInterceptor(TokenService tokens) {
    this.tokens = tokens;
  }

  @Override
  public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
    if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
      return true; // CORS preflight
    }
    String path = req.getRequestURI();
    String method;
    if (path.equals("/api/flights") || path.startsWith("/api/flights/")) {
      method = TokenService.FLIGHTS;
    } else if (path.equals("/api/bookings") || path.startsWith("/api/bookings/")) {
      method = TokenService.BOOKINGS;
    } else {
      return true;
    }
    String token = req.getHeader("X-Payment-Token");
    if (token == null || token.isBlank()) {
      String auth = req.getHeader("Authorization");
      if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
        token = auth.substring(7).trim();
      }
    }
    if (token == null || token.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
          "Нужен заголовок X-Payment-Token. Оплатите метод '" + method + "' в сервисе оплаты");
    }
    tokens.check(token.trim(), method);
    return true;
  }
}
