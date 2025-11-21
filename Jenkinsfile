pipeline {
    agent any
    
    tools {
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
                    script {
                        // Trouver le nom exact du JAR
                        def jarFile = findFiles(glob: 'target/*.jar')[0].name
                        env.JAR_FILE = jarFile
                        echo "JAR file: ${env.JAR_FILE}"
                    }
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    // Créer le Dockerfile
                    writeFile file: 'Dockerfile', text: """
FROM openjdk:17-alpine
COPY target/${env.JAR_FILE} app.jar
EXPOSE 8089
ENTRYPOINT ["java", "-jar", "app.jar"]
"""
                    // Afficher le contenu du Dockerfile pour vérification
                    sh 'cat Dockerfile'

                    // Builder l'image Docker
                    sh """
                        docker build -t ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} .
                        docker tag ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} ${env.DOCKER_IMAGE}:latest
                    """

                    // Lister les images pour vérification
                    sh 'docker images | grep student-management'
                }
            }
        }

        stage('Push to DockerHub') {
            steps {
                script {
                    // Authentification à DockerHub avec les credentials
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKER_USERNAME',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )]) {
                        sh """
                            echo "Authentification à DockerHub..."
                            echo ${DOCKER_PASSWORD} | docker login -u ${DOCKER_USERNAME} --password-stdin

                            echo "Push de l'image avec tag: ${env.DOCKER_TAG}"
                            docker push ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}

                            echo "Push de l'image latest"
                            docker push ${env.DOCKER_IMAGE}:latest
                        """
                    }
                }
            }
        }

        stage('Cleanup') {
            steps {
                script {
                    // Nettoyer les images locales pour libérer de l'espace
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
            echo 'Pipeline réussi avec succès!'
            echo "Image Docker disponible sur: ${env.DOCKER_REGISTRY}/${env.DOCKER_IMAGE}:${env.DOCKER_TAG}"
            echo "Image latest disponible sur: ${env.DOCKER_REGISTRY}/${env.DOCKER_IMAGE}:latest"
        }
        failure {
            echo 'Pipeline a échoué!'
        }
        always {
            // Déconnexion de DockerHub
            sh 'docker logout || true'

            // Nettoyage des fichiers temporaires
            sh '''
                rm -f Dockerfile || true
            '''
        }
    }
}