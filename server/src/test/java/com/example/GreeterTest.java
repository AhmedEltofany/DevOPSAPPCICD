package com.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class GreeterTest {

  private Greeter greeter;

  @BeforeEach
  void setUp() {
    greeter = new Greeter();
  }

  @Test
  @DisplayName("greeting includes the person being greeted")
  void greetShouldIncludeTheOneBeingGreeted() {
    assertTrue(greeter.greet("World").contains("World"));
  }

  @Test
  @DisplayName("greeting is longer than the name alone")
  void greetShouldIncludeGreetingPhrase() {
    String someone = "World";
    assertTrue(greeter.greet(someone).length() > someone.length());
  }

  @Test
  void defaultsToEnglish() {
    assertEquals(Language.EN, greeter.defaultLanguage());
    assertEquals("Hello Ahmed, how are you?", greeter.greet("Ahmed"));
  }

  @Test
  void customDefaultLanguageIsUsed() {
    Greeter french = new Greeter(Language.FR);
    assertEquals("Bonjour Ahmed, comment allez-vous ?", french.greet("Ahmed"));
  }

  @ParameterizedTest(name = "{0} greeting contains the name")
  @EnumSource(Language.class)
  void everyLanguageGreetsByName(Language language) {
    String greeting = greeter.greet("Sara", language);
    assertTrue(greeting.contains("Sara"), () -> language + " greeting was: " + greeting);
  }

  @ParameterizedTest(name = "[{index}] blank name ''{0}'' falls back to World")
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "\t", "\n "})
  void blankNamesFallBackToDefault(String blank) {
    assertEquals("Hello World, how are you?", greeter.greet(blank));
  }

  @Test
  void rejectsNullLanguage() {
    assertThrows(NullPointerException.class, () -> greeter.greet("Ahmed", null));
  }

  @Test
  void rejectsNullDefaultLanguage() {
    assertThrows(NullPointerException.class, () -> new Greeter(null));
  }

  @Nested
  @DisplayName("normalizeName")
  class NormalizeName {

    @ParameterizedTest(name = "''{0}'' -> ''{1}''")
    @CsvSource(delimiter = '|', value = {
        "Ahmed         | Ahmed",
        "'  Ahmed  '   | Ahmed",
        "'Ahmed   Ali' | Ahmed Ali",
    })
    void trimsAndCollapsesWhitespace(String raw, String expected) {
      assertEquals(expected, Greeter.normalizeName(raw));
    }

    @Test
    void truncatesLongNames() {
      String longName = "x".repeat(Greeter.MAX_NAME_LENGTH + 25);
      assertEquals(Greeter.MAX_NAME_LENGTH, Greeter.normalizeName(longName).length());
    }

    @Test
    void neverSplitsAnEmojiWhenTruncating() {
      // Each emoji is 1 code point but 2 Java chars.
      String emojis = "😀".repeat(Greeter.MAX_NAME_LENGTH + 5);
      String result = Greeter.normalizeName(emojis);
      assertEquals(Greeter.MAX_NAME_LENGTH, result.codePointCount(0, result.length()));
    }

    @Test
    void keepsNamesAtTheLimitUntouched() {
      String exact = "y".repeat(Greeter.MAX_NAME_LENGTH);
      assertEquals(exact, Greeter.normalizeName(exact));
    }
  }
}
