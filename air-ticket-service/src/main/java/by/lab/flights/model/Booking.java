package by.lab.flights.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {
  public static final String CONFIRMED = "CONFIRMED";
  public static final String CANCELLED = "CANCELLED";

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(optional = false)
  @JoinColumn(name = "flight_id")
  private Flight flight;
  @Column(name = "passenger_name", nullable = false, length = 150)
  private String passengerName;
  @Column(name = "passport_number", nullable = false, length = 30)
  private String passportNumber;
  @Column(nullable = false, length = 20)
  private String status = CONFIRMED;
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  public Long getId() { return id; }
  public Flight getFlight() { return flight; }
  public void setFlight(Flight v) { this.flight = v; }
  public String getPassengerName() { return passengerName; }
  public void setPassengerName(String v) { this.passengerName = v; }
  public String getPassportNumber() { return passportNumber; }
  public void setPassportNumber(String v) { this.passportNumber = v; }
  public String getStatus() { return status; }
  public void setStatus(String v) { this.status = v; }
  public LocalDateTime getCreatedAt() { return createdAt; }
}
