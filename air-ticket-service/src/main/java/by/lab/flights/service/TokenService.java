package by.lab.flights.service;

import by.lab.flights.dto.Dtos.TokenNotification;
import by.lab.flights.model.PaymentToken;
import by.lab.flights.repo.PaymentTokenRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

/** Приём токенов от сервиса оплаты и проверка доступа к платным методам. */
@Service
public class TokenService {

  /** Платные методы этого сервиса (имена - как в регистре). */
  public static final String FLIGHTS = "Flights";
  public static final String BOOKINGS = "Bookings";
  private static final Map<String, String> CANONICAL =
      Map.of("flights", FLIGHTS, "bookings", BOOKINGS);

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PaymentInfo(String firmId, String serviceName, String methodName, LocalDateTime validFrom,
      LocalDateTime validTo, Double price) {
  }

  private final PaymentTokenRepository tokens;
  private final RestClient http;
  private final String paymentUrl;
  private final boolean verify;
  private final String serviceName;

  public TokenService(PaymentTokenRepository tokens, RestClient.Builder builder,
      @Value("${payment.url}") String paymentUrl,
      @Value("${payment.verify}") boolean verify,
      @Value("${service.name}") String serviceName) {
    SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
    rf.setConnectTimeout(5000);
    rf.setReadTimeout(5000);
    this.tokens = tokens;
    this.http = builder.requestFactory(rf).build();
    this.paymentUrl = paymentUrl.replaceAll("/+$", "");
    this.verify = verify;
    this.serviceName = serviceName;
  }

  /** Вызывается сервисом оплаты после оплаты. */
  public void store(TokenNotification n) {
    String method = CANONICAL.get(n.methodName().trim().toLowerCase());
    if (method == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Метод '" + n.methodName() + "' не является платным методом сервиса " + serviceName);
    }
    if (n.validFrom().isAfter(n.validTo())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "validFrom позже validTo");
    }
    PaymentToken t = new PaymentToken();
    t.setToken(n.token().trim());
    t.setMethodName(method);
    t.setValidFrom(n.validFrom());
    t.setValidTo(n.validTo());
    t.setVerified(false);
    tokens.save(t);
    System.out.println(">>> [FLIGHTS] Получен токен для метода " + method
        + " (" + n.validFrom() + " .. " + n.validTo() + ")");
  }

  /** Бросает ResponseStatusException (401/403/503), если доступ не разрешён. */
  public void check(String token, String requiredMethod) {
    PaymentToken t = tokens.findById(token).orElseThrow(() ->
        new ResponseStatusException(HttpStatus.UNAUTHORIZED,
            "Токен неизвестен. Оплатите метод '" + requiredMethod + "' в сервисе оплаты"));

    if (!t.getMethodName().equalsIgnoreCase(requiredMethod)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "Токен оплачен для метода '" + t.getMethodName() + "', а нужен '" + requiredMethod + "'");
    }
    if (verify && !t.isVerified()) {
      verifyWithPayment(t);
    }
    LocalDateTime now = LocalDateTime.now();
    if (now.isBefore(t.getValidFrom()) || now.isAfter(t.getValidTo())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "Текущий момент вне оплаченного периода (" + t.getValidFrom() + " .. " + t.getValidTo() + ")");
    }
  }

  private void verifyWithPayment(PaymentToken t) {
    PaymentInfo info;
    try {
      info = http.get().uri(paymentUrl + "/api/payment/check?token={t}", t.getToken())
          .retrieve().body(PaymentInfo.class);
    } catch (HttpClientErrorException.NotFound e) {
      tokens.delete(t);
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Токен не найден в сервисе оплаты");
    } catch (RestClientException e) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Не удалось проверить токен в сервисе оплаты: " + e.getMessage());
    }
    if (info == null || !serviceName.equalsIgnoreCase(info.serviceName())
        || !t.getMethodName().equalsIgnoreCase(info.methodName())) {
      tokens.delete(t);
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Токен оплачен не для этого сервиса/метода");
    }
    apply(t, info);
  }

  /** Источник истины об оплате - сервис оплаты: интервал, фирма и цена берутся из его ответа. */
  private void apply(PaymentToken t, PaymentInfo info) {
    t.setValidFrom(info.validFrom());
    t.setValidTo(info.validTo());
    t.setFirmId(info.firmId() == null ? null : info.firmId().trim());
    t.setPrice(info.price());
    t.setVerified(true);
    tokens.save(t);
  }

  /**
   * Для пользовательского интерфейса: пытается подтвердить токен и узнать фирму (пользователя), не бросая исключений.
   * Сервис оплаты шлёт уведомление до завершения своей транзакции, поэтому «не найден» в первые минуты
   * не считается ошибкой, токен не удаляется.
   *
   * @return true, если токен подтверждён и привязан к фирме
   */
  public boolean tryResolve(PaymentToken t) {
    if (t.isVerified() && t.getFirmId() != null) {
      return true;
    }
    PaymentInfo info;
    try {
      info = http.get().uri(paymentUrl + "/api/payment/check?token={t}", t.getToken())
          .retrieve().body(PaymentInfo.class);
    } catch (HttpClientErrorException.NotFound e) {
      if (t.getReceivedAt() != null && t.getReceivedAt().isBefore(LocalDateTime.now().minusMinutes(10))) {
        tokens.delete(t);
      }
      return false;
    } catch (RestClientException e) {
      return false;
    }
    if (info == null || !serviceName.equalsIgnoreCase(info.serviceName())
        || !t.getMethodName().equalsIgnoreCase(info.methodName())) {
      tokens.delete(t);
      return false;
    }
    apply(t, info);
    return true;
  }
}
