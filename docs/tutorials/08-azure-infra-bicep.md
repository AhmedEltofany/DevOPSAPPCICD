# 8. Azure infrastructure with Bicep

**Goal:** create everything the Azure pipeline deploys to - from code, repeatably - for dev and prod.
**Time:** 20 minutes. **Needs:** Azure CLI (`az`) logged in to a subscription.

## What `infra/azure/main.bicep` creates

| Resource | Purpose |
|---|---|
| Container Registry (Basic) | Stores images pushed by the pipeline |
| App Service plan (Linux) | `B1` for dev, `P1v3` for prod |
| Web App for Containers | Runs the image; HTTPS only, TLS 1.2, FTP disabled, health check on `/health/ready` |
| System-assigned managed identity + `AcrPull` | The Web App pulls from ACR with **no passwords** |
| Log Analytics + Application Insights | Console/HTTP logs and monitoring |

App settings set on the Web App: `WEBSITES_PORT=8080` (Tomcat's port), `APP_ENV=<env>`,
`APPLICATIONINSIGHTS_CONNECTION_STRING`.

## Step 1 - Preview, then deploy dev

```bash
az login
az group create -n rg-devopsappcicd-dev -l westeurope

# what would change? (nothing is created yet)
az deployment group what-if -g rg-devopsappcicd-dev \
  -f infra/azure/main.bicep -p infra/azure/dev.bicepparam

az deployment group create -g rg-devopsappcicd-dev \
  -f infra/azure/main.bicep -p infra/azure/dev.bicepparam \
  --query properties.outputs
```

Outputs:

```json
{
  "acrLoginServer": { "value": "acrdevopsappcicdxxxxxxxx.azurecr.io" },
  "webAppName":     { "value": "app-devopsappcicd-dev-xxxxxxxx" },
  "webAppUrl":      { "value": "https://app-devopsappcicd-dev-xxxxxxxx.azurewebsites.net" }
}
```

Copy them into `azure-pipelines.yml` ([tutorial 4](04-azure-pipelines.md)).

## Step 2 - Prod

```bash
az group create -n rg-devopsappcicd-prod -l westeurope
az deployment group create -g rg-devopsappcicd-prod \
  -f infra/azure/main.bicep -p infra/azure/prod.bicepparam --query properties.outputs
```

Each resource group gets its own registry. To share one registry, push the image to the dev ACR
and grant the prod Web App `AcrPull` on it, or import images between registries:
`az acr import -n <prodAcr> --source <devAcr>.azurecr.io/devopsappcicd:<tag>`.

## Step 3 - First image

The Web App starts pointing at `devopsappcicd:latest`, which does not exist yet, so it shows an error
until the pipeline pushes the first image. To test right away:

```bash
az acr build -r <acrName> -t devopsappcicd:latest .   # builds in Azure using the Dockerfile
az webapp restart -g rg-devopsappcicd-dev -n <webAppName>
bash scripts/smoke-test.sh https://<webAppName>.azurewebsites.net dev
```

## Step 4 - Look at logs

```bash
az webapp log tail -g rg-devopsappcicd-dev -n <webAppName>
```

Or in the portal: **Application Insights → Logs**, or **Log Analytics → Logs** and query
`AppServiceConsoleLogs | take 50`.

## Why infrastructure as code?

* **Repeatable** - dev and prod come from the same file; only parameters differ.
* **Reviewable** - changes go through pull requests like application code.
* **Idempotent** - running `az deployment group create` again changes only what differs.
* **Disposable** - `az group delete -n rg-devopsappcicd-dev` and recreate it in minutes.

## Clean up (stop paying)

```bash
az group delete -n rg-devopsappcicd-dev --yes --no-wait
az group delete -n rg-devopsappcicd-prod --yes --no-wait
```

## Exercises

1. Add a **staging deployment slot** to the prod Web App and change the pipeline to deploy to the
   slot, smoke test it, then `az webapp deployment slot swap` - a zero-downtime release.
2. Add an Azure Monitor **availability test** on `/health` with an email alert.
3. Run the Bicep deployment from the pipeline itself with `AzureCLI@2` as a new first stage.

Next: [9. Troubleshooting](09-troubleshooting.md)
