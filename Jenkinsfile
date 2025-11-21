pipeline {
agent any


environment {
    MVN_HOME = tool name: 'Maven 3', type: 'maven'
    JAVA_HOME = tool name: 'JDK 17', type: 'jdk'
    PATH = "${env.MVN_HOME}/bin:${env.JAVA_HOME}/bin:${env.PATH}"
    DOCKERHUB_CREDENTIALS = credentials('dockerhub-id') // remplace par ton ID Jenkins pour DockerHub
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
sh "docker build -t sakaoli55/student-management:56 ."
}
}
}


    stage('Push to DockerHub') {
        steps {
            script {
                docker.withRegistry('https://index.docker.io/v1/', 'dockerhub-id') {
                    sh "docker push sakaoli55/student-management:56"
                }
            }
        }
    }

    stage('Cleanup') {
        steps {
            sh 'docker logout'
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
