package by.lab.flights;

import by.lab.flights.model.Flight;
import by.lab.flights.repo.FlightRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Демо-данные при первом запуске (пока таблица рейсов пуста). */
@Component
public class DataSeeder implements CommandLineRunner {

  private final FlightRepository flights;

  public DataSeeder(FlightRepository flights) {
    this.flights = flights;
  }

  @Override
  public void run(String... args) {
    if (flights.count() > 0) return;
    LocalDateTime base = LocalDateTime.now().plusDays(3).withHour(8).withMinute(0).withSecond(0).withNano(0);
    add("B2 701", "Минск", "Москва", base, 95, "129.00", 120);
    add("B2 702", "Москва", "Минск", base.plusHours(6), 95, "135.00", 120);
    add("LH 1431", "Минск", "Франкфурт", base.plusDays(1), 150, "310.50", 150);
    add("TK 392", "Минск", "Стамбул", base.plusDays(2).plusHours(3), 190, "240.00", 180);
    add("B2 835", "Минск", "Тбилиси", base.plusDays(4).plusHours(5), 210, "199.99", 90);
  }

  private void add(String no, String from, String to, LocalDateTime dep, int minutes, String price, int seats) {
    Flight f = new Flight();
    f.setFlightNumber(no);
    f.setOrigin(from);
    f.setDestination(to);
    f.setDepartureTime(dep);
    f.setArrivalTime(dep.plusMinutes(minutes));
    f.setPrice(new BigDecimal(price));
    f.setTotalSeats(seats);
    flights.save(f);
  }
}
