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
import java.util.List;
import java.util.Map;

/** Все обращения клиента к другим приложениям: Регистр и прикладной сервис (оплата вызывается пользователем в браузере). */
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

  public RemoteApi(RestClient.Builder builder, ObjectMapper mapper, @Value("${registry.url}") String registry) {
    SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
    rf.setConnectTimeout(5000);
    rf.setReadTimeout(30000); // на Render сервис может просыпаться после простоя
    this.http = builder.requestFactory(rf).build();
    this.mapper = mapper;
    this.registry = registry.replaceAll("/+$", "");
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
            // не JSON — остаётся только raw
          }
        }
        return new Resp(res.getStatusCode().value(), json, raw);
      });
    } catch (Exception e) {
      return new Resp(0, null, "Нет связи с сервисом (" + uriTemplate + "): " + e.getMessage());
    }
  }

  // ---------- Регистр ----------

  /** Описание сервиса из Регистра: адрес (baseUrl), методы, их пути и цены. */
  public Dto.Service service(String name) {
    Resp r = call(HttpMethod.GET, registry + "/api/services/{n}", new Object[] {name}, null, Map.of());
    if (r.status() == 404) {
      throw new RemoteException("Сервис «" + name + "» не найден в реестре. Он запущен и зарегистрирован?");
    }
    if (!r.ok() || r.json() == null) {
      throw new RemoteException(r.status() == 0 ? r.raw() : "Реестр: " + r.message());
    }
    return mapper.convertValue(r.json(), Dto.Service.class);
  }

  // ---------- Прикладной сервис ----------

  /** Токены оплаты фирмы (скрытый метод сервиса, доступ по ключу клиента). */
  public List<Dto.FirmToken> firmTokens(String baseUrl, String firm, String key) {
    Resp r = call(HttpMethod.GET, baseUrl.replaceAll("/+$", "") + "/internal/firm-tokens?firmId={f}",
        new Object[] {firm}, null, Map.of("X-Client-Key", key));
    if (!r.ok() || r.json() == null) {
      throw new RemoteException(r.status() == 0 ? r.raw() : "Сервис: " + r.message());
    }
    return mapper.convertValue(r.json(), new TypeReference<List<Dto.FirmToken>>() {
    });
  }
}
