package by.lab.client.security;

import by.lab.client.repo.AppUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/** Вход по логину и паролю (сессия). Пароли хранятся в БД клиента в виде BCrypt-хеша. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService userDetailsService(AppUserRepository users) {
    return username -> users.findByUsernameIgnoreCase(username.trim())
        .map(u -> User.withUsername(u.getUsername()).password(u.getPasswordHash()).roles("USER").build())
        .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
  }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(a -> a
            .requestMatchers("/css/**", "/js/**", "/login", "/register", "/error").permitAll()
            .anyRequest().authenticated())
        .formLogin(f -> f.loginPage("/login").defaultSuccessUrl("/", true).failureUrl("/login?error").permitAll())
        .logout(l -> l.logoutSuccessUrl("/login?logout"));
    return http.build();
  }
}
