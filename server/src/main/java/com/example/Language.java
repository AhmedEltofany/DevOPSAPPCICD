package com.example;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Languages the {@link Greeter} can speak.
 *
 * <p>Each language carries a {@link java.util.Formatter format} template with exactly one
 * {@code %s} placeholder for the person's name. To add a language, add a constant here and
 * a test case in {@code LanguageTest} - nothing else needs to change.
 */
public enum Language {

  EN("en", "English", "Hello %s, how are you?"),
  AR("ar", "Arabic", "مرحباً %s، كيف حالك؟"),
  FR("fr", "French", "Bonjour %s, comment allez-vous ?"),
  ES("es", "Spanish", "¡Hola %s! ¿Cómo estás?"),
  DE("de", "German", "Hallo %s, wie geht es dir?");

  private final String code;
  private final String displayName;
  private final String template;

  Language(String code, String displayName, String template) {
    this.code = code;
    this.displayName = displayName;
    this.template = template;
  }

  /** ISO 639-1 code, e.g. {@code "en"}. */
  public String code() {
    return code;
  }

  /** Human readable English name, e.g. {@code "Arabic"}. */
  public String displayName() {
    return displayName;
  }

  /** Format template with a single {@code %s} placeholder for the name. */
  public String template() {
    return template;
  }

  /**
   * Looks up a language by its code, ignoring case and surrounding whitespace.
   *
   * @param code a code such as {@code "en"} or {@code " AR "}; may be {@code null}
   * @return the matching language, or empty when the code is unknown or {@code null}
   */
  public static Optional<Language> fromCode(String code) {
    if (code == null) {
      return Optional.empty();
    }
    String normalized = code.strip().toLowerCase(Locale.ROOT);
    return Arrays.stream(values())
        .filter(language -> language.code.equals(normalized))
        .findFirst();
  }

  /** Comma separated list of all supported codes, e.g. {@code "en, ar, fr, es, de"}. */
  public static String supportedCodes() {
    return Arrays.stream(values()).map(Language::code).collect(Collectors.joining(", "));
  }
}
