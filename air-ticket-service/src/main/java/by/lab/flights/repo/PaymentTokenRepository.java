package by.lab.flights.repo;

import by.lab.flights.model.PaymentToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PaymentTokenRepository extends JpaRepository<PaymentToken, String> {

  List<PaymentToken> findByFirmIdIgnoreCaseOrderByValidToDesc(String firmId);

  /** Токены, у которых ещё не известна фирма (не подтверждены в сервисе оплаты). */
  @Query("select t from PaymentToken t where t.verified = false or t.firmId is null")
  List<PaymentToken> findUnresolved();
}
