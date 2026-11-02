package by.lab.flights.api;

import by.lab.flights.dto.Dtos.FlightDto;
import by.lab.flights.service.FlightService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Платный метод "Flights": каталог рейсов (все операции на /api/flights). */
@RestController
@RequestMapping("/api/flights")
public class FlightController {

  private final FlightService service;

  public FlightController(FlightService service) {
    this.service = service;
  }

  @GetMapping
  public List<FlightDto> search(@RequestParam(required = false) String origin,
      @RequestParam(required = false) String destination,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return service.search(origin, destination, date);
  }

  @GetMapping("/{id}")
  public FlightDto get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public FlightDto create(@Valid @RequestBody FlightDto dto) {
    return service.create(dto);
  }

  @PutMapping("/{id}")
  public FlightDto update(@PathVariable Long id, @Valid @RequestBody FlightDto dto) {
    return service.update(id, dto);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}
