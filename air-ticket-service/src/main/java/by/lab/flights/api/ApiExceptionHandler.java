package by.lab.flights.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.stream.Collectors;

/** Единый JSON-формат ошибок: {"status": 403, "message": "..."}. */
@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, Object>> status(ResponseStatusException e) {
    return body(e.getStatusCode(), e.getReason() == null ? e.getStatusCode().toString() : e.getReason());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException e) {
    String msg = e.getBindingResult().getFieldErrors().stream()
        .map(f -> f.getField() + ": " + f.getDefaultMessage())
        .collect(Collectors.joining("; "));
    return body(HttpStatus.BAD_REQUEST, msg);
  }

  @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
  public ResponseEntity<Map<String, Object>> unreadable(Exception e) {
    return body(HttpStatus.BAD_REQUEST, "Некорректный запрос (формат полей или дат)");
  }

  private static ResponseEntity<Map<String, Object>> body(HttpStatusCode code, String message) {
    return ResponseEntity.status(code).body(Map.of("status", code.value(), "message", message));
  }
}
