pipeline {
    agent any
    
    environment {
        DOCKER_REGISTRY = "docker.io"
        DOCKER_IMAGE_NAME = "syrine47/devopsproject"
        DOCKER_IMAGE_TAG = "${BUILD_NUMBER}"
        GIT_BRANCH = "main"
        MAVEN_OPTS = "-Dhttp.connectionManager.timeout=60000 -Dhttp.socket.timeout=60000 -Dmaven.wagon.http.retryHandler.count=5"
        JENKINS_HEARTBEAT = "-Dorg.jenkinsci.plugins.durabletask.BourneShellScript.HEARTBEAT_CHECK_INTERVAL=300"
    }
    
    tools {
        maven 'Maven-3'
        jdk 'JDK-17'
    }
    
    stages {
        stage('🔍 Vérification de l\'environnement') {
            steps {
                script {
                    echo "=== Vérification Maven et Java ==="
                    sh 'mvn --version'
                    sh 'java -version'
                    sh 'docker --version'
                }
            }
        }
        
        stage('📥 Récupération du code') {
            steps {
                echo ">>> Récupération du code depuis ${GIT_BRANCH}..."
                git branch: "${GIT_BRANCH}", 
                    url: 'https://github.com/syrine47/devopsproject.git'
            }
        }
        
        stage('📚 Cache des dépendances') {
            steps {
                echo ">>> Pré-téléchargement des dépendances en cache..."
                sh '''
                    mvn dependency:go-offline -q \
                      -Dhttp.connectionManager.timeout=60000 \
                      -Dhttp.socket.timeout=60000 \
                      -Dmaven.wagon.http.retryHandler.count=5
                '''
            }
        }
        
        stage('🧪 Tests unitaires') {
            steps {
                echo ">>> Lancement des tests Maven..."
                sh '''
                    mvn clean test \
                      -Dhttp.connectionManager.timeout=60000 \
                      -Dhttp.socket.timeout=60000 \
                      -Dmaven.wagon.http.retryHandler.count=5
                '''
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', 
                          allowEmptyResults: true
                    echo "✅ Résultats de tests publiés"
                }
            }
        }
        
        stage('📦 Build Maven (Package)') {
            steps {
                echo ">>> Compilation et création du JAR..."
                sh '''
                    mvn clean package -DskipTests \
                      -Dhttp.connectionManager.timeout=60000 \
                      -Dhttp.socket.timeout=60000 \
                      -Dmaven.wagon.http.retryHandler.count=5
                '''
            }
        }
        
        stage('📚 Archivage du livrable') {
            steps {
                echo ">>> Archivage du JAR..."
                archiveArtifacts artifacts: 'target/*.jar', 
                                  fingerprint: true,
                                  allowEmptyArchive: false
            }
        }
        
        stage('🐳 Build Image Docker') {
            steps {
                script {
                    echo ">>> Vérification de Docker..."
                    sh 'docker --version'
                    
                    echo ">>> Construction de l'image Docker..."
                    sh '''
                        docker build \
                          --tag ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG} \
                          --tag ${DOCKER_IMAGE_NAME}:latest \
                          -f Dockerfile .
                    '''
                }
            }
        }
        
        stage('📋 Liste Images Docker') {
            steps {
                script {
                    echo ">>> Images créées..."
                    sh 'docker images | grep ${DOCKER_IMAGE_NAME}'
                }
            }
        }
        
        stage('✅ Test Image Docker') {
            steps {
                script {
                    echo ">>> Inspect de l'image..."
                    sh 'docker inspect ${DOCKER_IMAGE_NAME}:latest | head -20'
                }
            }
        }
    }
    
    post {
        success {
            echo "🎉 BUILD SUCCESS ✅"
            echo "Image Docker créée : ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}"
            echo "Commande pour lancer : docker run -p 8080:8080 ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}"
        }
        failure {
            echo "❌ BUILD FAILED"
            echo "Consulte la console complète pour les détails"
        }
        always {
            echo "Nettoyage..."
            cleanWs()
        }
    }
}
