pipeline {
    agent any
    
    tools {
        maven 'M2_HOME'
        jdk 'JAVA_HOME'
    }

    environment {
        DOCKER_IMAGE = 'malekmouelhi7/student-management'
        DOCKER_TAG = "${env.BUILD_NUMBER}"
        K8S_NAMESPACE = 'devops'
    }

    stages {
        // ÉTAPE 1: PRÉPARATION
        stage('Préparation') {
            steps {
                script {
                    echo "🎯 ATELIER KUBERNETES - ESPRIT UP ASI"
                    echo "======================================"
                }

                // Checkout
                git branch: 'master',
                   url: 'https://github.com/Malekmouelh/jenkins.git'

                sh '''
                    echo "=== 1. VÉRIFICATION DE L'ENVIRONNEMENT ==="

                    echo "📋 Fichiers disponibles:"
                    ls -la *.yaml *.yml application.properties Dockerfile pom.xml 2>/dev/null || true

                    # Installer Minikube si nécessaire
                    if ! command -v minikube &> /dev/null; then
                        echo "📦 Installation de Minikube..."
                        curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
                        sudo install minikube-linux-amd64 /usr/local/bin/minikube
                        rm minikube-linux-amd64
                    fi

                    # Démarrer Minikube
                    echo "🚀 Démarrage de Minikube..."
                    minikube start --driver=docker --force 2>/dev/null || true

                    # Configurer Docker pour Minikube
                    eval $(minikube docker-env 2>/dev/null) || echo "⚠️ Docker-env configuré"

                    echo "✅ Environnement vérifié"
                '''
            }
        }

        // ÉTAPE 2: BUILD APPLICATION
        stage('Build Application') {
            steps {
                sh '''
                    echo "=== 2. BUILD APPLICATION ==="

                    # Copier la configuration
                    if [ -f "application.properties" ]; then
                        cp application.properties src/main/resources/ || true
                    fi

                    # Nettoyage et build
                    mvn clean compile test package -q

                    if ls target/*.jar 2>/dev/null; then
                        echo "✅ Build réussi"
                    else
                        echo "❌ Échec du build"
                        exit 1
                    fi
                '''
            }
        }

        // ÉTAPE 3: ANALYSE QUALITÉ
        stage('Analyse Qualité') {
            steps {
                sh '''
                    echo "=== 3. ANALYSE QUALITÉ ==="

                    # Générer rapport JaCoCo
                    mvn jacoco:report -q
                    echo "📊 Rapport JaCoCo généré"
                '''

                // SonarQube optionnel
                script {
                    try {
                        withSonarQubeEnv('sonarqube') {
                            sh '''
                                mvn sonar:sonar \\
                                    -Dsonar.projectKey=student-management \\
                                    -Dsonar.host.url=http://localhost:9000
                            '''
                        }
                    } catch (Exception e) {
                        echo "⚠️ SonarQube non disponible"
                    }
                }
            }
        }

        // ÉTAPE 4: BUILD DOCKER
        stage('Build Docker') {
            steps {
                sh '''
                    echo "=== 4. BUILD DOCKER ==="

                    # Créer Dockerfile si nécessaire
                    if [ ! -f "Dockerfile" ]; then
                        cat > Dockerfile << EOF
FROM eclipse-temurin:17-jre-alpine
COPY target/student-management-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
EOF
                    fi

                    # Build Docker
                    docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                    docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest

                    echo "✅ Image Docker créée"
                '''
            }
        }

        // ÉTAPE 5: DÉPLOIEMENT KUBERNETES
        stage('Déploiement K8s') {
            steps {
                sh '''
                    echo "=== 5. DÉPLOIEMENT KUBERNETES ==="

                    # Créer namespace
                    kubectl create namespace ${K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

                    # Déployer MySQL
                    echo "🗄️ Déploiement MySQL..."
                    if [ -f "mysql-deployment.yaml" ]; then
                        kubectl apply -f mysql-deployment.yaml -n ${K8S_NAMESPACE}
                        sleep 20
                    fi

                    # Déployer Spring Boot
                    echo "🚀 Déploiement Spring Boot..."
                    if [ -f "spring-deployment.yaml" ]; then
                        # Mettre à jour l'image
                        sed -i.bak "s|image:.*|image: ${DOCKER_IMAGE}:${DOCKER_TAG}|" spring-deployment.yaml
                        kubectl apply -f spring-deployment.yaml -n ${K8S_NAMESPACE}
                        mv spring-deployment.yaml.bak spring-deployment.yaml
                    fi

                    echo "✅ Déploiements appliqués"
                '''
            }
        }

        // ÉTAPE 6: VÉRIFICATION
        stage('Vérification') {
            steps {
                sh '''
                    echo "=== 6. VÉRIFICATION ==="

                    sleep 15

                    echo "📊 État des pods:"
                    kubectl get pods -n ${K8S_NAMESPACE}

                    echo ""
                    echo "🔗 Services:"
                    kubectl get svc -n ${K8S_NAMESPACE}

                    # Obtenir l'URL
                    NODE_PORT=$(kubectl get svc spring-service -n ${K8S_NAMESPACE} -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "")
                    MINIKUBE_IP=$(minikube ip 2>/dev/null || echo "")

                    if [ -n "$NODE_PORT" ] && [ -n "$MINIKUBE_IP" ]; then
                        echo ""
                        echo "🎯 URL de l'application: http://${MINIKUBE_IP}:${NODE_PORT}/student"
                    fi
                '''
            }
        }
    }

    post {
        always {
            sh '''
                echo ""
                echo "========================================"
                echo "📋 RÉSUMÉ DU BUILD #${BUILD_NUMBER}"
                echo "========================================"
                echo ""
                echo "• Build: #${BUILD_NUMBER}"
                echo "• Namespace: ${K8S_NAMESPACE}"
                echo "• Image: ${DOCKER_IMAGE}:${DOCKER_TAG}"
                echo "• Minikube IP: $(minikube ip 2>/dev/null || echo 'N/A')"
                echo ""
                echo "📊 État final:"
                kubectl get all -n ${K8S_NAMESPACE} 2>/dev/null || echo "Aucune ressource"
            '''
        }

        success {
            echo """
            🎉 ATELIER RÉUSSI !

            ✅ Objectifs atteints:
            1. ✅ Pipeline CI/CD intégré
            2. ✅ Application buildée et testée
            3. ✅ Image Docker créée
            4. ✅ Déploiement Kubernetes réalisé
            5. ✅ Services exposés
            """
        }

        failure {
            echo "❌ Échec - Vérifiez les logs"
        }
    }
}