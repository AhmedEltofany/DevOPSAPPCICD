package com.example.web;

import static com.example.web.ServletTestSupport.request;
import static com.example.web.ServletTestSupport.response;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import com.example.web.ServletTestSupport.CapturedResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HelloServletTest {

  private final HelloServlet servlet = new HelloServlet();

  @Test
  void rendersGreetingAsHtml() throws Exception {
    CapturedResponse response = response();

    servlet.doGet(request("/hello", Map.of("name", "Ahmed")), response.mock());

    verify(response.mock()).setContentType("text/html");
    assertTrue(response.body().contains("<h1 id=\"greeting\">Hello Ahmed, how are you?</h1>"));
    assertTrue(response.body().contains("dir=\"ltr\""));
  }

  @Test
  void arabicIsRenderedRightToLeft() throws Exception {
    CapturedResponse response = response();

    servlet.doGet(request("/hello", Map.of("name", "Ahmed", "lang", "ar")), response.mock());

    assertTrue(response.body().contains("dir=\"rtl\""));
    assertTrue(response.body().contains("Language: Arabic"));
  }

  @Test
  void unknownLanguageFallsBackToEnglish() throws Exception {
    CapturedResponse response = response();

    servlet.doGet(request("/hello", Map.of("lang", "klingon")), response.mock());

    assertTrue(response.body().contains("Hello World, how are you?"));
  }

  @Test
  void escapesHtmlToPreventXss() throws Exception {
    CapturedResponse response = response();

    servlet.doGet(request("/hello", Map.of("name", "<script>alert(1)</script>")),
        response.mock());

    assertFalse(response.body().contains("<script>"), "raw script tag must never be echoed");
    assertTrue(response.body().contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
  }
}
