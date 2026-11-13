# 7. Kubernetes with Kustomize

**Goal:** deploy the app to a local cluster, see probes, rolling updates and autoscaling, and
understand base/overlay configuration.
**Time:** 45 minutes. **Needs:** Docker, `kubectl`, and one of minikube / kind / Docker Desktop Kubernetes.

## Layout

```
k8s/
├── base/                    # shared by every environment
│   ├── deployment.yaml      # probes, resources, non-root security context
│   ├── service.yaml         # ClusterIP :80 -> :8080
│   ├── hpa.yaml             # CPU-based autoscaling
│   └── kustomization.yaml   # + ConfigMap (APP_ENV, APP_READY)
└── overlays/
    ├── dev/                 # 1 replica, local image, APP_ENV=dev
    └── prod/                # 3 replicas, GHCR image, Ingress, PodDisruptionBudget, APP_ENV=prod
```

Overlays only describe **differences**. Render them to see the final YAML:

```bash
kubectl kustomize k8s/overlays/dev
kubectl kustomize k8s/overlays/prod
```

## Step 1 - Start a cluster and load the image

```bash
minikube start
minikube addons enable metrics-server          # for the HPA
docker build -t devopsappcicd:local .
minikube image load devopsappcicd:local        # kind: kind load docker-image devopsappcicd:local
```

(Docker Desktop's Kubernetes sees local images directly - skip `image load`.)

## Step 2 - Deploy dev

```bash
kubectl apply -k k8s/overlays/dev
kubectl -n devopsappcicd-dev get pods -w        # wait for 1/1 Running, then Ctrl+C
kubectl -n devopsappcicd-dev port-forward svc/devopsappcicd 8080:80
```

In another terminal:

```bash
bash scripts/smoke-test.sh http://localhost:8080 dev
```

## Step 3 - Probes in action

The Deployment has three probes:

| Probe | Path | If it fails |
|---|---|---|
| `startupProbe` | `/health/live` | Gives Tomcat up to 2 minutes to start before other probes run |
| `livenessProbe` | `/health/live` | Kubernetes **restarts** the container |
| `readinessProbe` | `/health/ready` | Kubernetes **stops sending traffic** to the pod (no restart) |

Make the pod "not ready" without killing it:

```bash
kubectl -n devopsappcicd-dev set env deploy/devopsappcicd APP_READY=false
kubectl -n devopsappcicd-dev get pods -w     # new pod stays 0/1 READY; old pod keeps serving
```

Because the rollout uses `maxUnavailable: 0`, Kubernetes keeps the old healthy pod until the new
one is ready - which never happens - so users are not affected. Undo it:

```bash
kubectl -n devopsappcicd-dev rollout undo deploy/devopsappcicd
```

## Step 4 - Rolling update and rollback

```bash
docker build --build-arg GIT_COMMIT=v2 -t devopsappcicd:v2 .
minikube image load devopsappcicd:v2
kubectl -n devopsappcicd-dev set image deploy/devopsappcicd app=devopsappcicd:v2
kubectl -n devopsappcicd-dev rollout status deploy/devopsappcicd
curl -s localhost:8080/api/info                # restart port-forward if needed; commit = v2
kubectl -n devopsappcicd-dev rollout history deploy/devopsappcicd
kubectl -n devopsappcicd-dev rollout undo deploy/devopsappcicd
```

## Step 5 - Autoscaling

```bash
kubectl -n devopsappcicd-dev get hpa
# generate load from inside the cluster
kubectl -n devopsappcicd-dev run load --rm -it --image=busybox --restart=Never -- \
  sh -c 'while true; do wget -q -O- http://devopsappcicd/api/greet >/dev/null; done'
kubectl -n devopsappcicd-dev get hpa -w         # in another terminal
```

The dev overlay caps it at 2 replicas; prod allows 3-10.

## Step 6 - Prod overlay

```bash
kubectl kustomize k8s/overlays/prod | less
```

It adds an Ingress (`ingressClassName: nginx`, host `devopsappcicd.example.com`), a
PodDisruptionBudget (`minAvailable: 2`) and pulls from `ghcr.io/ahmedeltofany/devopsappcicd`
(published by the [release workflow](05-github-actions.md)).

Pin the exact tested version in a pipeline before applying:

```bash
cd k8s/overlays/prod
kustomize edit set image devopsappcicd=ghcr.io/ahmedeltofany/devopsappcicd:1.0.0
kubectl apply -k .
```

## Clean up

```bash
kubectl delete -k k8s/overlays/dev
minikube stop
```

## Exercises

1. Add a `staging` overlay with 2 replicas and `APP_ENV=staging`.
2. Change `APP_ENV` in the dev overlay and re-apply. Why do the pods restart automatically?
   (Hint: look at the ConfigMap name - Kustomize adds a content hash.)
3. Deploy to AKS: `az aks get-credentials ...`, push the image to ACR, and set the image in the prod overlay.

Next: [8. Azure infrastructure with Bicep](08-azure-infra-bicep.md)
