package by.lab.client.service;

import by.lab.client.Dto;
import by.lab.client.RemoteApi;
import by.lab.client.model.UserToken;
import by.lab.client.repo.UserTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Токены оплаты пользователей. Оплата проходит в сервисе оплаты, токен приходит прикладному сервису;
 * клиент забирает токены пользователя у сервиса и хранит их в своей БД (таблица user_tokens).
 */
@Service
public class UserTokenService {

  private final UserTokenRepository repo;
  private final RemoteApi remote;
  private final String serviceName;
  private final String serviceKey;

  public UserTokenService(UserTokenRepository repo, RemoteApi remote,
      @Value("${client.service-name}") String serviceName, @Value("${client.service-key}") String serviceKey) {
    this.repo = repo;
    this.remote = remote;
    this.serviceName = serviceName;
    this.serviceKey = serviceKey;
  }

  /** Забирает у сервиса токены пользователя (firmId = логин) и сохраняет новые в БД. */
  public void sync(String username) {
    Dto.Service s = remote.service(serviceName);
    for (Dto.FirmToken f : remote.firmTokens(s.baseUrl(), username, serviceKey)) {
      UserToken t = repo.findByToken(f.token()).orElseGet(UserToken::new);
      t.setUsername(username);
      t.setMethodName(f.methodName());
      t.setToken(f.token());
      t.setValidFrom(f.validFrom());
      t.setValidTo(f.validTo());
      t.setPrice(f.price());
      t.setSyncedAt(LocalDateTime.now());
      repo.save(t);
    }
  }

  /** То же, но без исключений: страницы продолжают работать по последним сохранённым данным. */
  public void syncQuietly(String username) {
    try {
      sync(username);
    } catch (RuntimeException e) {
      System.out.println(">>> [CLIENT] Не удалось получить токены у сервиса: " + e.getMessage());
    }
  }

  /** Токен, действующий для метода в текущий момент (с самым поздним сроком окончания). */
  public Optional<UserToken> active(String username, String method) {
    LocalDateTime now = LocalDateTime.now();
    return repo.findByUsernameIgnoreCaseAndMethodName(username, method).stream()
        .filter(t -> !now.isBefore(t.getValidFrom()) && !now.isAfter(t.getValidTo()))
        .max(Comparator.comparing(UserToken::getValidTo));
  }

  public List<UserToken> history(String username) {
    return repo.findByUsernameIgnoreCaseOrderByValidToDesc(username);
  }
}
