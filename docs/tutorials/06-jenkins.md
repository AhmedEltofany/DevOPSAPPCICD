# 6. Jenkins pipeline

**Goal:** run Jenkins locally, configure tools and credentials, and run the `Jenkinsfile` as a
multibranch pipeline.
**Time:** 45 minutes. **Needs:** Docker.

## Step 1 - Start Jenkins with Docker access

The pipeline builds Docker images, so Jenkins needs a Docker CLI and access to a Docker daemon.
The simplest learning setup shares the host's Docker socket:

```bash
docker network create jenkins
docker run -d --name jenkins --network jenkins --restart unless-stopped \
  -p 8081:8080 -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  --group-add $(stat -c '%g' /var/run/docker.sock) \
  jenkins/jenkins:lts-jdk17

docker exec -u root jenkins bash -c "apt-get update && apt-get install -y docker.io curl"
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Open <http://localhost:8081>, paste the password, install the suggested plugins.

> Mounting the Docker socket gives Jenkins root-equivalent access to the host. Fine on a laptop;
> in production use dedicated build agents.

On Docker Desktop (Mac/Windows) skip `--group-add`; the socket is already accessible.

## Step 2 - Install plugins

**Manage Jenkins → Plugins → Available:** `Docker Pipeline`, `Coverage`, `Timestamper`
(`Pipeline`, `Git` and `JUnit` come with the suggested plugins).

## Step 3 - Configure tools

**Manage Jenkins → Tools**

| Section | Name (must match the Jenkinsfile) | Setting |
|---|---|---|
| JDK installations | `jdk-17` | Install automatically → Adoptium 17 |
| Maven installations | `maven-3.9` | Install automatically → 3.9.x |

## Step 4 - Add registry credentials (optional - only to push images)

**Manage Jenkins → Credentials → System → Global → Add**

* Kind: *Username with password*
* ID: `docker-registry`
* Username / password: your Docker Hub user and an **access token** (not your password)

## Step 5 - Create the pipeline

**New Item → Multibranch Pipeline**

* Branch Sources → GitHub → `https://github.com/AhmedEltofany/DevOPSAPPCICD.git`
* Build Configuration → by Jenkinsfile → `Jenkinsfile`
* Save. Jenkins scans branches and starts a build for each one that has a `Jenkinsfile`.

## Step 6 - Run with parameters

The first build registers the parameters. Then **Build with Parameters**:

| Parameter | Value for this tutorial |
|---|---|
| `REGISTRY` | `docker.io/<your-user>` |
| `PUSH_IMAGE` | `false` first, `true` when credentials are set up |
| `DEPLOY` | `true` |
| `SMOKE_HOST` | `host.docker.internal` (Jenkins runs in a container, the app is a sibling container) |

On Linux, `host.docker.internal` needs `--add-host=host.docker.internal:host-gateway` on the Jenkins
container; alternatively use the host's IP address.

## What each stage does

| Stage | Notes |
|---|---|
| Checkout | Saves the short commit SHA, passed to Maven as `-Dbuild.commit` |
| Build & Test | `mvn clean verify`; `junit` publishes test trends, `recordCoverage` the JaCoCo report, the WAR is archived with a fingerprint |
| Docker Build | `AzureDockerfile` - packages the WAR we just tested |
| Docker Push | Only when `PUSH_IMAGE` is ticked and it is not a PR; credentials are masked in logs |
| Deploy | Replaces the `devopsappcicd-jenkins` container on port 8085 |
| Smoke Test | `scripts/smoke-test.sh` - same checks as Azure and GitHub |

Open <http://localhost:8085/api/info> - `"environment":"jenkins"` and the commit you built.

## Exercises

1. Add a GitHub webhook (`http://<jenkins>/github-webhook/`) so pushes trigger builds instantly.
   (Use a tunnel such as ngrok for a local Jenkins.)
2. Replace `agent any` with `agent { docker { image 'maven:3.9-eclipse-temurin-17' } }` for the
   build stage. What do you lose and gain?
3. Add an `input` step before Deploy that asks "Deploy to production?" and times out after 10 minutes.

Next: [7. Kubernetes with Kustomize](07-kubernetes.md)
