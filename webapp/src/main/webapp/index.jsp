<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="com.example.Language" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>DevOps App CI/CD</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
<main class="card">
  <h1>DevOps App CI/CD</h1>
  <p class="lead">
    A small Java web app built, tested, containerised and deployed by
    Azure Pipelines, GitHub Actions and Jenkins.
  </p>

  <h2>Try it</h2>
  <form action="hello" method="get" class="row">
    <input type="text" name="name" placeholder="Your name" maxlength="50" aria-label="Your name">
    <select name="lang" aria-label="Language">
      <% for (Language language : Language.values()) { %>
        <option value="<%= language.code() %>"><%= language.displayName() %></option>
      <% } %>
    </select>
    <button type="submit">Greet me</button>
  </form>

  <h2>Endpoints</h2>
  <table>
    <tr><th>Path</th><th>What it shows</th></tr>
    <tr><td><a href="hello?name=Ahmed">/hello?name=Ahmed</a></td><td>HTML greeting page</td></tr>
    <tr><td><a href="api/greet?name=Ahmed&amp;lang=ar">/api/greet?name=Ahmed&amp;lang=ar</a></td><td>JSON greeting API</td></tr>
    <tr><td><a href="api/info">/api/info</a></td><td>Version, Git commit, environment, hostname</td></tr>
    <tr><td><a href="health">/health</a></td><td>Overall health</td></tr>
    <tr><td><a href="health/live">/health/live</a></td><td>Liveness probe</td></tr>
    <tr><td><a href="health/ready">/health/ready</a></td><td>Readiness probe</td></tr>
  </table>

  <p class="muted">
    Originally built while following the Rahul Shetty Academy
    "QA Automation + DevOps CI/CD" course, then extended with more pipelines and tutorials.
  </p>
</main>
</body>
</html>
