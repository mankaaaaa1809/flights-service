package by.lab.flights.service;

import by.lab.flights.model.PaymentToken;
import by.lab.flights.repo.PaymentTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Подтверждает пришедшие токены в сервисе оплаты и узнаёт, какой фирме (пользователю клиента) они принадлежат.
 * Делается не в момент уведомления: оплата шлёт его до завершения своей транзакции.
 */
@Component
public class TokenResolver {

  private final PaymentTokenRepository tokens;
  private final TokenService tokenService;

  public TokenResolver(PaymentTokenRepository tokens, TokenService tokenService) {
    this.tokens = tokens;
    this.tokenService = tokenService;
  }

  @Scheduled(fixedDelay = 4000, initialDelay = 10000)
  public void resolveNow() {
    for (PaymentToken t : tokens.findUnresolved()) {
      tokenService.tryResolve(t);
    }
  }
}
