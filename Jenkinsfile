pipeline {
    agent any

    environment {
        MVN_HOME = tool name: 'M2_HOME', type: 'maven'
        PATH = "${env.MVN_HOME}/bin:${env.PATH}"
        DOCKER_IMAGE = "sakaoli55/student-management"
        DOCKER_TAG = "56"
    }

    stages {
        stage('Checkout SCM') {
            steps {
                checkout scm
            }
        }

        stage('Test') {
            steps {
                sh "${MVN_HOME}/bin/mvn test"
            }
        }

        stage('Package') {
            steps {
                sh "${MVN_HOME}/bin/mvn clean package -DskipTests"
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    def jarFile = sh(script: "ls target/*.jar | head -1", returnStdout: true).trim()
                    echo "JAR file pour Docker: ${jarFile}"

                    writeFile file: 'Dockerfile', text: """
FROM eclipse-temurin:17-jdk-alpine
COPY ${jarFile} app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
"""
                    sh "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
                }
            }
        }

        stage('Push to DockerHub') {
            when {
                expression {
                    def creds = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
                        com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials.class,
                        Jenkins.instance,
                        null,
                        null
                    )
                    return creds.find { it.id == 'dockerhub-id' } != null
                }
            }
            steps {
                script {
                    docker.withRegistry('https://index.docker.io/v1/', 'dockerhub-id') {
                        sh "docker push ${DOCKER_IMAGE}:${DOCKER_TAG}"
                    }
                }
            }
        }

        stage('Cleanup') {
            steps {
                sh 'docker logout || true'
            }
        }
    }

    post {
        success {
            echo "✅ Pipeline terminé avec succès !"
        }
        failure {
            echo "❌ Pipeline a échoué !"
        }
    }
}
