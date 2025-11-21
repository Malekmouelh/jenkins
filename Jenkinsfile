pipeline {
    agent any
    
    tools {
        maven 'M2_HOME'
    }
    
    environment {
        MAVEN_HOME = "${tool 'M2_HOME'}"
        PATH = "${env.MAVEN_HOME}/bin:${env.PATH}"
        DOCKER_IMAGE = "sakaoli55/student-management"
        DOCKER_TAG = "${env.BUILD_NUMBER}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/Salsabil-23/hafsi_salsabil_4sim2_devops.git'
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
                    // Méthode alternative pour trouver le JAR
                    sh '''
                        echo "=== Recherche du fichier JAR ==="
                        ls -la target/
                        JAR_FILE=$(ls target/*.jar | head -1)
                        echo "Fichier JAR trouvé: $JAR_FILE"
                        echo "JAR_FILE=${JAR_FILE}" > jar_info.txt
                    '''
                    
                    // Lire le nom du fichier JAR
                    def jarInfo = readFile('jar_info.txt').trim()
                    def jarFile = jarInfo.split('=')[1]
                    echo "JAR file pour Docker: ${jarFile}"
                    
                    // Créer le Dockerfile
                    writeFile file: 'Dockerfile', text: """
FROM openjdk:17-alpine
COPY ${jarFile} app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
"""
                    // Afficher le Dockerfile pour vérification
                    sh 'cat Dockerfile'
                    
                    // Builder l'image Docker
                    sh """
                        docker build -t ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} .
                        docker tag ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} ${env.DOCKER_IMAGE}:latest
                    """
                    
                    // Lister les images pour vérification
                    sh 'docker images'
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
                            echo "Login to DockerHub..."
                            docker login -u ${DOCKER_USERNAME} -p ${DOCKER_PASSWORD}
                            
                            echo "Pushing image ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}"
                            docker push ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
                            
                            echo "Pushing image ${env.DOCKER_IMAGE}:latest"
                            docker push ${env.DOCKER_IMAGE}:latest
                            
                            echo "✅ Images pushed successfully!"
                        """
                    }
                }
            }
        }
        
        stage('Cleanup') {
            steps {
                sh '''
                    rm -f jar_info.txt Dockerfile || true
                    docker logout || true
                '''
            }
        }
    }
    
    post {
        success {
            echo "🎉 Pipeline réussi!"
            echo "📦 Repository DockerHub: https://hub.docker.com/r/sakaoli55/student-management"
            echo "🐳 Image: ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}"
        }
        failure {
            echo '❌ Pipeline a échoué!'
        }
        always {
            sh 'docker logout || true'
        }
    }
}
