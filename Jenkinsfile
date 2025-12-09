pipeline {
    agent any
    
    tools {
        maven 'M2_HOME'
    }
    
    environment {
        DOCKER_IMAGE = 'malekmouelhi7

/student-management'
        DOCKER_TAG = "${env.BUILD_NUMBER}"
        K8S_NAMESPACE = 'devops'
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main',
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

        stage('Deploy Kubernetes') {
            steps {
                script {
                    sh """
                        # Définir le kubeconfig explicitement
                        export KUBECONFIG=/var/lib/jenkins/.kube/config

                        echo "=== Déploiement dans Kubernetes ==="

                        # Créer le namespace si nécessaire
                        kubectl create namespace ${env.K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

                        echo "1. Déploiement de MySQL..."
                        kubectl apply -f mysql-deployment.yaml -n ${env.K8S_NAMESPACE}

                        echo "2. Déploiement de Spring Boot..."
                        kubectl apply -f spring-deployment.yaml -n ${env.K8S_NAMESPACE}

                        echo "3. Attente des déploiements..."
                        sleep 30

                        echo "4. État des ressources :"
                        kubectl get all -n ${env.K8S_NAMESPACE}
                    """
                }
            }
        }

        stage('Verify') {
            steps {
                script {
                    sh """
                        export KUBECONFIG=/var/lib/jenkins/.kube/config

                        echo "=== Vérification ==="

                        echo "1. Pods :"
                        kubectl get pods -n ${env.K8S_NAMESPACE} -o wide

                        echo "2. Services :"
                        kubectl get svc -n ${env.K8S_NAMESPACE}

                        echo "3. Obtention de l'URL..."
                        SERVICE_URL=\$(minikube service spring-service -n ${env.K8S_NAMESPACE} --url 2>/dev/null || echo "")

                        if [ -z "\$SERVICE_URL" ]; then
                            echo "Alternative : récupération du NodePort..."
                            NODE_PORT=\$(kubectl get svc spring-service -n ${env.K8S_NAMESPACE} -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "30080")
                            MINIKUBE_IP=\$(minikube ip)
                            SERVICE_URL="http://\${MINIKUBE_IP}:\${NODE_PORT}"
                        fi

                        echo "URL de l'application: \$SERVICE_URL"

                        echo "4. Test de l'application..."
                        curl -s -f --max-time 30 "\$SERVICE_URL/student/actuator/health" || \
                        curl -s -f --max-time 30 "\$SERVICE_URL/student" || \
                        echo "L'application n'est pas encore prête"
                    """
                }
            }
        }
    }

    post {
        success {
            echo "✅ Build ${env.BUILD_NUMBER} réussi et déployé sur Kubernetes!"
        }
        failure {
            echo '❌ Build échoué!'
            sh '''
                echo "=== Débogage ==="
                export KUBECONFIG=/var/lib/jenkins/.kube/config
                kubectl get all -n devops
                kubectl describe pods -n devops
            '''
        }
    }
}