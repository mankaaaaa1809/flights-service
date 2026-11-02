package by.lab.flights;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/** Однократная регистрация сервиса в регистре при старте (с повторами, если регистр ещё не поднялся). */
@Component
public class ServiceRegistrar implements ApplicationRunner {

  private final RestClient http;
  private final String registryUrl;
  private final String name;
  private final String baseUrl;
  private final boolean reregister;
  private final String adminKey;

  public ServiceRegistrar(RestClient.Builder builder,
      @Value("${registry.url}") String registryUrl,
      @Value("${service.name}") String name,
      @Value("${service.base-url}") String baseUrl,
      @Value("${registry.reregister}") boolean reregister,
      @Value("${registry.admin-key}") String adminKey) {
    this.http = builder.build();
    this.registryUrl = registryUrl.replaceAll("/+$", "");
    this.name = name;
    this.baseUrl = baseUrl;
    this.reregister = reregister;
    this.adminKey = adminKey;
  }

  @Override
  public void run(ApplicationArguments args) {
    Thread t = new Thread(this::registerWithRetry, "registry-registrar");
    t.setDaemon(true);
    t.start();
  }

  private void registerWithRetry() {
    for (int attempt = 1; attempt <= 12; attempt++) {
      try {
        tryRegister();
        return;
      } catch (RestClientException e) {
        System.out.println(">>> [FLIGHTS] Регистр недоступен (попытка " + attempt + "/12): " + e.getMessage());
        try { Thread.sleep(5000); } catch (InterruptedException ie) { return; }
      }
    }
    System.out.println(">>> [FLIGHTS] Регистрация не удалась. Проверьте registry.url");
  }

  /** @return true, если зарегистрирован сейчас. */
  private boolean tryRegister() {
    try {
      post();
      System.out.println(">>> [FLIGHTS] Зарегистрирован в регистре как " + name);
      return true;
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode() != HttpStatus.CONFLICT) {
        System.out.println(">>> [FLIGHTS] Регистр отклонил регистрацию: " + e.getResponseBodyAsString());
        return false;
      }
      if (!reregister) {
        System.out.println(">>> [FLIGHTS] Уже зарегистрирован в регистре, пропускаю");
        return false;
      }
      http.delete().uri(registryUrl + "/api/services/" + name)
          .header("X-Admin-Key", adminKey).retrieve().toBodilessEntity();
      post();
      System.out.println(">>> [FLIGHTS] Перерегистрирован в регистре как " + name);
      return true;
    }
  }

  private void post() {
    http.post().uri(registryUrl + "/api/services")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body())
        .retrieve().toBodilessEntity();
  }

  private Map<String, Object> body() {
    return Map.of(
        "name", name,
        "baseUrl", baseUrl,
        "description", "Удалённый заказ авиабилетов: каталог рейсов и бронирование",
        "methods", List.of(
            Map.of(
                "name", "Flights",
                "httpMethod", "GET",
                "path", "/api/flights",
                "description", "Каталог рейсов. Оплата даёт доступ ко всем операциям над рейсами: "
                    + "GET /api/flights (фильтры origin, destination, date=yyyy-MM-dd), GET /api/flights/{id}, "
                    + "POST /api/flights, PUT /api/flights/{id}, DELETE /api/flights/{id}",
                "paid", true,
                "price", 10.00,
                "parameters", List.of(
                    param("X-Payment-Token", "string", "header", true),
                    param("origin", "string", "query", false),
                    param("destination", "string", "query", false),
                    param("date", "string", "query", false))),
            Map.of(
                "name", "Bookings",
                "httpMethod", "GET",
                "path", "/api/bookings",
                "description", "Бронирование билетов. Оплата даёт доступ ко всем операциям над бронями: "
                    + "GET /api/bookings (фильтр flightId), GET /api/bookings/{id}, POST /api/bookings, "
                    + "PUT /api/bookings/{id}, DELETE /api/bookings/{id}",
                "paid", true,
                "price", 5.00,
                "parameters", List.of(
                    param("X-Payment-Token", "string", "header", true),
                    param("flightId", "integer", "query", false))),
            Map.of(
                "name", "PaymentToken",
                "httpMethod", "POST",
                "path", "/internal/payment-token",
                "description", "Служебный: приём токена от сервиса оплаты",
                "paid", false,
                "parameters", List.of(
                    param("token", "string", "body", true),
                    param("methodName", "string", "body", true),
                    param("validFrom", "string", "body", true),
                    param("validTo", "string", "body", true))),
            Map.of(
                "name", "Health",
                "httpMethod", "GET",
                "path", "/internal/health",
                "description", "Служебный: проверка работоспособности",
                "paid", false,
                "parameters", List.of()),
            Map.of(
                "name", "ServerTime",
                "httpMethod", "GET",
                "path", "/internal/server-time",
                "description", "Служебный: время сервера",
                "paid", false,
                "parameters", List.of())));
  }

  private static Map<String, Object> param(String name, String type, String location, boolean required) {
    return Map.of("name", name, "type", type, "location", location, "required", required);
  }
}
