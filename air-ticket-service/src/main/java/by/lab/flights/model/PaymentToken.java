package by.lab.flights.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Токен, присланный сервисом оплаты: даёт доступ к одному платному методу в интервале [validFrom; validTo]. */
@Entity
@Table(name = "payment_tokens")
public class PaymentToken {
  @Id @Column(length = 64)
  private String token;
  @Column(name = "method_name", nullable = false, length = 100)
  private String methodName;
  @Column(name = "valid_from", nullable = false)
  private LocalDateTime validFrom;
  @Column(name = "valid_to", nullable = false)
  private LocalDateTime validTo;
  /** true - токен подтверждён запросом в сервис оплаты. */
  @Column(nullable = false)
  private boolean verified;
  @Column(name = "received_at", nullable = false)
  private LocalDateTime receivedAt = LocalDateTime.now();
  /** Фирма (логин пользователя), оплатившая метод; становится известна после подтверждения в сервисе оплаты. */
  @Column(name = "firm_id", length = 100)
  private String firmId;
  private Double price;

  public String getToken() { return token; }
  public void setToken(String v) { this.token = v; }
  public String getMethodName() { return methodName; }
  public void setMethodName(String v) { this.methodName = v; }
  public LocalDateTime getValidFrom() { return validFrom; }
  public void setValidFrom(LocalDateTime v) { this.validFrom = v; }
  public LocalDateTime getValidTo() { return validTo; }
  public void setValidTo(LocalDateTime v) { this.validTo = v; }
  public boolean isVerified() { return verified; }
  public void setVerified(boolean v) { this.verified = v; }
  public LocalDateTime getReceivedAt() { return receivedAt; }
  public String getFirmId() { return firmId; }
  public void setFirmId(String v) { this.firmId = v; }
  public Double getPrice() { return price; }
  public void setPrice(Double v) { this.price = v; }
}
