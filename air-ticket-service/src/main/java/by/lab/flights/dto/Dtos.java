package by.lab.flights.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class Dtos {
  private Dtos() {}

  /** Рейс (и запрос на создание/изменение, и ответ). id и seatsAvailable в запросе игнорируются. */
  public record FlightDto(
      Long id,
      @NotBlank @Size(max = 20) String flightNumber,
      @NotBlank @Size(max = 100) String origin,
      @NotBlank @Size(max = 100) String destination,
      @NotNull LocalDateTime departureTime,
      @NotNull LocalDateTime arrivalTime,
      @NotNull @PositiveOrZero BigDecimal price,
      @Min(1) int totalSeats,
      Integer seatsAvailable) {
  }

  public record BookingRequest(
      @NotNull Long flightId,
      @NotBlank @Size(max = 150) String passengerName,
      @NotBlank @Size(max = 30) String passportNumber,
      @Pattern(regexp = "CONFIRMED|CANCELLED", message = "status: CONFIRMED или CANCELLED") String status) {
  }

  public record BookingDto(Long id, Long flightId, String flightNumber, String passengerName,
      String passportNumber, String status, LocalDateTime createdAt) {
  }

  /** Уведомление от сервиса оплаты (формат совпадает с PaymentDtos.TokenNotification). */
  public record TokenNotification(
      @NotBlank String token,
      @NotBlank String methodName,
      @NotNull LocalDateTime validFrom,
      @NotNull LocalDateTime validTo) {
  }
}
