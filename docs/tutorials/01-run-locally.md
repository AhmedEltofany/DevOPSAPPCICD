# 1. Run and test the app locally

**Goal:** build the project, run the tests, read the coverage report and call every endpoint.
**Time:** 15 minutes.

## Prerequisites

| Tool | Version | Check |
|---|---|---|
| JDK | 17 or newer | `java -version` |
| Maven | 3.6.3 or newer | `mvn -version` |
| curl | any | `curl --version` |

No Java? Skip to [tutorial 3](03-docker.md) - Docker can build everything for you.

## Step 1 - Understand the modules

```
server/   plain Java: Greeter, Language          -> server.jar
webapp/   servlets + JSP, depends on server       -> webapp.war
```

Keeping business logic (`server`) away from web code (`webapp`) means most tests run in milliseconds
without starting Tomcat. This is the same idea as keeping a "domain" layer separate in any architecture.

## Step 2 - Build and test

```bash
mvn verify
```

What happens, in order (Maven *lifecycle phases*):

1. `validate` - the **enforcer** plugin checks you have JDK 17+ and a recent Maven.
2. `compile` - compiles `src/main/java`.
3. `process-resources` - copies `build-info.properties`, replacing `${project.version}` etc. (**filtering**).
4. `test` - runs JUnit 5 tests; **JaCoCo** records which lines ran.
5. `package` - creates `server/target/server.jar` and `webapp/target/webapp.war`.
6. `verify` - writes the coverage report and **fails the build if line coverage < 70%**.

Expected ending:

```
[INFO] Server ............................................. SUCCESS
[INFO] Webapp ............................................. SUCCESS
[INFO] BUILD SUCCESS
```

## Step 3 - Read the reports

* Test results: `server/target/surefire-reports/` and `webapp/target/surefire-reports/`
* Coverage (open in a browser): `server/target/site/jacoco/index.html`, `webapp/target/site/jacoco/index.html`

Green lines ran during tests, red lines did not, yellow means only some branches of an `if` ran.

Useful variations:

```bash
mvn test -pl server                         # only the server module
mvn test -Dtest=GreeterTest                 # one test class
mvn test -Dtest='GreeterTest#defaultsToEnglish'
mvn verify -Djacoco.minimum.coverage=0.90   # stricter gate - watch it fail
mvn package -DskipTests                     # fast package, no tests
```

## Step 4 - Run the app

```bash
mvn install -DskipTests          # puts server.jar in ~/.m2 so webapp can find it
mvn -pl webapp jetty:run
```

Open <http://localhost:8080/>. Jetty rescans every 2 seconds, so edits to JSP/CSS show up on refresh.

## Step 5 - Call the endpoints

```bash
curl -s "localhost:8080/api/greet?name=Ahmed"
curl -s "localhost:8080/api/greet?name=Ahmed&lang=ar"
curl -s -i "localhost:8080/api/greet?lang=xx"          # 400 Bad Request
curl -s "localhost:8080/api/info"
curl -s "localhost:8080/health/ready"
```

Then run the same checks the pipelines run after every deployment:

```bash
bash scripts/smoke-test.sh http://localhost:8080
```

## Step 6 - Override build metadata

```bash
mvn -q package -DskipTests -Dbuild.commit=$(git rev-parse --short HEAD)
unzip -p webapp/target/webapp.war WEB-INF/classes/build-info.properties
```

CI servers pass the real commit SHA the same way, so `/api/info` always tells you exactly which
commit is running in each environment.

## Exercises

1. Make a test fail on purpose (change `"Hello %s, how are you?"` in `Language.java`) and read the failure message.
2. Delete a test from `JsonTest` and run `mvn verify`. Does the coverage gate still pass? Why?
3. Run `mvn dependency:tree -pl webapp` and find where `server` comes from.

Next: [2. Add a feature end to end](02-add-a-feature.md)
