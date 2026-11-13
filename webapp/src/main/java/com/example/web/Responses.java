package com.example.web;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Helpers shared by the servlets for writing responses. */
final class Responses {

  private Responses() {
  }

  /** Writes a JSON body with the given status code. Responses are never cached. */
  static void json(HttpServletResponse response, int status, Map<String, ?> body)
      throws IOException {
    response.setStatus(status);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType("application/json");
    response.setHeader("Cache-Control", "no-store");
    response.getWriter().write(Json.object(body));
  }

  /** Escapes text for safe use inside HTML, preventing cross-site scripting (XSS). */
  static String escapeHtml(String text) {
    StringBuilder out = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      switch (c) {
        case '<':
          out.append("&lt;");
          break;
        case '>':
          out.append("&gt;");
          break;
        case '&':
          out.append("&amp;");
          break;
        case '"':
          out.append("&quot;");
          break;
        case '\'':
          out.append("&#39;");
          break;
        default:
          out.append(c);
      }
    }
    return out.toString();
  }
}
