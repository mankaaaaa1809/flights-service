package by.lab.flights.api;

import by.lab.flights.dto.Dtos.TokenNotification;
import by.lab.flights.service.TokenService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/** Скрытые служебные методы (в регистре помечены paid=false, клиентам не отдаются). */
@RestController
@RequestMapping("/internal")
public class InternalController {

  private final TokenService tokens;

  public InternalController(TokenService tokens) {
    this.tokens = tokens;
  }

  /** Сюда сервис оплаты присылает токен: POST {baseUrl}/internal/payment-token. */
  @PostMapping("/payment-token")
  public Map<String, String> paymentToken(@Valid @RequestBody TokenNotification n) {
    tokens.store(n);
    return Map.of("status", "accepted");
  }

  @GetMapping("/health")
  public Map<String, String> health() {
    return Map.of("status", "ok", "service", "AirTicketService");
  }

  /** Время сервера - клиент берёт его за основу при выборе оплачиваемого периода. */
  @GetMapping("/server-time")
  public Map<String, String> serverTime() {
    return Map.of("now", LocalDateTime.now().toString());
  }
}
