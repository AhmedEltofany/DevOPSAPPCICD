# 3. Docker and Docker Compose

**Goal:** understand both Dockerfiles, run the app in a container, and see health checks and
non-root users in action.
**Time:** 25 minutes. **Needs:** Docker Desktop or Docker Engine with BuildKit (default since Docker 23).

## Two Dockerfiles - why?

| File | Builds the code? | Used by | Why |
|---|---|---|---|
| `Dockerfile` | Yes, inside Docker (multi-stage) | You, Compose, GitHub Actions CI | Only Docker needed; reproducible anywhere |
| `AzureDockerfile` | No, copies `webapp/target/webapp.war` | Azure Pipelines, Jenkins, GitHub release | CI already compiled and tested the WAR - don't do it twice |

Both produce the same runtime image: Tomcat 10.1 + JRE 17, app served at `/`, running as UID `10001`.

## Step 1 - Build with the multi-stage Dockerfile

```bash
docker build --build-arg GIT_COMMIT=$(git rev-parse --short HEAD) -t devopsappcicd:local .
```

Read `Dockerfile` while it builds:

* **Stage 1 (`build`)** uses the `maven` image, copies the POMs and sources, runs `mvn package`.
  `RUN --mount=type=cache,target=/root/.m2` keeps downloaded dependencies between builds -
  run the build twice and compare the times.
* **Stage 2** starts from a clean `tomcat` image and copies **only** the WAR from stage 1
  (`COPY --from=build`). Maven, the JDK and the source code never reach the final image.

```bash
docker image ls devopsappcicd      # compare with: docker image ls maven
docker history devopsappcicd:local
```

## Step 2 - Run it

```bash
docker run -d --name app -p 8080:8080 -e APP_ENV=my-laptop devopsappcicd:local
docker ps                                  # STATUS shows (health: starting) then (healthy)
curl -s localhost:8080/api/info            # "environment":"my-laptop", "hostname":"<container id>"
bash scripts/smoke-test.sh http://localhost:8080 my-laptop
```

### Health check

`HEALTHCHECK` calls `/health/live` every 15 seconds. Watch it:

```bash
docker inspect --format '{{json .State.Health}}' app | python3 -m json.tool
```

### Non-root

```bash
docker exec app id          # uid=10001(app) gid=10001(app)
```

If an attacker breaks into the app, they are not root inside the container. Kubernetes enforces
this too (`runAsNonRoot: true` in `k8s/base/deployment.yaml`).

### Logs and cleanup

```bash
docker logs -f app          # Ctrl+C to stop following
docker rm -f app
```

## Step 3 - The CI way (pre-built WAR)

```bash
mvn -B package                               # or: make build
docker build -f AzureDockerfile -t devopsappcicd:prebuilt .
```

`.dockerignore` excludes every `target/` folder **except** `webapp/target/webapp.war`, so the build
context stays tiny.

## Step 4 - Docker Compose

```bash
docker compose up --build -d
docker compose ps
docker compose logs -f app
```

### Try readiness

Edit `docker-compose.yml`, set `APP_READY: "false"`, then:

```bash
docker compose up -d
curl -i localhost:8080/health/ready     # 503 {"status":"DOWN",...}
curl -i localhost:8080/health/live      # 200 - the process is alive, just not taking traffic
```

This is exactly what Kubernetes does with readiness vs liveness probes ([tutorial 7](07-kubernetes.md)).
Set it back to `"true"`.

### Scale out

Remove the `ports:` mapping (two containers cannot both bind host port 8080), then:

```bash
docker compose up -d --scale app=3
docker compose ps
docker compose down
```

## Step 5 - Push to a registry

```bash
# Docker Hub
docker tag devopsappcicd:local <your-user>/devopsappcicd:1.0
docker login
docker push <your-user>/devopsappcicd:1.0

# Azure Container Registry
az acr login --name <acrName>
docker tag devopsappcicd:local <acrName>.azurecr.io/devopsappcicd:1.0
docker push <acrName>.azurecr.io/devopsappcicd:1.0
```

## Exercises

1. Remove `--mount=type=cache...` from the Dockerfile and time two consecutive builds. Put it back.
2. Run `docker run --rm -m 300m devopsappcicd:local` and check the JVM heap:
   `docker exec <id> java -XX:+PrintFlagsFinal -version | grep MaxHeapSize`. Why does `MaxRAMPercentage` matter?
3. Scan your image: `docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy image devopsappcicd:local`.

Next: [4. Azure Pipelines](04-azure-pipelines.md)
