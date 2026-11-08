package by.lab.flights;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AirTicketApplication {
  public static void main(String[] args) {
    SpringApplication.run(AirTicketApplication.class, args);
  }
}
