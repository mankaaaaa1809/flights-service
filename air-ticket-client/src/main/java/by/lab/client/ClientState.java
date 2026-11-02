package by.lab.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/** Состояние приложения на сервере: фирма, имя сервиса и токены оплаты (файл JSON). */
@Component
public class ClientState {

  public record TokenInfo(String token, LocalDateTime from, LocalDateTime to, Double price) {
  }

  /** code: none / wait / live / expired (используется как CSS-класс). */
  public record Status(String code, String label, String period) {
  }

  public static class Data {
    public String firm;
    public String service;
    public Map<String, TokenInfo> tokens = new LinkedHashMap<>();
  }

  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

  private final ObjectMapper mapper;
  private final Path file;
  private Data d = new Data();

  public ClientState(ObjectMapper mapper, @Value("${client.firm}") String firm,
      @Value("${client.service-name}") String service, @Value("${client.data-file}") String file) {
    this.mapper = mapper;
    this.file = Path.of(file).toAbsolutePath();
    try {
      if (Files.exists(this.file)) {
        d = mapper.readValue(this.file.toFile(), Data.class);
      }
    } catch (Exception e) {
      System.out.println(">>> [CLIENT] Не удалось прочитать " + this.file + ": " + e.getMessage());
    }
    if (d.firm == null || d.firm.isBlank()) {
      d.firm = firm;
    }
    if (d.service == null || d.service.isBlank()) {
      d.service = service;
    }
    if (d.tokens == null) {
      d.tokens = new LinkedHashMap<>();
    }
  }

  public synchronized String getFirm() {
    return d.firm;
  }

  public synchronized String getService() {
    return d.service;
  }

  public synchronized void setFirm(String firm) {
    d.firm = firm;
    save();
  }

  public synchronized void setSettings(String firm, String service) {
    d.firm = firm;
    d.service = service;
    save();
  }

  public synchronized TokenInfo token(String service, String method) {
    return d.tokens.get(service + "/" + method);
  }

  public synchronized void putToken(String service, String method, TokenInfo t) {
    d.tokens.put(service + "/" + method, t);
    save();
  }

  public synchronized Status status(String service, String method) {
    TokenInfo t = token(service, method);
    if (t == null) {
      return new Status("none", "не оплачен", "");
    }
    LocalDateTime now = LocalDateTime.now();
    String period = t.from().format(FMT) + " — " + t.to().format(FMT);
    if (now.isBefore(t.from())) {
      return new Status("wait", "ещё не действует", period);
    }
    if (now.isAfter(t.to())) {
      return new Status("expired", "истёк", period);
    }
    return new Status("live", "действует", period);
  }

  private void save() {
    try {
      Files.createDirectories(file.getParent());
      mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), d);
    } catch (Exception e) {
      System.out.println(">>> [CLIENT] Не удалось сохранить состояние: " + e.getMessage());
    }
  }
}
