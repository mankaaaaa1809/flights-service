package by.lab.client.web;

import by.lab.client.model.AppUser;
import by.lab.client.repo.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

  private final AppUserRepository users;
  private final PasswordEncoder encoder;

  public AuthController(AppUserRepository users, PasswordEncoder encoder) {
    this.users = users;
    this.encoder = encoder;
  }

  @GetMapping("/login")
  public String login() {
    return "login";
  }

  @GetMapping("/register")
  public String registerForm() {
    return "register";
  }

  @PostMapping("/register")
  public String register(@RequestParam String username, @RequestParam String password,
      @RequestParam String confirm, Model m) {
    String name = username.trim();
    String error = null;
    if (!name.matches("[A-Za-z0-9_-]{3,30}")) {
      error = "Логин: 3–30 символов, латинские буквы, цифры, «_» и «-»";
    } else if (password.length() < 6) {
      error = "Пароль должен быть не короче 6 символов";
    } else if (!password.equals(confirm)) {
      error = "Пароли не совпадают";
    } else if (users.existsByUsernameIgnoreCase(name)) {
      error = "Такой логин уже занят";
    }
    if (error != null) {
      m.addAttribute("error", error);
      m.addAttribute("username", name);
      return "register";
    }
    users.save(new AppUser(name, encoder.encode(password)));
    return "redirect:/login?registered";
  }
}
