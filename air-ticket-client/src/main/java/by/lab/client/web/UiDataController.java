package by.lab.client.web;

import by.lab.client.Dto;
import by.lab.client.RemoteApi;
import by.lab.client.RemoteApi.RemoteException;
import by.lab.client.RemoteApi.Resp;
import by.lab.client.model.UserToken;
import by.lab.client.service.UserTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Обращение клиента к прикладному сервису от имени пользователя.
 * Адрес сервиса и путь метода берутся из Регистра; к запросу присоединяется токен оплаты пользователя
 * (из БД клиента) в заголовке X-Payment-Token. Проверку токена и попадания текущего момента в оплаченный
 * интервал выполняет сам сервис; его ответ передаётся браузеру.
 */
@RestController
@RequestMapping("/ui/api/{method}")
public class UiDataController {

  private final RemoteApi remote;
  private final UserTokenService tokens;
  private final String serviceName;

  public UiDataController(RemoteApi remote, UserTokenService tokens, @Value("${client.service-name}") String serviceName) {
    this.remote = remote;
    this.tokens = tokens;
    this.serviceName = serviceName;
  }

  @ExceptionHandler(RemoteException.class)
  public ResponseEntity<String> remoteError(RemoteException e) {
    return json(502, "{\"message\":" + quote(e.getMessage()) + "}");
  }

  @GetMapping
  public ResponseEntity<String> list(@PathVariable String method, Authentication a) {
    return proxy(HttpMethod.GET, method, null, null, a);
  }

  @PostMapping
  public ResponseEntity<String> create(@PathVariable String method, @RequestBody String body, Authentication a) {
    return proxy(HttpMethod.POST, method, null, body, a);
  }

  @PutMapping("/{id}")
  public ResponseEntity<String> update(@PathVariable String method, @PathVariable Long id,
      @RequestBody String body, Authentication a) {
    return proxy(HttpMethod.PUT, method, id, body, a);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<String> delete(@PathVariable String method, @PathVariable Long id, Authentication a) {
    return proxy(HttpMethod.DELETE, method, id, null, a);
  }

  private ResponseEntity<String> proxy(HttpMethod http, String method, Long id, String body, Authentication a) {
    Dto.Service s = remote.service(serviceName);
    Dto.Method m = s.methods().stream().filter(x -> x.name().equalsIgnoreCase(method)).findFirst()
        .orElseThrow(() -> new RemoteException("Метод «" + method + "» не найден у сервиса " + serviceName));

    UserToken t = tokens.active(a.getName(), m.name()).orElse(null);
    if (t == null) {
      tokens.syncQuietly(a.getName()); // токен мог только что прийти
      t = tokens.active(a.getName(), m.name()).orElse(null);
    }
    if (t == null) {
      return json(402, "{\"message\":\"Услуга не оплачена\"}");
    }

    String url = s.baseUrl().replaceAll("/+$", "") + m.path() + (id == null ? "" : "/" + id);
    Resp r = remote.call(http, url, new Object[0], body, Map.of("X-Payment-Token", t.getToken()));
    if (r.status() == 0) {
      return json(502, "{\"message\":" + quote(r.message()) + "}");
    }
    if (r.status() == 401 || r.status() == 403) { // сервис не принял токен (истёк, чужой метод и т. д.)
      return json(402, "{\"message\":" + quote(r.message()) + "}");
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
