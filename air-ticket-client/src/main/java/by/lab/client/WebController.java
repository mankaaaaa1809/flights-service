package by.lab.client;

import by.lab.client.ClientState.TokenInfo;
import by.lab.client.RemoteApi.RemoteException;
import by.lab.client.RemoteApi.Resp;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

/** Страницы веб-приложения (HTML рендерится на сервере, Thymeleaf). */
@Controller
public class WebController {

  /** Методы, для которых в клиенте есть таблица данных. */
  static final Map<String, String> TITLES = Map.of("Flights", "Рейсы", "Bookings", "Бронирования");

  private final RemoteApi remote;
  private final ClientState state;
  private final String registryUrl;
  private final String paymentUrl;

  public WebController(RemoteApi remote, ClientState state,
      @Value("${registry.url}") String registryUrl, @Value("${payment.url}") String paymentUrl) {
    this.remote = remote;
    this.state = state;
    this.registryUrl = registryUrl;
    this.paymentUrl = paymentUrl;
  }

  @ExceptionHandler(RemoteException.class)
  public String remoteError(RemoteException e, Model m) {
    m.addAttribute("message", e.getMessage());
    return "error";
  }

  @GetMapping("/")
  public String home() {
    return "redirect:/service/" + UriUtils.encodePathSegment(state.getService(), StandardCharsets.UTF_8);
  }

  @GetMapping("/service/{name}")
  public String service(@PathVariable String name, Model m) {
    m.addAttribute("svc", remote.service(name));
    m.addAttribute("mine", name.equalsIgnoreCase(state.getService()));
    m.addAttribute("known", TITLES.keySet());
    return "service";
  }

  @GetMapping("/registry")
  public String registry(@RequestParam(defaultValue = "") String q, Model m) {
    m.addAttribute("list", remote.services(q));
    m.addAttribute("q", q);
    return "registry";
  }

  @GetMapping("/pay")
  public String payForm(@RequestParam(required = false) String service,
      @RequestParam(required = false) String method, Model m) {
    String name = service == null || service.isBlank() ? state.getService() : service;
    LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
    m.addAttribute("svc", remote.service(name));
    m.addAttribute("selected", method);
    m.addAttribute("firm", state.getFirm());
    m.addAttribute("now", now.toString());
    m.addAttribute("later", now.plusDays(1).toString());
    return "pay";
  }

  @PostMapping("/pay")
  public String pay(@RequestParam String service, @RequestParam String method, @RequestParam String firm,
      @RequestParam String dateFrom, @RequestParam String dateTo, RedirectAttributes ra) {
    ra.addAttribute("service", service);
    ra.addAttribute("method", method);
    LocalDateTime from;
    LocalDateTime to;
    try {
      from = LocalDateTime.parse(dateFrom);
      to = LocalDateTime.parse(dateTo);
    } catch (DateTimeParseException e) {
      ra.addFlashAttribute("err", "Некорректная дата");
      return "redirect:/pay";
    }
    if (!to.isAfter(from)) {
      ra.addFlashAttribute("err", "Конец периода должен быть позже начала");
      return "redirect:/pay";
    }
    state.setFirm(firm.trim());

    Resp r = remote.pay(firm.trim(), service, method, from, to);
    JsonNode j = r.json();
    String token = j != null && j.hasNonNull("token") ? j.get("token").asText() : null;
    Double price = j != null && j.hasNonNull("price") ? j.get("price").asDouble() : null;
    if (token != null) {
      state.putToken(service, method, new TokenInfo(token, from, to, price));
    }
    if (r.ok() && j != null && j.path("success").asBoolean(false)) {
      ra.addFlashAttribute("ok", "Оплата принята: " + service + " / " + method
          + ", сумма " + (price == null ? 0 : price) + ". Токен сохранён на сервере клиента.");
    } else {
      ra.addFlashAttribute("err", r.message() + (token != null ? " Токен сохранён." : ""));
    }
    return "redirect:/pay";
  }

  @GetMapping("/data/{service}/{method}")
  public String data(@PathVariable String service, @PathVariable String method, Model m) {
    Dto.Service s = remote.service(service);
    if (!TITLES.containsKey(method) || s.methods().stream().noneMatch(x -> x.name().equals(method))) {
      throw new RemoteException("Для метода «" + method + "» нет таблицы данных или метод не найден у сервиса");
    }
    m.addAttribute("svc", s);
    m.addAttribute("method", method);
    m.addAttribute("title", TITLES.get(method));
    m.addAttribute("st", state.status(service, method));
    return "data";
  }

  @GetMapping("/settings")
  public String settings(Model m) {
    m.addAttribute("firm", state.getFirm());
    m.addAttribute("service", state.getService());
    m.addAttribute("registryUrl", registryUrl);
    m.addAttribute("paymentUrl", paymentUrl);
    return "settings";
  }

  @PostMapping("/settings")
  public String saveSettings(@RequestParam String firm, @RequestParam String service, RedirectAttributes ra) {
    state.setSettings(firm.trim(), service.trim());
    ra.addFlashAttribute("ok", "Сохранено");
    return "redirect:/settings";
  }
}
