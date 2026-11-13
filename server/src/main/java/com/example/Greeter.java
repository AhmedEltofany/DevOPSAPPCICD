package com.example;

import java.util.Objects;

/**
 * Builds friendly greetings in several {@link Language languages}.
 *
 * <p>This class is deliberately free of any web or framework code so it can be unit tested
 * in milliseconds and reused by any front end (servlets, CLI, a future REST service...).
 */
public class Greeter {

  /** Name used when the caller does not provide one. */
  public static final String DEFAULT_NAME = "World";

  /** Longer names are truncated so a single request cannot produce a huge response. */
  public static final int MAX_NAME_LENGTH = 50;

  private final Language defaultLanguage;

  /** Creates a greeter that speaks English unless told otherwise. */
  public Greeter() {
    this(Language.EN);
  }

  /**
   * Creates a greeter with a custom default language.
   *
   * @param defaultLanguage language used by {@link #greet(String)}; must not be {@code null}
   */
  public Greeter(Language defaultLanguage) {
    this.defaultLanguage = Objects.requireNonNull(defaultLanguage, "defaultLanguage");
  }

  /** Language used when none is passed explicitly. */
  public Language defaultLanguage() {
    return defaultLanguage;
  }

  /**
   * Greets someone in the default language.
   *
   * @param someone the name to greet; {@code null} or blank falls back to {@value #DEFAULT_NAME}
   * @return the greeting, never {@code null}
   */
  public String greet(String someone) {
    return greet(someone, defaultLanguage);
  }

  /**
   * Greets someone in the given language.
   *
   * @param someone  the name to greet; {@code null} or blank falls back to {@value #DEFAULT_NAME}
   * @param language the language to use; must not be {@code null}
   * @return the greeting, never {@code null}
   */
  public String greet(String someone, Language language) {
    Objects.requireNonNull(language, "language");
    return String.format(language.template(), normalizeName(someone));
  }

  /**
   * Cleans user input before it is used in a greeting: trims it, collapses inner whitespace,
   * falls back to {@value #DEFAULT_NAME} when empty, and truncates to {@value #MAX_NAME_LENGTH}
   * characters (counting Unicode code points, so emoji and Arabic letters are never split).
   *
   * @param raw the raw name, may be {@code null}
   * @return a safe, non-empty name
   */
  public static String normalizeName(String raw) {
    if (raw == null) {
      return DEFAULT_NAME;
    }
    String cleaned = raw.strip().replaceAll("\\s+", " ");
    if (cleaned.isEmpty()) {
      return DEFAULT_NAME;
    }
    if (cleaned.codePointCount(0, cleaned.length()) > MAX_NAME_LENGTH) {
      return cleaned.substring(0, cleaned.offsetByCodePoints(0, MAX_NAME_LENGTH));
    }
    return cleaned;
  }
}
