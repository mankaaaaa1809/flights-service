package by.lab.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Форматы данных, которыми клиент обменивается с Регистром и прикладным сервисом. */
public final class Dto {
  private Dto() {
  }

  /** Ответ Регистра (ServiceView). */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Param(String name, String type, String location, boolean required) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Method(String name, String httpMethod, String path, String description,
      BigDecimal price, List<Param> parameters) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Service(String name, String baseUrl, String description, List<Method> methods) {
  }

  /** Токен оплаты фирмы: ответ скрытого метода сервиса /internal/firm-tokens. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record FirmToken(String token, String methodName, LocalDateTime validFrom, LocalDateTime validTo,
      Double price) {
  }

  /** Услуга для отображения в каталоге (строится из описания метода в Регистре). */
  public record Item(String method, String title, String description, BigDecimal price) {
  }
}
