package com.example.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Version metadata written into {@code build-info.properties} by Maven resource filtering.
 *
 * <p>Seeing the exact version and Git commit at {@code /api/info} is one of the most useful
 * things in a CI/CD setup: it answers "what is actually deployed in this environment?"
 */
public final class BuildInfo {

  static final String RESOURCE = "/build-info.properties";
  static final String UNKNOWN = "unknown";

  private final Properties properties;

  BuildInfo(Properties properties) {
    this.properties = properties;
  }

  /** Loads build info from the classpath; returns "unknown" values if the file is missing. */
  public static BuildInfo load() {
    Properties properties = new Properties();
    try (InputStream in = BuildInfo.class.getResourceAsStream(RESOURCE)) {
      if (in != null) {
        properties.load(in);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read " + RESOURCE, e);
    }
    return new BuildInfo(properties);
  }

  public String appName() {
    return get("app.name");
  }

  public String version() {
    return get("app.version");
  }

  public String commit() {
    return get("build.commit");
  }

  public String buildTime() {
    return get("build.time");
  }

  private String get(String key) {
    String value = properties.getProperty(key);
    // An unreplaced "${...}" placeholder means the file was not filtered.
    if (value == null || value.isBlank() || value.startsWith("${")) {
      return UNKNOWN;
    }
    return value.strip();
  }
}
