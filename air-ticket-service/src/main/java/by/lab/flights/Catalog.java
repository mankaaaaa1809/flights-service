package by.lab.flights;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Платные услуги сервиса: то, что видит пользователь в каталоге и что регистрируется в Регистре. */
public final class Catalog {
  private Catalog() {
  }

  public record Item(String method, String title, String description, BigDecimal price) {
  }

  public static final Item FLIGHTS = new Item("Flights", "Каталог рейсов",
      "Расписание рейсов: поиск, добавление, редактирование, свободные места.", new BigDecimal("10.00"));
  public static final Item BOOKINGS = new Item("Bookings", "Бронирование билетов",
      "Оформление, изменение и отмена бронирований пассажиров.", new BigDecimal("5.00"));

  public static final List<Item> ITEMS = List.of(FLIGHTS, BOOKINGS);

  public static Optional<Item> find(String method) {
    return ITEMS.stream().filter(i -> i.method().equalsIgnoreCase(method)).findFirst();
  }
}
