package by.lab.client.web;

import by.lab.client.Dto;
import by.lab.client.RemoteApi;
import by.lab.client.RemoteApi.RemoteException;
import by.lab.client.model.UserToken;
import by.lab.client.service.UserTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Страницы клиента для авторизованного пользователя. Услуги берутся из Регистра, оплата — во внешнем сервисе. */
@Controller
public class UiController {

  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
  /** Методы, для которых в клиенте есть таблица данных, и их названия для пользователя. */
  private static final Map<String, String> TITLES = Map.of("Flights", "Каталог рейсов", "Bookings", "Бронирование билетов");
  private static final Map<String, String> DESCRIPTIONS = Map.of(
      "Flights", "Расписание рейсов: поиск, добавление, редактирование, свободные места.",
      "Bookings", "Оформление, изменение и отмена бронирований пассажиров.");

  public record Card(Dto.Item item, String code, String label) {
  }

  public record Row(String title, String period, String price, String code, String label) {
  }

  private final RemoteApi remote;
  private final UserTokenService tokens;
  private final String serviceName;
  private final String paymentPublicUrl;

  public UiController(RemoteApi remote, UserTokenService tokens, @Value("${client.service-name}") String serviceName,
      @Value("${payment.public-url}") String paymentPublicUrl) {
    this.remote = remote;
    this.tokens = tokens;
    this.serviceName = serviceName;
    this.paymentPublicUrl = paymentPublicUrl;
  }

  @ExceptionHandler(RemoteException.class)
  public String remoteError(RemoteException e, Model m) {
    m.addAttribute("message", e.getMessage());
    return "error";
  }

  /** Услуги = платные методы сервиса из Регистра. */
  private List<Dto.Item> items() {
    return remote.service(serviceName).methods().stream()
        .map(m -> new Dto.Item(m.name(), TITLES.getOrDefault(m.name(), m.name()),
            DESCRIPTIONS.getOrDefault(m.name(), m.description()), m.price() == null ? BigDecimal.ZERO : m.price()))
        .toList();
  }

  private Dto.Item item(String method) {
    return items().stream().filter(i -> i.method().equals(method)).findFirst().orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Услуга '" + method + "' не найдена у сервиса " + serviceName));
  }

  @GetMapping("/")
  public String catalog(Authentication auth, Model m) {
    List<Dto.Item> items = items();
    tokens.syncQuietly(auth.getName());
    m.addAttribute("cards", items.stream().map(i -> tokens.active(auth.getName(), i.method())
        .map(t -> new Card(i, "live", "Оплачено до " + t.getValidTo().format(FMT)))
        .orElseGet(() -> new Card(i, "none", "Не оплачено"))).toList());
    return "catalog";
  }

  /** Шаг «оплатить»: переход в отдельный сервис оплаты; страница сама ждёт, пока токен дойдёт до клиента. */
  @GetMapping("/pay/{method}")
  public String pay(@PathVariable String method, Authentication auth, Model m) {
    Dto.Item it = item(method);
    String user = auth.getName();
    tokens.syncQuietly(user);
    m.addAttribute("item", it);
    m.addAttribute("user", user);
    m.addAttribute("service", serviceName);
    m.addAttribute("paid", tokens.active(user, it.method()).isPresent());
    m.addAttribute("payUrl", UriComponentsBuilder.fromUriString(paymentPublicUrl)
        .queryParam("firmId", user).queryParam("serviceName", serviceName)
        .queryParam("methodName", it.method()).build().encode().toUriString());
    return "pay";
  }

  /** Опрос из браузера: получен ли токен оплаты текущего пользователя для услуги. */
  @GetMapping("/ui/status/{method}")
  @ResponseBody
  public Map<String, Object> status(@PathVariable String method, Authentication auth) {
    tokens.syncQuietly(auth.getName());
    return tokens.active(auth.getName(), method)
        .<Map<String, Object>>map(t -> Map.of("paid", true, "until", t.getValidTo().format(FMT)))
        .orElse(Map.of("paid", false));
  }

  @GetMapping("/data/{method}")
  public String data(@PathVariable String method, Authentication auth, Model m) {
    if (!TITLES.containsKey(method)) {
      throw new RemoteException("Для метода «" + method + "» в клиенте нет таблицы данных");
    }
    Dto.Item it = item(method);
    tokens.syncQuietly(auth.getName());
    if (tokens.active(auth.getName(), it.method()).isEmpty()) {
      return "redirect:/pay/" + it.method();
    }
    m.addAttribute("item", it);
    return "data";
  }

  @GetMapping("/my-payments")
  public String payments(Authentication auth, Model m) {
    tokens.syncQuietly(auth.getName());
    LocalDateTime now = LocalDateTime.now();
    List<Row> rows = tokens.history(auth.getName()).stream().map((UserToken t) -> {
      String code = now.isBefore(t.getValidFrom()) ? "wait" : now.isAfter(t.getValidTo()) ? "expired" : "live";
      String label = switch (code) {
        case "wait" -> "ещё не действует";
        case "expired" -> "истекла";
        default -> "действует";
      };
      return new Row(TITLES.getOrDefault(t.getMethodName(), t.getMethodName()),
          t.getValidFrom().format(FMT) + " — " + t.getValidTo().format(FMT),
          t.getPrice() == null ? "—" : String.format("%.2f ₽", t.getPrice()), code, label);
    }).toList();
    m.addAttribute("rows", rows);
    return "payments";
  }
}
