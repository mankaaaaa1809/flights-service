package by.lab.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/** Формат ответа Регистра сервисов (ServiceView). */
public final class Dto {
  private Dto() {
  }

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
}
