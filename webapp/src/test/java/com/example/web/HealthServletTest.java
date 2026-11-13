package com.example.web;

import static com.example.web.ServletTestSupport.request;
import static com.example.web.ServletTestSupport.response;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import com.example.web.ServletTestSupport.CapturedResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HealthServletTest {

  /** A clock we can move forward by hand. */
  private static final class MutableClock extends Clock {
    private Instant now = Instant.parse("2026-01-01T00:00:00Z");

    void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public java.time.ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource({
      "/health,       overall",
      "/health/live,  live",
      "/health/ready, ready",
  })
  void reportsUpWithUptime(String path, String check) throws Exception {
    MutableClock clock = new MutableClock();
    HealthServlet servlet = new HealthServlet(clock, name -> null);
    clock.advance(Duration.ofSeconds(42));
    CapturedResponse response = response();

    servlet.doGet(request(path, Map.of()), response.mock());

    verify(response.mock()).setStatus(HttpServletResponse.SC_OK);
    assertEquals("{\"status\":\"UP\",\"check\":\"" + check + "\",\"uptimeSeconds\":42}",
        response.body());
  }

  @Test
  void readinessCanBeSwitchedOff() throws Exception {
    HealthServlet servlet = new HealthServlet(new MutableClock(),
        name -> "APP_READY".equals(name) ? "false" : null);
    CapturedResponse response = response();

    servlet.doGet(request("/health/ready", Map.of()), response.mock());

    verify(response.mock()).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    assertEquals("{\"status\":\"DOWN\",\"check\":\"ready\",\"uptimeSeconds\":0}",
        response.body());
  }

  @Test
  void livenessIgnoresReadinessSwitch() throws Exception {
    HealthServlet servlet = new HealthServlet(new MutableClock(),
        name -> "APP_READY".equals(name) ? "false" : null);
    CapturedResponse response = response();

    servlet.doGet(request("/health/live", Map.of()), response.mock());

    verify(response.mock()).setStatus(HttpServletResponse.SC_OK);
  }
}
