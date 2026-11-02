package by.lab.flights.service;

import by.lab.flights.dto.Dtos.BookingDto;
import by.lab.flights.dto.Dtos.BookingRequest;
import by.lab.flights.model.Booking;
import by.lab.flights.model.Flight;
import by.lab.flights.repo.BookingRepository;
import by.lab.flights.repo.FlightRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class BookingService {

  private final BookingRepository bookings;
  private final FlightRepository flights;
  private final FlightService flightService;

  public BookingService(BookingRepository bookings, FlightRepository flights, FlightService flightService) {
    this.bookings = bookings;
    this.flights = flights;
    this.flightService = flightService;
  }

  @Transactional(readOnly = true)
  public List<BookingDto> list(Long flightId) {
    List<Booking> list = flightId == null
        ? bookings.findAllByOrderByIdDesc()
        : bookings.findByFlightIdOrderByIdDesc(flightId);
    return list.stream().map(BookingService::toDto).toList();
  }

  @Transactional(readOnly = true)
  public BookingDto get(Long id) {
    return toDto(find(id));
  }

  @Transactional
  public BookingDto create(BookingRequest req) {
    Flight flight = flights.findById(req.flightId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Рейс " + req.flightId() + " не найден"));
    String status = req.status() == null ? Booking.CONFIRMED : req.status();
    if (!Booking.CANCELLED.equals(status)) {
      ensureSeat(flight, 0);
    }
    Booking b = new Booking();
    b.setFlight(flight);
    fill(b, req, status);
    return toDto(bookings.save(b));
  }

  @Transactional
  public BookingDto update(Long id, BookingRequest req) {
    Booking b = find(id);
    Flight flight = flights.findById(req.flightId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Рейс " + req.flightId() + " не найден"));
    String status = req.status() == null ? b.getStatus() : req.status();
    if (!Booking.CANCELLED.equals(status)) {
      boolean countedNow = !Booking.CANCELLED.equals(b.getStatus())
          && b.getFlight().getId().equals(flight.getId());
      ensureSeat(flight, countedNow ? 1 : 0);
    }
    b.setFlight(flight);
    fill(b, req, status);
    return toDto(bookings.save(b));
  }

  @Transactional
  public void delete(Long id) {
    bookings.delete(find(id));
  }

  /** alreadyCounted - сколько занятых мест принадлежит самой изменяемой брони. */
  private void ensureSeat(Flight flight, long alreadyCounted) {
    if (flightService.occupied(flight.getId()) - alreadyCounted >= flight.getTotalSeats()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "На рейсе " + flight.getFlightNumber() + " нет свободных мест");
    }
  }

  private void fill(Booking b, BookingRequest req, String status) {
    b.setPassengerName(req.passengerName().trim());
    b.setPassportNumber(req.passportNumber().trim());
    b.setStatus(status);
  }

  private Booking find(Long id) {
    return bookings.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Бронь " + id + " не найдена"));
  }

  private static BookingDto toDto(Booking b) {
    return new BookingDto(b.getId(), b.getFlight().getId(), b.getFlight().getFlightNumber(),
        b.getPassengerName(), b.getPassportNumber(), b.getStatus(), b.getCreatedAt());
  }
}
