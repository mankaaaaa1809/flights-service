package by.lab.client.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Пользователь клиента. Логин одновременно служит идентификатором фирмы (firmId) в сервисе оплаты. */
@Entity
@Table(name = "app_users")
public class AppUser {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false, unique = true, length = 30)
  private String username;
  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  protected AppUser() {
  }

  public AppUser(String username, String passwordHash) {
    this.username = username;
    this.passwordHash = passwordHash;
  }

  public Long getId() { return id; }
  public String getUsername() { return username; }
  public String getPasswordHash() { return passwordHash; }
  public LocalDateTime getCreatedAt() { return createdAt; }
}
