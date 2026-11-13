package com.example.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shows what is running where.
 *
 * <pre>
 *   GET /api/info
 *   {"app":"devopsappcicd","version":"1.0-SNAPSHOT","commit":"3f2a...","buildTime":"...",
 *    "environment":"dev","hostname":"devopsappcicd-7c9d-xk2p","javaVersion":"17.0.12"}
 * </pre>
 *
 * <p>{@code environment} comes from the {@code APP_ENV} variable set by Docker Compose,
 * Kubernetes overlays or Azure App Service settings. {@code hostname} is the container or
 * pod name - refresh the page behind a load balancer and watch it change.
 */
@WebServlet(name = "info", urlPatterns = "/api/info")
public class InfoServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  private final transient BuildInfo buildInfo;
  private final transient Environment environment;

  /** Used by the servlet container. */
  public InfoServlet() {
    this(BuildInfo.load(), Environment.system());
  }

  InfoServlet(BuildInfo buildInfo, Environment environment) {
    this.buildInfo = buildInfo;
    this.environment = environment;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("app", buildInfo.appName());
    body.put("version", buildInfo.version());
    body.put("commit", buildInfo.commit());
    body.put("buildTime", buildInfo.buildTime());
    body.put("environment", environment.get("APP_ENV", "local"));
    body.put("hostname", hostname());
    body.put("javaVersion", System.getProperty("java.version"));
    Responses.json(response, HttpServletResponse.SC_OK, body);
  }

  private String hostname() {
    String fromEnv = environment.get("HOSTNAME", null);
    if (fromEnv != null) {
      return fromEnv;
    }
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return BuildInfo.UNKNOWN;
    }
  }
}
