package by.lab.flights.api;

import by.lab.flights.dto.Dtos.BookingDto;
import by.lab.flights.dto.Dtos.BookingRequest;
import by.lab.flights.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Платный метод "Bookings": бронирование билетов (все операции на /api/bookings). */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

  private final BookingService service;

  public BookingController(BookingService service) {
    this.service = service;
  }

  @GetMapping
  public List<BookingDto> list(@RequestParam(required = false) Long flightId) {
    return service.list(flightId);
  }

  @GetMapping("/{id}")
  public BookingDto get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BookingDto create(@Valid @RequestBody BookingRequest req) {
    return service.create(req);
  }

  @PutMapping("/{id}")
  public BookingDto update(@PathVariable Long id, @Valid @RequestBody BookingRequest req) {
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}
