pipeline {
    agent any

    tools {
        maven 'M2_HOME' // Assure-toi que ce Maven est configuré dans Jenkins
        jdk 'JAVA_HOME'     // Assure-toi que JDK 17 est configuré
    }

    environment {
        DOCKER_IMAGE = "malekmouelhi7/student-management"
        DOCKER_TAG = "${env.BUILD_NUMBER}"
        PATH = "${tool 'M2_HOME'}/bin:${env.PATH}"
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'master', url: 'https://github.com/Malekmouelh/jenkins.git'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Build & Push Docker') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh """
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin

                        echo "🐳 Build Docker image..."
                        docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} -t ${DOCKER_IMAGE}:latest .

                        echo "📤 Push Docker image..."
                        docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                        docker push ${DOCKER_IMAGE}:latest
                    """
                }
            }
        }
    }

    post {
        success {
            echo "✅ Tests OK, Docker image built and pushed: ${DOCKER_IMAGE}:${DOCKER_TAG}"
        }
        failure {
            echo "❌ Pipeline failed!"
        }
    }
}
