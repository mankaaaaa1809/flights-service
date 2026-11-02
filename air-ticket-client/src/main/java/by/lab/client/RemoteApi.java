package by.lab.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Все обращения клиента к другим сервисам: Регистр, сервис оплаты, прикладной сервис. */
@Service
public class RemoteApi {

  public record Resp(int status, JsonNode json, String raw) {
    public boolean ok() {
      return status >= 200 && status < 300;
    }

    public String message() {
      if (status == 0) {
        return raw;
      }
      if (json != null) {
        if (json.hasNonNull("message")) {
          return json.get("message").asText();
        }
        if (json.hasNonNull("error")) {
          return json.get("error").asText();
        }
      }
      return "HTTP " + status;
    }
  }

  public static class RemoteException extends RuntimeException {
    public RemoteException(String message) {
      super(message);
    }
  }

  private final RestClient http;
  private final ObjectMapper mapper;
  private final String registry;
  private final String payment;

  public RemoteApi(RestClient.Builder builder, ObjectMapper mapper,
      @Value("${registry.url}") String registry, @Value("${payment.url}") String payment) {
    SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
    rf.setConnectTimeout(5000);
    rf.setReadTimeout(15000);
    this.http = builder.requestFactory(rf).build();
    this.mapper = mapper;
    this.registry = registry.replaceAll("/+$", "");
    this.payment = payment.replaceAll("/+$", "");
  }

  /** Универсальный вызов; сетевые ошибки превращает в Resp со status = 0. */
  public Resp call(HttpMethod method, String uriTemplate, Object[] vars, String body, Map<String, String> headers) {
    try {
      RestClient.RequestBodySpec spec = http.method(method).uri(uriTemplate, vars).headers(h -> {
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.forEach(h::set);
      });
      if (body != null) {
        spec.body(body);
      }
      return spec.exchange((req, res) -> {
        String raw = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonNode json = null;
        if (!raw.isBlank()) {
          try {
            json = mapper.readTree(raw);
          } catch (Exception ignored) {
            // не JSON — оставляем только raw
          }
        }
        return new Resp(res.getStatusCode().value(), json, raw);
      });
    } catch (Exception e) {
      return new Resp(0, null, "Нет связи с сервисом (" + uriTemplate + "): " + e.getMessage());
    }
  }

  // ---------- Регистр ----------

  public Dto.Service service(String name) {
    Resp r = call(HttpMethod.GET, registry + "/api/services/{n}", new Object[] {name}, null, Map.of());
    if (r.status() == 404) {
      throw new RemoteException("Сервис «" + name + "» не найден в реестре. Он запущен и зарегистрирован?");
    }
    check(r, "Реестр");
    return mapper.convertValue(r.json(), Dto.Service.class);
  }

  public List<Dto.Service> services(String q) {
    Resp r = q == null || q.isBlank()
        ? call(HttpMethod.GET, registry + "/api/services", new Object[0], null, Map.of())
        : call(HttpMethod.GET, registry + "/api/services/search?q={q}", new Object[] {q}, null, Map.of());
    check(r, "Реестр");
    return mapper.convertValue(r.json(), new TypeReference<List<Dto.Service>>() {
    });
  }

  // ---------- Оплата ----------

  public Resp pay(String firm, String service, String method, LocalDateTime from, LocalDateTime to) {
    DateTimeFormatter iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    Map<String, Object> body = Map.of("firmId", firm, "serviceName", service, "methodName", method,
        "dateFrom", from.format(iso), "dateTo", to.format(iso));
    return call(HttpMethod.POST, payment + "/api/payment/pay", new Object[0],
        mapper.valueToTree(body).toString(), Map.of());
  }

  private void check(Resp r, String who) {
    if (!r.ok() || r.json() == null) {
      throw new RemoteException(r.status() == 0 ? r.raw() : who + ": " + r.message());
    }
  }
}
