pipeline {
    agent any
    
    tools {
        maven 'M2_HOME'
    }
    
    environment {
        DOCKER_IMAGE = 'malekmouelhi7/student-management'
        DOCKER_TAG = "${env.BUILD_NUMBER}"
        K8S_NAMESPACE = 'devops'
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'master',
                    url: 'https://github.com/Malekmouelh/jenkins.git'
            }
        }

        stage('Build & Test') {
            steps {
                sh 'mvn clean compile test'
            }
        }

        stage('SonarQube') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    sh 'mvn sonar:sonar -Dsonar.projectKey=student-management'
                }
            }
        }

        stage('Package') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Build Docker') {
            steps {
                sh """
                    docker build -t ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} .
                    docker tag ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} ${env.DOCKER_IMAGE}:latest
                """
            }
        }

        stage('Push Docker') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USERNAME',
                    passwordVariable: 'DOCKER_PASSWORD'
                )]) {
                    sh """
                        echo \$DOCKER_PASSWORD | docker login -u \$DOCKER_USERNAME --password-stdin
                        docker push ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
                        docker push ${env.DOCKER_IMAGE}:latest
                    """
                }
            }
        }

        stage('Prepare K8S Manifests') {
            steps {
                script {
                    sh """
                        echo "=== Préparation des manifests Kubernetes ==="

                        # Vérifier que les fichiers existent
                        ls -la mysql-deployment.yaml spring-deployment.yaml

                        # Mettre à jour l'image dans spring-deployment.yaml
                        # Version 1: remplacer n'importe quelle image qui commence par malekmouelhi7/student-management
                        sed -i 's|image:.*malekmouelhi7/student-management.*|image: ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}|g' spring-deployment.yaml

                        # Version 2: alternative si la version 1 ne fonctionne pas
                        # sed -i "s|image:.*|image: ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}|g" spring-deployment.yaml

                        echo "Image mise à jour dans spring-deployment.yaml: ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}"

                        # Afficher les premières lignes pour vérification
                        head -20 spring-deployment.yaml
                    """
                }
            }
        }

        stage('Deploy Kubernetes') {
            steps {
                script {
                    sh """
                        export KUBECONFIG=/var/lib/jenkins/.kube/config

                        echo "=== Déploiement dans Kubernetes ==="

                        # Créer le namespace
                        kubectl create namespace ${env.K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

                        echo "1. Déploiement de MySQL..."
                        kubectl apply -f mysql-deployment.yaml -n ${env.K8S_NAMESPACE}

                        echo "2. Attente initiale pour MySQL..."
                        sleep 20

                        echo "3. Déploiement de Spring Boot..."
                        kubectl apply -f spring-deployment.yaml -n ${env.K8S_NAMESPACE}

                        echo "4. Vérification du déploiement..."
                        kubectl rollout status deployment/spring-boot-deployment -n ${env.K8S_NAMESPACE} --timeout=180s || echo "Rollout en cours..."

                        echo "5. État des ressources :"
                        kubectl get all -n ${env.K8S_NAMESPACE}
                    """
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                script {
                    sh """
                        export KUBECONFIG=/var/lib/jenkins/.kube/config

                        echo "=== Vérification du déploiement ==="

                        # Attendre que les pods soient prêts
                        echo "Attente des pods Spring Boot..."
                        timeout 120 bash -c 'until kubectl get pods -n ${env.K8S_NAMESPACE} -l app=spring-boot-app -o jsonpath="{.items[0].status.phase}" 2>/dev/null | grep -q Running; do sleep 5; echo "En attente..."; done' || echo "Continuation..."

                        echo "1. État des pods :"
                        kubectl get pods -n ${env.K8S_NAMESPACE} -o wide

                        echo "2. Services :"
                        kubectl get svc -n ${env.K8S_NAMESPACE}

                        echo "3. Logs Spring Boot :"
                        kubectl logs deployment/spring-boot-deployment -n ${env.K8S_NAMESPACE} --tail=30 || echo "Impossible de récupérer les logs pour le moment"

                        echo "4. URL de l'application :"
                        if which minikube >/dev/null 2>&1; then
                            SERVICE_URL=\$(minikube service spring-service -n ${env.K8S_NAMESPACE} --url 2>/dev/null || echo "")
                        else
                            SERVICE_URL=""
                        fi

                        if [ -z "\$SERVICE_URL" ]; then
                            echo "Récupération du NodePort..."
                            NODE_PORT=\$(kubectl get svc spring-service -n ${env.K8S_NAMESPACE} -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "30080")
                            SERVICE_URL="http://localhost:\${NODE_PORT}"
                        fi

                        echo "URL: \$SERVICE_URL"

                        echo "5. Test de connectivité..."
                        for i in {1..12}; do
                            echo "Tentative \$i/12..."
                            if curl -s -f --max-time 10 "\$SERVICE_URL/student/actuator/health" > /dev/null; then
                                echo "✓ Application accessible!"
                                curl -s "\$SERVICE_URL/student/actuator/health" | head -5
                                break
                            elif curl -s -f --max-time 10 "\$SERVICE_URL/student" > /dev/null; then
                                echo "✓ Application accessible (racine)!"
                                break
                            else
                                echo "Application non disponible, nouvelle tentative dans 10s..."
                                sleep 10
                            fi
                        done
                    """
                }
            }
        }
    }

    post {
        success {
            echo "✅ Build ${env.BUILD_NUMBER} réussi et déployé sur Kubernetes!"
            script {
                sh """
                    export KUBECONFIG=/var/lib/jenkins/.kube/config
                    echo "=== Récapitulatif ==="
                    kubectl get pods,svc,deploy -n ${env.K8S_NAMESPACE}
                """
            }
        }
        failure {
            echo '❌ Build échoué!'
            script {
                sh """
                    echo "=== Débogage ==="
                    export KUBECONFIG=/var/lib/jenkins/.kube/config

                    echo "1. Ressources dans le namespace ${env.K8S_NAMESPACE}:"
                    kubectl get all -n ${env.K8S_NAMESPACE} 2>/dev/null || true

                    echo "2. Détails des pods:"
                    kubectl describe pods -n ${env.K8S_NAMESPACE} 2>/dev/null | head -100 || true

                    echo "3. Logs MySQL:"
                    kubectl logs -l app=mysql -n ${env.K8S_NAMESPACE} --tail=50 2>/dev/null || true

                    echo "4. Logs Spring Boot:"
                    kubectl logs -l app=spring-boot-app -n ${env.K8S_NAMESPACE} --tail=50 2>/dev/null || true

                    echo "5. Événements:"
                    kubectl get events -n ${env.K8S_NAMESPACE} 2>/dev/null | tail -20 || true
                """
            }
        }
    }
}