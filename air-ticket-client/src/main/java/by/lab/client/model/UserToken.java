package by.lab.client.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Токен оплаты пользователя: получен у сервиса, хранится в БД клиента и присоединяется к запросам платных методов. */
@Entity
@Table(name = "user_tokens")
public class UserToken {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false, length = 30)
  private String username;
  @Column(name = "method_name", nullable = false, length = 100)
  private String methodName;
  @Column(nullable = false, unique = true, length = 64)
  private String token;
  @Column(name = "valid_from", nullable = false)
  private LocalDateTime validFrom;
  @Column(name = "valid_to", nullable = false)
  private LocalDateTime validTo;
  private Double price;
  @Column(name = "synced_at", nullable = false)
  private LocalDateTime syncedAt = LocalDateTime.now();

  public Long getId() { return id; }
  public String getUsername() { return username; }
  public void setUsername(String v) { this.username = v; }
  public String getMethodName() { return methodName; }
  public void setMethodName(String v) { this.methodName = v; }
  public String getToken() { return token; }
  public void setToken(String v) { this.token = v; }
  public LocalDateTime getValidFrom() { return validFrom; }
  public void setValidFrom(LocalDateTime v) { this.validFrom = v; }
  public LocalDateTime getValidTo() { return validTo; }
  public void setValidTo(LocalDateTime v) { this.validTo = v; }
  public Double getPrice() { return price; }
  public void setPrice(Double v) { this.price = v; }
  public LocalDateTime getSyncedAt() { return syncedAt; }
  public void setSyncedAt(LocalDateTime v) { this.syncedAt = v; }
}
