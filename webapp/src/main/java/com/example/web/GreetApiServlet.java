package com.example.web;

import com.example.Greeter;
import com.example.Language;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JSON greeting API.
 *
 * <pre>
 *   GET /api/greet?name=Ahmed&amp;lang=fr
 *   200 {"name":"Ahmed","language":"fr","message":"Bonjour Ahmed, comment allez-vous ?"}
 *
 *   GET /api/greet?lang=xx
 *   400 {"error":"Unsupported language 'xx'","supportedLanguages":"en, ar, fr, es, de"}
 * </pre>
 */
@WebServlet(name = "greetApi", urlPatterns = "/api/greet")
public class GreetApiServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  private final transient Greeter greeter = new Greeter();

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String langParam = request.getParameter("lang");
    Optional<Language> language = langParam == null || langParam.isBlank()
        ? Optional.of(greeter.defaultLanguage())
        : Language.fromCode(langParam);

    if (language.isEmpty()) {
      Map<String, Object> error = new LinkedHashMap<>();
      error.put("error", "Unsupported language '" + langParam.strip() + "'");
      error.put("supportedLanguages", Language.supportedCodes());
      Responses.json(response, HttpServletResponse.SC_BAD_REQUEST, error);
      return;
    }

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("name", Greeter.normalizeName(request.getParameter("name")));
    body.put("language", language.get().code());
    body.put("message", greeter.greet(request.getParameter("name"), language.get()));
    Responses.json(response, HttpServletResponse.SC_OK, body);
  }
}
