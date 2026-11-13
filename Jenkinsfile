// Declarative Jenkins pipeline: build -> test -> package image -> push -> deploy -> smoke test.
//
// Prerequisites (see docs/tutorials/06-jenkins.md):
//   * Jenkins agent with Docker available
//   * Global Tool Configuration: JDK named "jdk-17", Maven named "maven-3.9"
//   * Credentials (Username with password) with ID "docker-registry" for pushing images
//   * Plugins: Pipeline, Git, Docker Pipeline, JUnit, Coverage (optional), Timestamper
pipeline {
    agent any

    tools {
        jdk 'jdk-17'
        maven 'maven-3.9'
    }

    parameters {
        string(name: 'REGISTRY', defaultValue: 'docker.io/your-dockerhub-user',
               description: 'Registry/namespace to push to, e.g. docker.io/ahmed or myacr.azurecr.io')
        booleanParam(name: 'PUSH_IMAGE', defaultValue: false, description: 'Push the image to REGISTRY')
        booleanParam(name: 'DEPLOY', defaultValue: true, description: 'Run the container on this agent and smoke test it')
        string(name: 'SMOKE_HOST', defaultValue: 'localhost',
               description: 'Host the smoke test calls. Use host.docker.internal when Jenkins itself runs in Docker')
    }

    environment {
        IMAGE_NAME = 'devopsappcicd'
        IMAGE_TAG  = "${env.BUILD_NUMBER}"
        APP_PORT   = '8085'
    }

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                }
                echo "Building commit ${env.GIT_SHORT}"
            }
        }

        stage('Build & Test') {
            steps {
                sh "mvn -B -ntp clean verify -Dbuild.commit=${env.GIT_SHORT}"
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/TEST-*.xml', allowEmptyResults: true
                    // Requires the "Coverage" plugin; remove this line if you do not have it.
                    recordCoverage tools: [[parser: 'JACOCO', pattern: '**/target/site/jacoco/jacoco.xml']]
                }
                success {
                    archiveArtifacts artifacts: 'webapp/target/webapp.war', fingerprint: true
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh "docker build -f AzureDockerfile -t ${IMAGE_NAME}:${IMAGE_TAG} -t ${IMAGE_NAME}:latest ."
            }
        }

        stage('Docker Push') {
            when {
                allOf {
                    expression { params.PUSH_IMAGE }
                    not { changeRequest() }   // never push images from pull requests
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-registry',
                                                  usernameVariable: 'REG_USER',
                                                  passwordVariable: 'REG_PASS')]) {
                    sh '''
                        registry_host="${REGISTRY%%/*}"
                        echo "$REG_PASS" | docker login "$registry_host" -u "$REG_USER" --password-stdin
                        docker tag "$IMAGE_NAME:$IMAGE_TAG" "$REGISTRY/$IMAGE_NAME:$IMAGE_TAG"
                        docker tag "$IMAGE_NAME:$IMAGE_TAG" "$REGISTRY/$IMAGE_NAME:latest"
                        docker push "$REGISTRY/$IMAGE_NAME:$IMAGE_TAG"
                        docker push "$REGISTRY/$IMAGE_NAME:latest"
                    '''
                }
            }
            post {
                always { sh 'docker logout || true' }
            }
        }

        stage('Deploy (local container)') {
            when { expression { params.DEPLOY } }
            steps {
                sh '''
                    docker rm -f devopsappcicd-jenkins || true
                    docker run -d --name devopsappcicd-jenkins -p "$APP_PORT:8080" \
                        -e APP_ENV=jenkins --restart unless-stopped "$IMAGE_NAME:$IMAGE_TAG"
                '''
            }
        }

        stage('Smoke Test') {
            when { expression { params.DEPLOY } }
            steps {
                sh 'bash scripts/smoke-test.sh "http://$SMOKE_HOST:$APP_PORT" jenkins'
            }
        }
    }

    post {
        success {
            echo "Deployed ${IMAGE_NAME}:${IMAGE_TAG} (commit ${env.GIT_SHORT}) - http://localhost:${APP_PORT}/"
        }
        failure {
            echo 'Build failed - open "Console Output" and the "Test Result" page for details.'
        }
        cleanup {
            // Remove dangling images so the agent's disk does not fill up.
            sh 'docker image prune -f || true'
        }
    }
}
