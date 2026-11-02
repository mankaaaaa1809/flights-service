package by.lab.flights.repo;

import by.lab.flights.model.PaymentToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTokenRepository extends JpaRepository<PaymentToken, String> {
}
