package com.example.web;

import com.example.Greeter;
import com.example.Language;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Human friendly greeting page.
 *
 * <pre>
 *   GET /hello                      -> "Hello World, how are you?"
 *   GET /hello?name=Ahmed&amp;lang=ar   -> Arabic greeting, right-to-left
 * </pre>
 *
 * <p>Unknown languages fall back to English here (a page should never break), while the JSON
 * API in {@link GreetApiServlet} answers 400 so API clients learn about their mistake.
 */
@WebServlet(name = "hello", urlPatterns = "/hello")
public class HelloServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  private final transient Greeter greeter = new Greeter();

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    Language language = Language.fromCode(request.getParameter("lang")).orElse(Language.EN);
    String greeting = greeter.greet(request.getParameter("name"), language);
    String direction = language == Language.AR ? "rtl" : "ltr";

    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    out.println("<!DOCTYPE html>");
    out.println("<html lang=\"" + language.code() + "\" dir=\"" + direction + "\">");
    out.println("<head><meta charset=\"UTF-8\"><title>Greeting</title>");
    out.println("<link rel=\"stylesheet\" href=\"style.css\"></head>");
    out.println("<body><main class=\"card\">");
    out.println("<h1 id=\"greeting\">" + Responses.escapeHtml(greeting) + "</h1>");
    out.println("<p>Language: " + language.displayName() + "</p>");
    out.println("<p><a href=\"./\">&larr; Back</a></p>");
    out.println("</main></body></html>");
  }
}
