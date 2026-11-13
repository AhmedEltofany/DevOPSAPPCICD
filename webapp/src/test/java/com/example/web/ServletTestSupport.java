package com.example.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Map;

/** Small helpers so each servlet test reads like "given request -> expect body". */
final class ServletTestSupport {

  private ServletTestSupport() {
  }

  /** A mocked GET request with the given query parameters and servlet path. */
  static HttpServletRequest request(String servletPath, Map<String, String> params) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getServletPath()).thenReturn(servletPath);
    params.forEach((name, value) -> when(request.getParameter(name)).thenReturn(value));
    return request;
  }

  /** A mocked response whose body is captured into {@link CapturedResponse#body()}. */
  static CapturedResponse response() {
    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter body = new StringWriter();
    try {
      when(response.getWriter()).thenReturn(new PrintWriter(body, true));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return new CapturedResponse(response, body);
  }

  record CapturedResponse(HttpServletResponse mock, StringWriter writer) {
    String body() {
      return writer.toString();
    }
  }
}
