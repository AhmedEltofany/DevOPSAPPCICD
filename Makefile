# Shortcuts for the most common commands. Run `make help` to list them.
# Windows users: run these from Git Bash or WSL, or copy the command after the colon.

IMAGE ?= devopsappcicd
TAG   ?= local
URL   ?= http://localhost:8080
MVN   ?= mvn -B -ntp

.DEFAULT_GOAL := help
.PHONY: help build test verify coverage run docker-build docker-build-prebuilt docker-run \
        compose-up compose-down smoke k8s-dev k8s-prod k8s-render clean

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | \
	  awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-22s\033[0m %s\n", $$1, $$2}'

build: ## Compile and package the WAR (skips tests)
	$(MVN) package -DskipTests

test: ## Run unit tests
	$(MVN) test

verify: ## Full build: tests + coverage quality gate (what CI runs)
	$(MVN) verify

coverage: verify ## Build and print where the coverage reports are
	@echo "Coverage reports:"; ls -1 */target/site/jacoco/index.html

run: ## Run the app with Jetty at http://localhost:8080 (no Docker needed)
	$(MVN) install -DskipTests
	$(MVN) -pl webapp jetty:run

docker-build: ## Build the image with the multi-stage Dockerfile
	docker build --build-arg GIT_COMMIT=$$(git rev-parse --short HEAD 2>/dev/null || echo local) \
	  -t $(IMAGE):$(TAG) .

docker-build-prebuilt: build ## Build the WAR with Maven, then the image with AzureDockerfile
	docker build -f AzureDockerfile -t $(IMAGE):$(TAG) .

docker-run: ## Run the image on port 8080
	docker run --rm -p 8080:8080 -e APP_ENV=local-docker --name $(IMAGE) $(IMAGE):$(TAG)

compose-up: ## Start with Docker Compose (detached)
	docker compose up --build -d

compose-down: ## Stop Docker Compose
	docker compose down

smoke: ## Smoke test a running app (URL=http://... to override)
	bash scripts/smoke-test.sh $(URL)

k8s-render: ## Print the Kubernetes manifests for dev and prod
	kubectl kustomize k8s/overlays/dev
	@echo "---"
	kubectl kustomize k8s/overlays/prod

k8s-dev: ## Deploy to the current kubectl context (dev overlay)
	kubectl apply -k k8s/overlays/dev

k8s-prod: ## Deploy to the current kubectl context (prod overlay)
	kubectl apply -k k8s/overlays/prod

clean: ## Remove build output
	$(MVN) clean
