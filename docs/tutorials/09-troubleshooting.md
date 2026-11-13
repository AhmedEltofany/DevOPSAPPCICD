# 9. Troubleshooting

## Build

| Symptom | Cause | Fix |
|---|---|---|
| `Rule 1: RequireJavaVersion failed ... needs JDK 17` | Old JDK on PATH | Install JDK 17+, check `mvn -version` (it shows the JDK Maven uses) |
| `Coverage checks have not been met ... lines covered ratio is 0.65, but expected minimum is 0.70` | New code without tests | Open `*/target/site/jacoco/index.html`, test the red lines |
| `Could not resolve dependencies ... com.example.maven-project:server` when running `jetty:run` | `server` module not installed | `mvn install -DskipTests` first, then `mvn -pl webapp jetty:run` |
| `/api/info` shows `"commit":"local"` | `-Dbuild.commit` not passed | Pipelines pass it automatically; locally add `-Dbuild.commit=$(git rev-parse --short HEAD)` |

## Docker

| Symptom | Cause | Fix |
|---|---|---|
| `the --mount option requires BuildKit` | Old Docker or BuildKit disabled | `DOCKER_BUILDKIT=1 docker build ...` or upgrade Docker |
| `COPY failed: ... webapp/target/webapp.war: not found` with `AzureDockerfile` | WAR not built | Run `mvn package` first, or use the multi-stage `Dockerfile` |
| Container `unhealthy` | App failed to start | `docker logs <container>`; look for exceptions after `Deploying web application archive` |
| `ClassNotFoundException: javax.servlet...` | Running an old WAR on Tomcat 10+ | Tomcat 10+ uses `jakarta.*`; rebuild with this version of the code |
| Port 8080 already in use | Another app/container | `docker ps`, stop it, or map `-p 9090:8080` |

## Azure Pipelines

| Symptom | Cause | Fix |
|---|---|---|
| `The pipeline is not valid. ... service connection ... could not be found` | `enableDeploy: true` but the connection names don't match | Names in Project settings must equal the variables exactly |
| Run waits with "This pipeline needs permission to access a resource" | First use of a service connection or environment | Click **View → Permit** (one time) |
| Prod stage waits forever | Approval check on `devopsappcicd-prod` | Approve it in the run, or remove the check |
| Smoke test fails right after deploy | App Service still pulling the image | The script retries `/health` for ~2.5 minutes; check **Deployment Center → Logs** on the Web App |
| Web App shows "Application Error" | Image tag missing in ACR or `AcrPull` not assigned yet | `az acr repository show-tags -n <acr> --repository devopsappcicd`; re-run the Bicep deployment |

## GitHub Actions

| Symptom | Cause | Fix |
|---|---|---|
| CI didn't run | Only docs/markdown changed (`paths-ignore`) | Expected - run it manually with **Run workflow** |
| `denied: permission_denied` pushing to ghcr.io | Workflow permissions | Repo **Settings → Actions → General → Workflow permissions → Read and write** |
| `kubeconform` errors | Invalid field in a manifest | Run `kubectl kustomize k8s/overlays/dev` locally and read the line it reports |

## Jenkins

| Symptom | Cause | Fix |
|---|---|---|
| `Tool type "maven" does not have an install of "maven-3.9" configured` | Tool names | **Manage Jenkins → Tools**: names must be `jdk-17` and `maven-3.9` |
| `docker: not found` / `permission denied ... docker.sock` | Docker CLI or socket access missing | See [tutorial 6, step 1](06-jenkins.md#step-1---start-jenkins-with-docker-access) |
| `No such DSL method 'recordCoverage'` | Coverage plugin not installed | Install **Coverage** or delete that line |
| Smoke test `Connection refused` | Jenkins in Docker calling `localhost` | Set `SMOKE_HOST=host.docker.internal` |

## Kubernetes

| Symptom | Cause | Fix |
|---|---|---|
| `ErrImagePull` / `ImagePullBackOff` in dev | Image not loaded into the cluster | `minikube image load devopsappcicd:local` |
| `CreateContainerConfigError: container has runAsNonRoot and image has non-numeric user` | Old image without `USER 10001` | Rebuild the image from this repo |
| Pod `0/1 READY` forever | Readiness failing | `kubectl describe pod ...` → Events; check `APP_READY` in the ConfigMap |
| HPA shows `<unknown>/70%` | metrics-server missing | `minikube addons enable metrics-server` |

## Still stuck?

Hit the endpoints directly - they tell you a lot:

```bash
curl -i <url>/health/ready      # is it ready?
curl -s <url>/api/info          # which version/commit/environment is really running?
```
