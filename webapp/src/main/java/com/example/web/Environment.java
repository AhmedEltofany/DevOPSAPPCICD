package com.example.web;

/**
 * Read-only view of environment variables.
 *
 * <p>Servlets depend on this interface instead of calling {@link System#getenv(String)}
 * directly so tests can pass in fake values (see {@code InfoServletTest}).
 */
@FunctionalInterface
interface Environment {

  /** Returns the raw value, or {@code null} when the variable is not set. */
  String getRaw(String name);

  /** Returns the value, or {@code fallback} when the variable is unset or blank. */
  default String get(String name, String fallback) {
    String value = getRaw(name);
    return value == null || value.isBlank() ? fallback : value;
  }

  /** The real process environment. */
  static Environment system() {
    return System::getenv;
  }
}
