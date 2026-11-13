package com.example.web;

import static com.example.web.ServletTestSupport.request;
import static com.example.web.ServletTestSupport.response;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import com.example.web.ServletTestSupport.CapturedResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GreetApiServletTest {

  private final GreetApiServlet servlet = new GreetApiServlet();

  @Test
  void greetsInEnglishByDefault() throws Exception {
    CapturedResponse response = response();

    servlet.doGet(request("/api/greet", Map.of("name", "Ahmed")), response.mock());

    verify(response.mock()).setStatus(HttpServletResponse.SC_OK);
    verify(response.mock()).setContentType("application/json");
    assertEquals(
        "{\"name\":\"Ahmed\",\"language\":\"en\",\"message\":\"Hello Ahmed, how are you?\"}",
        response.body());
  }

  @Test
  void greetsInRequestedLanguage() throws Exception {
    CapturedResponse response = response();

    servlet.doGet(request("/api/greet", Map.of("name", "Ahmed", "lang", "FR")), response.mock());

    assertEquals(
        "{\"name\":\"Ahmed\",\"language\":\"fr\","
            + "\"message\":\"Bonjour Ahmed, comment allez-vous ?\"}",
        response.body());
  }

  @Test
  void blankLanguageMeansDefault() throws Exception {
    CapturedResponse response = response();

    servlet.doGet(request("/api/greet", Map.of("lang", "  ")), response.mock());

    verify(response.mock()).setStatus(HttpServletResponse.SC_OK);
    assertEquals(
        "{\"name\":\"World\",\"language\":\"en\",\"message\":\"Hello World, how are you?\"}",
        response.body());
  }

  @Test
  void unknownLanguageIsABadRequest() throws Exception {
    CapturedResponse response = response();

    servlet.doGet(request("/api/greet", Map.of("lang", "xx")), response.mock());

    verify(response.mock()).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    assertEquals(
        "{\"error\":\"Unsupported language 'xx'\",\"supportedLanguages\":\"en, ar, fr, es, de\"}",
        response.body());
  }

  @Test
  void quotesInNamesAreEscapedInJson() throws Exception {
    CapturedResponse response = response();

    servlet.doGet(request("/api/greet", Map.of("name", "Al \"The Coder\"")), response.mock());

    assertEquals(
        "{\"name\":\"Al \\\"The Coder\\\"\",\"language\":\"en\","
            + "\"message\":\"Hello Al \\\"The Coder\\\", how are you?\"}",
        response.body());
  }
}
