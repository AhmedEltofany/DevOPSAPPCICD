// Azure infrastructure for one environment of the app:
//   * Azure Container Registry (ACR)           - stores the Docker images
//   * Linux App Service plan + Web App          - runs the container
//   * Log Analytics + Application Insights      - logs and monitoring
//   * Managed identity with AcrPull             - the Web App pulls images without passwords
//
// Deploy (see docs/tutorials/08-azure-infra-bicep.md):
//   az group create -n rg-devopsappcicd-dev -l westeurope
//   az deployment group create -g rg-devopsappcicd-dev -f infra/azure/main.bicep -p infra/azure/dev.bicepparam

targetScope = 'resourceGroup'

@description('Short base name used in every resource name.')
@minLength(3)
@maxLength(16)
param appName string = 'devopsappcicd'

@description('Environment this deployment is for.')
@allowed([
  'dev'
  'prod'
])
param envName string = 'dev'

@description('Azure region. Defaults to the resource group region.')
param location string = resourceGroup().location

@description('Image tag the Web App starts with. Pipelines deploy newer tags later.')
param imageTag string = 'latest'

@description('App Service plan SKU. B1 is cheap for learning; P1v3 for production.')
param appServiceSku string = envName == 'prod' ? 'P1v3' : 'B1'

var suffix = uniqueString(resourceGroup().id)
var tags = {
  app: appName
  environment: envName
  managedBy: 'bicep'
}

// ACR names: 5-50 lowercase letters and digits only.
var acrName = take('acr${replace(appName, '-', '')}${suffix}', 50)
var acrPullRoleId = '7f951dda-4ed3-4680-a7ca-43fe172d538d' // built-in "AcrPull" role

resource logs 'Microsoft.OperationalInsights/workspaces@2022-10-01' = {
  name: 'log-${appName}-${envName}'
  location: location
  tags: tags
  properties: {
    sku: {
      name: 'PerGB2018'
    }
    retentionInDays: 30
  }
}

resource appInsights 'Microsoft.Insights/components@2020-02-02' = {
  name: 'appi-${appName}-${envName}'
  location: location
  kind: 'web'
  tags: tags
  properties: {
    Application_Type: 'web'
    WorkspaceResourceId: logs.id
  }
}

resource acr 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: acrName
  location: location
  tags: tags
  sku: {
    name: 'Basic'
  }
  properties: {
    adminUserEnabled: false
  }
}

resource plan 'Microsoft.Web/serverfarms@2023-12-01' = {
  name: 'plan-${appName}-${envName}'
  location: location
  tags: tags
  kind: 'linux'
  sku: {
    name: appServiceSku
  }
  properties: {
    reserved: true // required for Linux plans
  }
}

resource webApp 'Microsoft.Web/sites@2023-12-01' = {
  name: 'app-${appName}-${envName}-${suffix}'
  location: location
  tags: tags
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    serverFarmId: plan.id
    httpsOnly: true
    siteConfig: {
      linuxFxVersion: 'DOCKER|${acr.properties.loginServer}/${appName}:${imageTag}'
      acrUseManagedIdentityCreds: true
      alwaysOn: true
      healthCheckPath: '/health/ready'
      ftpsState: 'Disabled'
      minTlsVersion: '1.2'
      http20Enabled: true
      appSettings: [
        {
          name: 'WEBSITES_PORT'
          value: '8080'
        }
        {
          name: 'APP_ENV'
          value: envName
        }
        {
          name: 'APPLICATIONINSIGHTS_CONNECTION_STRING'
          value: appInsights.properties.ConnectionString
        }
        {
          name: 'DOCKER_REGISTRY_SERVER_URL'
          value: 'https://${acr.properties.loginServer}'
        }
      ]
    }
  }
}

// Let the Web App's managed identity pull images from ACR.
resource acrPull 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(acr.id, webApp.id, acrPullRoleId)
  scope: acr
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', acrPullRoleId)
    principalId: webApp.identity.principalId
    principalType: 'ServicePrincipal'
  }
}

// Ship platform logs (HTTP, console, app logs) to Log Analytics.
resource webAppDiagnostics 'Microsoft.Insights/diagnosticSettings@2021-05-01-preview' = {
  name: 'to-log-analytics'
  scope: webApp
  properties: {
    workspaceId: logs.id
    logs: [
      {
        category: 'AppServiceHTTPLogs'
        enabled: true
      }
      {
        category: 'AppServiceConsoleLogs'
        enabled: true
      }
      {
        category: 'AppServiceAppLogs'
        enabled: true
      }
    ]
    metrics: [
      {
        category: 'AllMetrics'
        enabled: true
      }
    ]
  }
}

@description('Put this in azure-pipelines.yml as webAppNameDev / webAppNameProd.')
output webAppName string = webApp.name

@description('Public URL of the app.')
output webAppUrl string = 'https://${webApp.properties.defaultHostName}'

@description('Put this in azure-pipelines.yml as containerRegistry.')
output acrLoginServer string = acr.properties.loginServer

output acrName string = acr.name
