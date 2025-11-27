pipeline {
    agent any

    tools {
        maven 'M2_HOME'
    }

    environment {
        MAVEN_HOME = "${tool 'M2_HOME'}"
        PATH = "${env.MAVEN_HOME}/bin:${env.PATH}"
        DOCKER_IMAGE = "malekmouelhi7/student-management"
        DOCKER_TAG = "${env.BUILD_NUMBER}"
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

        stage('Build Docker Image') {
            steps {
                script {
                    sh """
                        echo "🐳 Building Docker image..."

                        docker build -t ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} \\
                                     -t ${env.DOCKER_IMAGE}:latest \\
                                     -f Dockerfile .

                        echo "✅ Docker image built"
                    """
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                script {
                    sh """
                        echo "🔐 DockerHub Login..."
                        docker login -u malekmouelhi7

                        echo "📤 Pushing images..."
                        docker push ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
                        docker push ${env.DOCKER_IMAGE}:latest

                        echo "🚀 Docker images pushed"
                    """
                }
            }
        }
    }

    post {
        success {
            echo """
            🎉 BUILD & DEPLOY OK !

            ➤ Image poussée : ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
            ➤ Latest : ${env.DOCKER_IMAGE}:latest

            Pour tester :
              docker run -p 8089:8089 ${env.DOCKER_IMAGE}:latest
            """
        }
        failure {
            echo '❌ Pipeline Failed.'
        }
    }
}
