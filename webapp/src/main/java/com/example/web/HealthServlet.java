package com.example.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health endpoints used by Docker HEALTHCHECK, Kubernetes probes, Azure App Service health
 * check and the smoke tests in every pipeline.
 *
 * <pre>
 *   GET /health        -> overall status
 *   GET /health/live   -> liveness:  "is the process alive?"   (restart me if not)
 *   GET /health/ready  -> readiness: "can I take traffic?"     (stop routing to me if not)
 * </pre>
 *
 * <p>Readiness can be switched off with the environment variable {@code APP_READY=false}.
 * That is handy in tutorials to watch Kubernetes take a pod out of the Service without
 * restarting it.
 */
@WebServlet(name = "health", urlPatterns = {"/health", "/health/live", "/health/ready"})
public class HealthServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  private final transient Clock clock;
  private final transient Environment environment;
  private final Instant startedAt;

  /** Used by the servlet container. */
  public HealthServlet() {
    this(Clock.systemUTC(), Environment.system());
  }

  HealthServlet(Clock clock, Environment environment) {
    this.clock = clock;
    this.environment = environment;
    this.startedAt = clock.instant();
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String check = checkName(request.getServletPath());
    boolean up = !"ready".equals(check) || isReady();

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("status", up ? "UP" : "DOWN");
    body.put("check", check);
    body.put("uptimeSeconds", Duration.between(startedAt, clock.instant()).getSeconds());
    Responses.json(response,
        up ? HttpServletResponse.SC_OK : HttpServletResponse.SC_SERVICE_UNAVAILABLE, body);
  }

  private boolean isReady() {
    return !"false".equalsIgnoreCase(environment.get("APP_READY", "true").strip());
  }

  private static String checkName(String servletPath) {
    if ("/health/live".equals(servletPath)) {
      return "live";
    }
    if ("/health/ready".equals(servletPath)) {
      return "ready";
    }
    return "overall";
  }
}
