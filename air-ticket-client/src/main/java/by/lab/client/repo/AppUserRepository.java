package by.lab.client.repo;

import by.lab.client.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
  Optional<AppUser> findByUsernameIgnoreCase(String username);

  boolean existsByUsernameIgnoreCase(String username);
}
