package by.lab.flights.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "flights")
public class Flight {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "flight_number", nullable = false, length = 20)
  private String flightNumber;
  @Column(nullable = false, length = 100)
  private String origin;
  @Column(nullable = false, length = 100)
  private String destination;
  @Column(name = "departure_time", nullable = false)
  private LocalDateTime departureTime;
  @Column(name = "arrival_time", nullable = false)
  private LocalDateTime arrivalTime;
  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal price;
  @Column(name = "total_seats", nullable = false)
  private int totalSeats;

  public Long getId() { return id; }
  public String getFlightNumber() { return flightNumber; }
  public void setFlightNumber(String v) { this.flightNumber = v; }
  public String getOrigin() { return origin; }
  public void setOrigin(String v) { this.origin = v; }
  public String getDestination() { return destination; }
  public void setDestination(String v) { this.destination = v; }
  public LocalDateTime getDepartureTime() { return departureTime; }
  public void setDepartureTime(LocalDateTime v) { this.departureTime = v; }
  public LocalDateTime getArrivalTime() { return arrivalTime; }
  public void setArrivalTime(LocalDateTime v) { this.arrivalTime = v; }
  public BigDecimal getPrice() { return price; }
  public void setPrice(BigDecimal v) { this.price = v; }
  public int getTotalSeats() { return totalSeats; }
  public void setTotalSeats(int v) { this.totalSeats = v; }
}
