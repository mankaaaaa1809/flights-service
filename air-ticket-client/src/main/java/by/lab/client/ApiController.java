package by.lab.client;

import by.lab.client.ClientState.TokenInfo;
import by.lab.client.RemoteApi.RemoteException;
import by.lab.client.RemoteApi.Resp;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Прокси к прикладному сервису для таблицы в браузере. Адрес сервиса и путь метода берутся из Регистра,
 * токен оплаты подставляется на сервере — браузер его не видит.
 */
@RestController
@RequestMapping("/api/data/{service}/{method}")
public class ApiController {

  private final RemoteApi remote;
  private final ClientState state;

  public ApiController(RemoteApi remote, ClientState state) {
    this.remote = remote;
    this.state = state;
  }

  @ExceptionHandler(RemoteException.class)
  public ResponseEntity<String> remoteError(RemoteException e) {
    return json(502, "{\"message\":" + quote(e.getMessage()) + "}");
  }

  @GetMapping
  public ResponseEntity<String> list(@PathVariable String service, @PathVariable String method) {
    return proxy(HttpMethod.GET, service, method, null, null);
  }

  @PostMapping
  public ResponseEntity<String> create(@PathVariable String service, @PathVariable String method,
      @RequestBody String body) {
    return proxy(HttpMethod.POST, service, method, null, body);
  }

  @PutMapping("/{id}")
  public ResponseEntity<String> update(@PathVariable String service, @PathVariable String method,
      @PathVariable Long id, @RequestBody String body) {
    return proxy(HttpMethod.PUT, service, method, id, body);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<String> delete(@PathVariable String service, @PathVariable String method,
      @PathVariable Long id) {
    return proxy(HttpMethod.DELETE, service, method, id, null);
  }

  private ResponseEntity<String> proxy(HttpMethod http, String service, String method, Long id, String body) {
    Dto.Service s = remote.service(service);
    Dto.Method m = s.methods().stream().filter(x -> x.name().equals(method)).findFirst()
        .orElseThrow(() -> new RemoteException("Метод «" + method + "» не найден у сервиса " + service));
    TokenInfo t = state.token(service, method);
    if (t == null) {
      return json(401, "{\"message\":\"Метод не оплачен\"}");
    }
    String url = s.baseUrl().replaceAll("/+$", "") + m.path() + (id == null ? "" : "/" + id);
    Resp r = remote.call(http, url, new Object[0], body, Map.of("X-Payment-Token", t.token()));
    if (r.status() == 0) {
      return json(502, "{\"message\":" + quote(r.message()) + "}");
    }
    return json(r.status(), r.raw());
  }

  private static ResponseEntity<String> json(int status, String body) {
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body == null ? "" : body);
  }

  private static String quote(String s) {
    return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\"";
  }
}
