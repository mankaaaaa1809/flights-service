package by.lab.flights.service;

import by.lab.flights.dto.Dtos.FlightDto;
import by.lab.flights.model.Booking;
import by.lab.flights.model.Flight;
import by.lab.flights.repo.BookingRepository;
import by.lab.flights.repo.FlightRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class FlightService {

  private final FlightRepository flights;
  private final BookingRepository bookings;

  public FlightService(FlightRepository flights, BookingRepository bookings) {
    this.flights = flights;
    this.bookings = bookings;
  }

  @Transactional(readOnly = true)
  public List<FlightDto> search(String origin, String destination, LocalDate date) {
    return flights.findAll(Sort.by("departureTime")).stream()
        .filter(f -> contains(f.getOrigin(), origin))
        .filter(f -> contains(f.getDestination(), destination))
        .filter(f -> date == null || f.getDepartureTime().toLocalDate().equals(date))
        .map(this::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public FlightDto get(Long id) {
    return toDto(find(id));
  }

  @Transactional
  public FlightDto create(FlightDto dto) {
    Flight f = new Flight();
    apply(f, dto);
    return toDto(flights.save(f));
  }

  @Transactional
  public FlightDto update(Long id, FlightDto dto) {
    Flight f = find(id);
    long occupied = occupied(id);
    if (dto.totalSeats() < occupied) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Мест не может быть меньше, чем уже занято (" + occupied + ")");
    }
    apply(f, dto);
    return toDto(flights.save(f));
  }

  @Transactional
  public void delete(Long id) {
    Flight f = find(id);
    if (bookings.existsByFlightId(id)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Нельзя удалить рейс, на который есть бронирования");
    }
    flights.delete(f);
  }

  Flight find(Long id) {
    return flights.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Рейс " + id + " не найден"));
  }

  long occupied(Long flightId) {
    return bookings.countByFlightIdAndStatusNot(flightId, Booking.CANCELLED);
  }

  private void apply(Flight f, FlightDto d) {
    if (!d.arrivalTime().isAfter(d.departureTime())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Прибытие должно быть позже вылета");
    }
    f.setFlightNumber(d.flightNumber().trim());
    f.setOrigin(d.origin().trim());
    f.setDestination(d.destination().trim());
    f.setDepartureTime(d.departureTime());
    f.setArrivalTime(d.arrivalTime());
    f.setPrice(d.price());
    f.setTotalSeats(d.totalSeats());
  }

  private FlightDto toDto(Flight f) {
    int free = f.getTotalSeats() - (int) occupied(f.getId());
    return new FlightDto(f.getId(), f.getFlightNumber(), f.getOrigin(), f.getDestination(),
        f.getDepartureTime(), f.getArrivalTime(), f.getPrice(), f.getTotalSeats(), free);
  }

  private static boolean contains(String value, String query) {
    return query == null || query.isBlank() || value.toLowerCase().contains(query.trim().toLowerCase());
  }
}
