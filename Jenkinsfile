pipeline {
agent any

```
tools {
    jdk 'JDK 17'
    maven 'M2_HOME'
}

environment {
    MAVEN_HOME = "${tool 'M2_HOME'}"
    PATH = "${env.MAVEN_HOME}/bin:${env.PATH}"
    DOCKER_IMAGE = "salsabil55/student-management"
    DOCKER_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.substring(0,7)}"
    DOCKER_REGISTRY = "docker.io"
}

stages {
    stage('Checkout') {
        steps {
            git branch: 'main', url: 'https://github.com/Malekmouelh/jenkins.git'
        }
    }

    stage('Check Tools') {
        steps {
            sh "${MAVEN_HOME}/bin/mvn -v"
            sh "${JAVA_HOME}/bin/java -version"
        }
    }

    stage('Test') {
        steps {
            sh "${MAVEN_HOME}/bin/mvn test"
        }
        post {
            always {
                junit 'target/surefire-reports/*.xml'
            }
        }
    }

    stage('Package') {
        steps {
            sh "${MAVEN_HOME}/bin/mvn clean package -DskipTests"
        }
        post {
            success {
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                script {
                    // Vérifier que le JAR existe
                    def jarFiles = findFiles(glob: 'target/*.jar')
                    if (jarFiles.length == 0) {
                        error "❌ Aucun JAR trouvé dans target/"
                    }
                    env.JAR_FILE = jarFiles[0].name
                    echo "JAR file: ${env.JAR_FILE}"
                }
            }
        }
    }

    stage('Build Docker Image') {
        steps {
            script {
                writeFile file: 'Dockerfile', text: """
```

FROM openjdk:17-alpine
COPY target/${env.JAR_FILE} app.jar
EXPOSE 8089
ENTRYPOINT ["java", "-jar", "app.jar"]
"""
sh 'cat Dockerfile'

```
                sh """
                    docker build -t ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} .
                    docker tag ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} ${env.DOCKER_IMAGE}:latest
                """

                sh 'docker images | grep student-management'
            }
        }
    }

    stage('Push to DockerHub') {
        steps {
            script {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USERNAME',
                    passwordVariable: 'DOCKER_PASSWORD'
                )]) {
                    sh """
                        echo ${DOCKER_PASSWORD} | docker login -u ${DOCKER_USERNAME} --password-stdin
                        docker push ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
                        docker push ${env.DOCKER_IMAGE}:latest
                    """
                }
            }
        }
    }

    stage('Cleanup') {
        steps {
            script {
                sh """
                    docker rmi ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} || true
                    docker rmi ${env.DOCKER_IMAGE}:latest || true
                """
            }
        }
    }
}

post {
    success {
        echo "✅ Pipeline réussi !"
        echo "Image Docker: ${env.DOCKER_REGISTRY}/${env.DOCKER_IMAGE}:${env.DOCKER_TAG}"
        echo "Image latest: ${env.DOCKER_REGISTRY}/${env.DOCKER_IMAGE}:latest"
    }
    failure {
        echo "❌ Pipeline a échoué !"
    }
    always {
        sh 'docker logout || true'
        sh 'rm -f Dockerfile || true'
    }
}
```

}
