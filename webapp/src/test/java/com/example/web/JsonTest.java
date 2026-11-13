package com.example.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonTest {

  @Test
  void writesAllSupportedValueTypes() {
    Map<String, Object> nested = new LinkedHashMap<>();
    nested.put("ok", true);

    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("text", "hi");
    fields.put("number", 42);
    fields.put("missing", null);
    fields.put("nested", nested);

    assertEquals("{\"text\":\"hi\",\"number\":42,\"missing\":null,\"nested\":{\"ok\":true}}",
        Json.object(fields));
  }

  @Test
  void escapesSpecialCharacters() {
    assertEquals("\"a\\\"b\\\\c\\nd\\re\\tf\\u0001\"", Json.quote("a\"b\\c\nd\re\tf\u0001"));
  }

  @Test
  void keepsUnicodeAsIs() {
    assertEquals("\"مرحباً\"", Json.quote("مرحباً"));
  }

  @Test
  void emptyObject() {
    assertEquals("{}", Json.object(Map.of()));
  }

  @Test
  void escapeHtmlCoversAllDangerousCharacters() {
    assertEquals("&lt;a href=&quot;x&quot; title=&#39;y&#39;&gt;&amp;&lt;/a&gt;",
        Responses.escapeHtml("<a href=\"x\" title='y'>&</a>"));
  }
}
