package com.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class LanguageTest {

  @ParameterizedTest(name = "''{0}'' -> {1}")
  @CsvSource({
      "en,    EN",
      "EN,    EN",
      "' ar ', AR",
      "Fr,    FR",
      "es,    ES",
      "de,    DE",
  })
  void findsLanguageByCodeIgnoringCaseAndSpaces(String code, Language expected) {
    assertEquals(Optional.of(expected), Language.fromCode(code));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"xx", "english", "e n"})
  void unknownCodesAreEmpty(String code) {
    assertFalse(Language.fromCode(code).isPresent());
  }

  @ParameterizedTest
  @EnumSource(Language.class)
  void everyTemplateHasExactlyOnePlaceholder(Language language) {
    String template = language.template();
    assertEquals(template.indexOf("%s"), template.lastIndexOf("%s"), "only one %s allowed");
    assertTrue(template.contains("%s"), "template must contain %s");
    assertFalse(language.displayName().isBlank());
  }

  @Test
  void listsSupportedCodes() {
    assertEquals("en, ar, fr, es, de", Language.supportedCodes());
  }
}
