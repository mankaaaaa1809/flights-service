package by.lab.flights.api;

import by.lab.flights.dto.Dtos.TokenNotification;
import by.lab.flights.model.PaymentToken;
import by.lab.flights.repo.PaymentTokenRepository;
import by.lab.flights.service.TokenResolver;
import by.lab.flights.service.TokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Скрытые служебные методы (в регистре помечены paid=false, клиентам не отдаются). */
@RestController
@RequestMapping("/internal")
public class InternalController {

  public record FirmToken(String token, String methodName, LocalDateTime validFrom, LocalDateTime validTo,
      Double price) {
  }

  private final TokenService tokens;
  private final TokenResolver resolver;
  private final PaymentTokenRepository repo;
  private final String clientKey;

  public InternalController(TokenService tokens, TokenResolver resolver, PaymentTokenRepository repo,
      @Value("${service.client-key}") String clientKey) {
    this.tokens = tokens;
    this.resolver = resolver;
    this.repo = repo;
    this.clientKey = clientKey;
  }

  /** Сюда сервис оплаты присылает токен: POST {baseUrl}/internal/payment-token. */
  @PostMapping("/payment-token")
  public Map<String, String> paymentToken(@Valid @RequestBody TokenNotification n) {
    tokens.store(n);
    return Map.of("status", "accepted");
  }

  /**
   * Клиент забирает токены своего пользователя (firmId = логин), оплаченные в сервисе оплаты,
   * и дальше присоединяет их к запросам платных методов. Доступ — по общему ключу клиента.
   */
  @GetMapping("/firm-tokens")
  public List<FirmToken> firmTokens(@RequestParam String firmId,
      @RequestHeader(value = "X-Client-Key", required = false) String key) {
    if (key == null || !MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8),
        clientKey.getBytes(StandardCharsets.UTF_8))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Неверный X-Client-Key");
    }
    resolver.resolveNow();
    return repo.findByFirmIdIgnoreCaseOrderByValidToDesc(firmId.trim()).stream()
        .filter(PaymentToken::isVerified)
        .map(t -> new FirmToken(t.getToken(), t.getMethodName(), t.getValidFrom(), t.getValidTo(), t.getPrice()))
        .toList();
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
