package by.lab.client.repo;

import by.lab.client.model.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {
  Optional<UserToken> findByToken(String token);

  List<UserToken> findByUsernameIgnoreCaseAndMethodName(String username, String methodName);

  List<UserToken> findByUsernameIgnoreCaseOrderByValidToDesc(String username);
}
