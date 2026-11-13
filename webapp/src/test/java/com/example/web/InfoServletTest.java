package com.example.web;

import static com.example.web.ServletTestSupport.request;
import static com.example.web.ServletTestSupport.response;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.web.ServletTestSupport.CapturedResponse;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class InfoServletTest {

  private static BuildInfo buildInfo() {
    Properties properties = new Properties();
    properties.setProperty("app.name", "devopsappcicd");
    properties.setProperty("app.version", "1.2.3");
    properties.setProperty("build.commit", "abc1234");
    properties.setProperty("build.time", "2026-11-13T10:00:00Z");
    return new BuildInfo(properties);
  }

  @Test
  void showsBuildAndRuntimeInformation() throws Exception {
    Map<String, String> env = Map.of("APP_ENV", "dev", "HOSTNAME", "pod-1");
    InfoServlet servlet = new InfoServlet(buildInfo(), env::get);
    CapturedResponse response = response();

    servlet.doGet(request("/api/info", Map.of()), response.mock());

    String body = response.body();
    assertTrue(body.startsWith("{\"app\":\"devopsappcicd\",\"version\":\"1.2.3\","
        + "\"commit\":\"abc1234\",\"buildTime\":\"2026-11-13T10:00:00Z\","
        + "\"environment\":\"dev\",\"hostname\":\"pod-1\",\"javaVersion\":"), body);
  }

  @Test
  void defaultsEnvironmentToLocal() throws Exception {
    InfoServlet servlet = new InfoServlet(buildInfo(), name -> null);
    CapturedResponse response = response();

    servlet.doGet(request("/api/info", Map.of()), response.mock());

    assertTrue(response.body().contains("\"environment\":\"local\""), response.body());
    assertTrue(response.body().contains("\"hostname\":\""), response.body());
  }
}
