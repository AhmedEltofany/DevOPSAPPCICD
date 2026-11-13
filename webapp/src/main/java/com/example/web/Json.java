package com.example.web;

import java.util.Map;

/**
 * A tiny JSON writer so the sample app stays dependency free.
 *
 * <p>Supports {@code null}, strings, numbers, booleans and nested {@link Map}s - enough for
 * the API responses in this project. In a real service you would use Jackson or JSON-B.
 */
final class Json {

  private Json() {
  }

  /** Serialises a map (keys in iteration order - pass a LinkedHashMap to control order). */
  static String object(Map<String, ?> fields) {
    StringBuilder json = new StringBuilder("{");
    boolean first = true;
    for (Map.Entry<String, ?> field : fields.entrySet()) {
      if (!first) {
        json.append(',');
      }
      first = false;
      json.append(quote(field.getKey())).append(':').append(value(field.getValue()));
    }
    return json.append('}').toString();
  }

  @SuppressWarnings("unchecked")
  private static String value(Object value) {
    if (value == null) {
      return "null";
    }
    if (value instanceof Number || value instanceof Boolean) {
      return value.toString();
    }
    if (value instanceof Map) {
      return object((Map<String, ?>) value);
    }
    return quote(value.toString());
  }

  /** Wraps a string in quotes, escaping everything JSON requires. */
  static String quote(String text) {
    StringBuilder out = new StringBuilder(text.length() + 2).append('"');
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      switch (c) {
        case '"':
          out.append("\\\"");
          break;
        case '\\':
          out.append("\\\\");
          break;
        case '\n':
          out.append("\\n");
          break;
        case '\r':
          out.append("\\r");
          break;
        case '\t':
          out.append("\\t");
          break;
        default:
          if (c < 0x20) {
            out.append(String.format("\\u%04x", (int) c));
          } else {
            out.append(c);
          }
      }
    }
    return out.append('"').toString();
  }
}
