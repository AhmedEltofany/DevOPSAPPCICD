# 2. Add a feature end to end (TDD)

**Goal:** add a new language and a new endpoint, test-first, and ship it through CI.
**Time:** 30 minutes.

You will follow the loop every change in a CI/CD team follows:

```
branch -> failing test -> code -> green tests -> push -> CI -> review -> merge -> deploy
```

## Part A - Add Italian (a one-file change)

### 1. Create a branch

```bash
git checkout -b feature/italian
```

### 2. Write the failing test first

Add a row to `LanguageTest.findsLanguageByCodeIgnoringCaseAndSpaces`:

```java
"it,    IT",
```

and update `listsSupportedCodes`:

```java
assertEquals("en, ar, fr, es, de, it", Language.supportedCodes());
```

Run `mvn test -pl server` - it fails (that is the point: the test proves the feature is missing).

### 3. Make it pass

In `Language.java` add one constant:

```java
DE("de", "German", "Hallo %s, wie geht es dir?"),
IT("it", "Italian", "Ciao %s, come stai?");
```

Run `mvn test -pl server` again - green. Notice that `everyLanguageGreetsByName` and
`everyTemplateHasExactlyOnePlaceholder` automatically test the new language too, because they use
`@EnumSource`. Good tests cover future code for free.

The landing page dropdown also picks it up automatically (`index.jsp` loops over `Language.values()`).

## Part B - Add a `/api/languages` endpoint

Target response:

```json
{"count":6,"languages":{"en":"English","ar":"Arabic","fr":"French","es":"Spanish","de":"German","it":"Italian"}}
```

### 1. Test first - `webapp/src/test/java/com/example/web/LanguagesServletTest.java`

```java
package com.example.web;

import static com.example.web.ServletTestSupport.request;
import static com.example.web.ServletTestSupport.response;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import com.example.web.ServletTestSupport.CapturedResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LanguagesServletTest {

  @Test
  void listsAllLanguages() throws Exception {
    CapturedResponse response = response();

    new LanguagesServlet().doGet(request("/api/languages", Map.of()), response.mock());

    verify(response.mock()).setStatus(HttpServletResponse.SC_OK);
    assertTrue(response.body().contains("\"en\":\"English\""), response.body());
    assertTrue(response.body().contains("\"ar\":\"Arabic\""), response.body());
  }
}
```

### 2. Implement - `webapp/src/main/java/com/example/web/LanguagesServlet.java`

```java
package com.example.web;

import com.example.Language;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet(name = "languages", urlPatterns = "/api/languages")
public class LanguagesServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    Map<String, Object> languages = new LinkedHashMap<>();
    for (Language language : Language.values()) {
      languages.put(language.code(), language.displayName());
    }
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("count", languages.size());
    body.put("languages", languages);
    Responses.json(response, HttpServletResponse.SC_OK, body);
  }
}
```

No `web.xml` change is needed - `@WebServlet` registers the URL.

### 3. Add it to the smoke test

In `scripts/smoke-test.sh`, next to the other checks:

```bash
check "/api/languages" "\"en\":\"English\""
```

Now every pipeline verifies the endpoint after each deployment.

### 4. Verify and ship

```bash
mvn verify
mvn install -DskipTests && mvn -pl webapp jetty:run   # try http://localhost:8080/api/languages
git add -A && git commit -m "Add Italian and /api/languages"
git push -u origin feature/italian
```

Open a pull request. GitHub Actions (and Azure Pipelines, if connected) run on the PR.
Merge only when the checks are green.

## What you practised

* **Test-first**: the failing test defines "done".
* **Small, safe change**: separate logic (`server`) from delivery (`webapp`).
* **Pipeline as safety net**: unit tests, coverage gate, Docker smoke test and K8s validation all run for you.
* **Smoke tests grow with the app**: each endpoint you add gets a post-deploy check.

## Exercises

1. Add `GET /api/greet` support for a `uppercase=true` query parameter. Test first.
2. Make `/api/languages` return `405 Method Not Allowed` for `POST` (hint: that is already the default - write a test that proves it).
3. Lower `jacoco.minimum.coverage` is tempting when a build fails. Instead, find the uncovered lines in the HTML report and test them.

Next: [3. Docker and Docker Compose](03-docker.md)
