pipeline {
    agent any
    
    tools {
        maven 'M2_HOME'
    }
    
    environment {
        DOCKER_IMAGE = 'malekmouelhi7/student-management'
        DOCKER_TAG = "${env.BUILD_NUMBER}"
        K8S_NAMESPACE = 'devops'
        SONARQUBE_URL = 'http://sonarqube-service.devops.svc.cluster.local:9000'
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'master',
                    url: 'https://github.com/Malekmouelh/jenkins.git'
            }
        }

        stage('Setup SonarQube on K8S') {
            steps {
                script {
                    sh """
                        export KUBECONFIG=/var/lib/jenkins/.kube/config

                        echo "=== Déploiement de SonarQube sur Kubernetes ==="

                        # Créer le namespace si nécessaire
                        kubectl create namespace ${env.K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

                        # Déployer SonarQube
                        echo "1. Création des volumes..."
                        kubectl apply -f sonarqube-persistentvolume.yaml -n ${env.K8S_NAMESPACE} 2>/dev/null || echo "PV déjà existant"
                        kubectl apply -f sonarqube-persistentvolumeclaim.yaml -n ${env.K8S_NAMESPACE}

                        echo "2. Déploiement de SonarQube..."
                        kubectl apply -f sonarqube-deployment.yaml -n ${env.K8S_NAMESPACE}
                        kubectl apply -f sonarqube-service.yaml -n ${env.K8S_NAMESPACE}

                        echo "3. Attente du démarrage de SonarQube..."
                        # Attendre que SonarQube soit prêt
                        timeout 300 bash -c 'until curl -s -f http://localhost:30090/api/system/status 2>/dev/null | grep -q "UP"; do sleep 10; echo "En attente de SonarQube..."; done' || echo "SonarQube prend du temps à démarrer"

                        echo "4. Vérification de l'état..."
                        kubectl get pods -l app=sonarqube -n ${env.K8S_NAMESPACE}

                        echo "5. URL SonarQube : http://localhost:30090"
                    """
                }
            }
        }

        stage('Build & Test') {
            steps {
                sh 'mvn clean compile test'
            }
        }

        stage('SonarQube Analysis on K8S') {
            steps {
                script {
                    sh """
                        echo "=== Analyse SonarQube sur Kubernetes ==="
                        echo "SonarQube URL: ${env.SONARQUBE_URL}"

                        # Configuration alternative pour utiliser le SonarQube sur K8S
                        # Obtenir l'IP du pod SonarQube
                        SONAR_POD_IP=\$(kubectl get pods -l app=sonarqube -n ${env.K8S_NAMESPACE} -o jsonpath='{.items[0].status.podIP}')
                        echo "SonarQube Pod IP: \$SONAR_POD_IP"

                        # Vérifier la connectivité
                        echo "Test de connexion à SonarQube..."
                        curl -s http://\${SONAR_POD_IP}:9000/api/system/status || echo "SonarQube non accessible directement, utilisation du service"

                        # Exécuter l'analyse SonarQube
                        mvn sonar:sonar \
                            -Dsonar.projectKey=student-management \
                            -Dsonar.host.url=${env.SONARQUBE_URL} \
                            -Dsonar.login=admin \
                            -Dsonar.password=admin \
                            -Dsonar.projectName="Student Management" \
                            -Dsonar.projectVersion="${env.DOCKER_TAG}" \
                            -Dsonar.sources=src/main/java \
                            -Dsonar.tests=src/test/java \
                            -Dsonar.java.binaries=target/classes \
                            -Dsonar.junit.reportPaths=target/surefire-reports \
                            -Dsonar.jacoco.reportPaths=target/jacoco.exec
                    """
                }
            }
        }

        stage('Verify SonarQube Analysis') {
            steps {
                script {
                    sh """
                        export KUBECONFIG=/var/lib/jenkins/.kube/config

                        echo "=== Vérification de l'analyse SonarQube ==="

                        # Attendre que l'analyse soit terminée
                        echo "Attente de la fin de l'analyse..."
                        sleep 30

                        # Vérifier l'état de SonarQube
                        echo "1. État de SonarQube:"
                        kubectl get pods -l app=sonarqube -n ${env.K8S_NAMESPACE} -o wide

                        # Obtenir les logs de SonarQube
                        echo "2. Logs SonarQube (dernières 10 lignes):"
                        kubectl logs deployment/sonarqube-deployment -n ${env.K8S_NAMESPACE} --tail=10 || echo "Impossible de récupérer les logs"

                        # Tester l'accès à l'interface web
                        echo "3. Test d'accès web:"
                        curl -s -f http://localhost:30090/api/projects/search?projects=student-management || \\
                        curl -s -f http://\$(kubectl get svc sonarqube-service -n ${env.K8S_NAMESPACE} -o jsonpath='{.spec.clusterIP}'):9000/api/projects/search?projects=student-management || \\
                        echo "Impossible d'accéder à l'API SonarQube"

                        # Vérifier si le projet existe
                        echo "4. Vérification du projet sur SonarQube..."
                        SONAR_STATUS=\$(curl -s "http://localhost:30090/api/qualitygates/project_status?projectKey=student-management" 2>/dev/null || echo "{}")
                        echo "Résultat SonarQube: \$SONAR_STATUS"

                        if echo "\$SONAR_STATUS" | grep -q "ERROR" || echo "\$SONAR_STATUS" | grep -q "OK"; then
                            echo "✓ Analyse SonarQube détectée!"
                        else
                            echo "⚠ Analyse SonarQube non encore disponible"
                        fi
                    """
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
                        sed -i 's|image:.*malekmouelhi7/student-management.*|image: ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}|g' spring-deployment.yaml

                        echo "Image mise à jour dans spring-deployment.yaml: ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}"
                    """
                }
            }
        }

        stage('Deploy Application on K8S') {
            steps {
                script {
                    sh """
                        export KUBECONFIG=/var/lib/jenkins/.kube/config

                        echo "=== Déploiement de l'application sur Kubernetes ==="

                        echo "1. Déploiement de MySQL..."
                        kubectl apply -f mysql-deployment.yaml -n ${env.K8S_NAMESPACE}

                        echo "2. Correction du problème MySQL (mot de passe)..."
                        # Corriger le mot de passe MySQL
                        kubectl set env deployment/mysql-deployment -n ${env.K8S_NAMESPACE} MYSQL_ROOT_PASSWORD=rootpassword
                        kubectl rollout restart deployment/mysql-deployment -n ${env.K8S_NAMESPACE}

                        echo "3. Attente que MySQL soit prêt..."
                        sleep 30

                        echo "4. Déploiement de Spring Boot..."
                        kubectl apply -f spring-deployment.yaml -n ${env.K8S_NAMESPACE}

                        echo "5. Vérification du déploiement..."
                        kubectl rollout status deployment/spring-boot-deployment -n ${env.K8S_NAMESPACE} --timeout=180s || echo "Rollout en cours..."

                        echo "6. État des ressources :"
                        kubectl get all -n ${env.K8S_NAMESPACE}
                    """
                }
            }
        }

        stage('Quality Gate Check') {
            steps {
                script {
                    sh """
                        export KUBECONFIG=/var/lib/jenkins/.kube/config

                        echo "=== Vérification de la Quality Gate SonarQube ==="

                        # Attendre que l'analyse soit complète
                        echo "Attente des résultats SonarQube..."
                        sleep 60

                        # Obtenir le statut de la quality gate
                        echo "Récupération du statut Quality Gate..."
                        SONAR_QG_STATUS=\$(curl -s "http://localhost:30090/api/qualitygates/project_status?projectKey=student-management" 2>/dev/null || echo '{"projectStatus":{"status":"NONE"}}')

                        echo "Statut Quality Gate: \$SONAR_QG_STATUS"

                        # Extraire le statut
                        QG_STATUS=\$(echo "\$SONAR_QG_STATUS" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)

                        echo "Statut: \$QG_STATUS"

                        # Vérifier le statut
                        if [ "\$QG_STATUS" = "OK" ]; then
                            echo "✅ Quality Gate PASSED"
                        elif [ "\$QG_STATUS" = "ERROR" ]; then
                            echo "❌ Quality Gate FAILED"
                            echo "L'analyse SonarQube a échoué. Vérifiez les problèmes sur http://localhost:30090"
                            # Vous pouvez choisir de fail le build ici si besoin
                            # currentBuild.result = 'FAILURE'
                        else
                            echo "⚠ Quality Gate indéterminée"
                            echo "Consultez manuellement: http://localhost:30090/dashboard?id=student-management"
                        fi
                    """
                }
            }
        }
    }

    post {
        success {
            echo "✅ Build ${env.BUILD_NUMBER} réussi !"
            echo "🔗 SonarQube: http://localhost:30090"
            echo "🔗 Application Spring: http://localhost:30080/student"
        }
        failure {
            echo '❌ Build échoué!'
            script {
                sh '''
                    echo "=== Débogage ==="
                    export KUBECONFIG=/var/lib/jenkins/.kube/config

                    echo "1. Tous les pods:"
                    kubectl get pods -n devops

                    echo "2. Logs SonarQube:"
                    kubectl logs deployment/sonarqube-deployment -n devops --tail=50 2>/dev/null || true

                    echo "3. Logs MySQL:"
                    kubectl logs deployment/mysql-deployment -n devops --tail=50 2>/dev/null || true

                    echo "4. Logs Spring Boot:"
                    kubectl logs deployment/spring-boot-deployment -n devops --tail=50 2>/dev/null || true
                '''
            }
        }
        always {
            script {
                // Nettoyage optionnel des ressources SonarQube si nécessaire
                // sh '''
                //     kubectl delete -f sonarqube-deployment.yaml -n devops --ignore-not-found
                //     kubectl delete -f sonarqube-service.yaml -n devops --ignore-not-found
                // '''
            }
        }
    }
}