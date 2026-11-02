package by.lab.flights.repo;

import by.lab.flights.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
  List<Booking> findAllByOrderByIdDesc();
  List<Booking> findByFlightIdOrderByIdDesc(Long flightId);
  long countByFlightIdAndStatusNot(Long flightId, String status);
  boolean existsByFlightId(Long flightId);
}
