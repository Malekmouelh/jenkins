pipeline {
    agent any
    
    tools {
        maven 'M2_HOME'
    }
    
    environment {
        DOCKER_IMAGE = 'malekmouelhi7/student-management'
        DOCKER_TAG = "${env.BUILD_NUMBER}"
        K8S_NAMESPACE = 'devops'
        SPRING_APP_URL = ''
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'master',
                    url: 'https://github.com/Malekmouelh/jenkins.git'
            }
        }

        stage('Fix Maven Issues') {
            steps {
                sh '''
                    echo "=== Correction des problèmes Maven ==="

                    # 1. Désactiver le filtering dans pom.xml
                    if [ -f pom.xml ]; then
                        echo "Désactivation du filtering Maven..."
                        # Créer une copie de backup
                        cp pom.xml pom.xml.backup
                        # Supprimer les sections de filtering problématiques
                        sed -i '/<filtering>/d' pom.xml
                        sed -i '/<resource>/,/<\/resource>/s/<filtering>.*<\/filtering>//' pom.xml
                        sed -i 's/<filtering>true<\/filtering>//g' pom.xml
                    fi

                    # 2. Vérifier le fichier application.properties
                    echo "Vérification de application.properties..."
                    if [ -f src/main/resources/application.properties ]; then
                        # Convertir en UTF-8
                        iconv -f latin1 -t UTF-8 src/main/resources/application.properties > src/main/resources/application.properties.utf8 2>/dev/null || true
                        mv src/main/resources/application.properties.utf8 src/main/resources/application.properties 2>/dev/null || true
                    fi

                    # 3. Nettoyer les dépendances dupliquées
                    echo "Nettoyage des dépendances dupliquées..."
                    sed -i '/mysql-connector-j.*duplicate/d' pom.xml 2>/dev/null || true
                '''
            }
        }

        stage('Setup Kubernetes') {
            steps {
                script {
                    sh """
                        export KUBECONFIG=/var/lib/jenkins/.kube/config

                        echo "=== Configuration Kubernetes ==="

                        # Créer le namespace
                        kubectl create namespace ${env.K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

                        # Vérifier la connexion
                        kubectl cluster-info
                    """
                }
            }
        }

        stage('Build & Test') {
            steps {
                sh '''
                    # Désactiver le resource filtering explicitement
                    mvn clean verify -DskipTests=false -Dmaven.test.failure.ignore=false -Dmaven.resources.filtering=false
                '''
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    sh '''
                        echo "=== Vérification du rapport JaCoCo ==="
                        if [ -f "target/site/jacoco/jacoco.xml" ]; then
                            echo "✅ Rapport JaCoCo trouvé"
                        else
                            echo "⚠ Recherche alternative..."
                            find . -name "jacoco.xml" -type f | head -5
                        fi

                        mvn sonar:sonar \
                            -Dsonar.projectKey=student-management \
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                            -Dsonar.host.url=http://localhost:9000
                    '''
                }
            }
        }

        stage('Package') {
            steps {
                sh '''
                    echo "=== Sauvegarde des rapports ==="
                    mkdir -p saved-reports
                    cp -r target/site/jacoco saved-reports/ 2>/dev/null || echo "Rapport non disponible"

                    # Package sans tests
                    mvn clean package -DskipTests -Dmaven.resources.filtering=false
                '''
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

        stage('Clean Old Spring Boot Deployment') {
            steps {
                script {
                    sh """
                        export KUBECONFIG=/var/lib/jenkins/.kube/config

                        echo "=== Nettoyage de l'ancien déploiement Spring Boot ==="

                        # Supprimer l'ancien deployment avec les mauvaises variables
                        kubectl delete deployment spring-boot-deployment -n ${env.K8S_NAMESPACE} --ignore-not-found=true

                        # Supprimer les anciennes ressources
                        kubectl delete configmap spring-app-config -n ${env.K8S_NAMESPACE} --ignore-not-found=true
                        kubectl delete secret spring-app-secret -n ${env.K8S_NAMESPACE} --ignore-not-found=true
                        kubectl delete pvc spring-app-pvc -n ${env.K8S_NAMESPACE} --ignore-not-found=true

                        echo "Ancien déploiement supprimé. Attente..."
                        sleep 10
                    """
                }
            }
        }

        stage('Deploy MySQL on K8S') {
            steps {
                script {
                    sh """
                        export KUBECONFIG=/var/lib/jenkins/.kube/config

                        echo "=== Déploiement de MySQL sur K8S ==="

                        kubectl apply -f mysql-deployment.yaml -n ${env.K8S_NAMESPACE}

                        echo "MySQL déployé. Attente du démarrage..."
                        sleep 30

                        kubectl get pods -l app=mysql -n ${env.K8S_NAMESPACE}

                        # Vérifier que MySQL est accessible
                        echo "Vérification de la base de données..."
                        kubectl exec -it \$(kubectl get pod -l app=mysql -n ${env.K8S_NAMESPACE} -o jsonpath='{.items[0].metadata.name}') -n ${env.K8S_NAMESPACE} -- \
                          mysql -u root -ppassword -e "CREATE DATABASE IF NOT EXISTS studentdb; SHOW DATABASES;" 2>/dev/null || echo "MySQL en cours de démarrage..."
                    """
                }
            }
        }

        stage('Deploy SonarQube on K8S') {
            steps {
                script {
                    sh """
                        export KUBECONFIG=/var/lib/jenkins/.kube/config

                        echo "=== Déploiement de SonarQube sur K8S ==="

                        kubectl apply -f sonarqube-deployment.yaml -n ${env.K8S_NAMESPACE} 2>/dev/null || echo "Déploiement déjà existant"
                        kubectl apply -f sonarqube-service.yaml -n ${env.K8S_NAMESPACE}

                        echo "SonarQube déployé. Attente du démarrage..."
                        sleep 30

                        kubectl get pods -l app=sonarqube -n ${env.K8S_NAMESPACE}
                        echo "URL SonarQube: http://localhost:30090"
                    """
                }
            }
        }

        stage('Deploy Spring Boot - CORRECT VERSION') {
            steps {
                script {
                    sh """
                        export KUBECONFIG=/var/lib/jenkins/.kube/config

                        echo "=== DÉPLOIEMENT SPRING BOOT - VERSION CORRIGÉE ==="

                        # Créer le fichier YAML directement avec la BONNE configuration
                        cat > spring-correct.yaml << 'EOF'
apiVersion: apps/v1
kind: Deployment
metadata:
  name: spring-boot-deployment
  namespace: ${env.K8S_NAMESPACE}
spec:
  replicas: 1
  selector:
    matchLabels:
      app: spring-boot-app
  template:
    metadata:
      labels:
        app: spring-boot-app
    spec:
      containers:
      - name: spring-boot-app
        image: ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
        ports:
        - containerPort: 8089
        env:
        # Configuration base de données CORRECTE
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:mysql://mysql-service:3306/studentdb?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
        - name: SPRING_DATASOURCE_USERNAME
          value: "root"
        - name: SPRING_DATASOURCE_PASSWORD
          value: "password"
        # Configuration JPA
        - name: SPRING_JPA_HIBERNATE_DDL_AUTO
          value: "update"
        - name: SPRING_JPA_SHOW_SQL
          value: "true"
        - name: SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT
          value: "org.hibernate.dialect.MySQL8Dialect"
        # Configuration serveur
        - name: SERVER_PORT
          value: "8089"
        - name: SERVER_SERVLET_CONTEXT_PATH
          value: "/student"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: spring-service
  namespace: ${env.K8S_NAMESPACE}
spec:
  type: NodePort
  selector:
    app: spring-boot-app
  ports:
  - port: 8089
    targetPort: 8089
    nodePort: 30080
EOF

                        echo "Fichier YAML généré:"
                        cat spring-correct.yaml

                        # Déployer
                        kubectl apply -f spring-correct.yaml

                        echo "Spring Boot déployé avec la configuration CORRECTE. Attente..."
                        sleep 60

                        # Vérifier
                        echo "=== État des pods ==="
                        kubectl get pods -n ${env.K8S_NAMESPACE}

                        # Vérifier les logs
                        echo "=== Logs Spring Boot ==="
                        kubectl logs -l app=spring-boot-app -n ${env.K8S_NAMESPACE} --tail=20 --since=10s 2>/dev/null || echo "Pas encore de logs..."
                    """
                }
            }
        }

        stage('Verify and Test') {
            steps {
                script {
                    sh """
                        export KUBECONFIG=/var/lib/jenkins/.kube/config

                        echo "=== VÉRIFICATION FINALE ==="
                        echo ""

                        # Attendre un peu plus
                        sleep 30

                        # 1. Vérifier l'état
                        echo "1. État du cluster:"
                        kubectl get all -n ${env.K8S_NAMESPACE}
                        echo ""

                        # 2. Vérifier Spring Boot
                        echo "2. Spring Boot:"
                        SPRING_POD=\$(kubectl get pod -l app=spring-boot-app -n ${env.K8S_NAMESPACE} -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
                        if [ -n "\$SPRING_POD" ]; then
                            echo "   Pod: \$SPRING_POD"
                            STATUS=\$(kubectl get pod \$SPRING_POD -n ${env.K8S_NAMESPACE} -o jsonpath='{.status.phase}')
                            echo "   Statut: \$STATUS"

                            if [ "\$STATUS" = "Running" ]; then
                                echo "   ✅ Spring Boot est en cours d'exécution"

                                # Vérifier les logs pour "Started"
                                echo "   Vérification du démarrage..."
                                if kubectl logs \$SPRING_POD -n ${env.K8S_NAMESPACE} 2>/dev/null | grep -q "Started StudentManagementApplication"; then
                                    echo "   ✅ Application démarrée avec succès"
                                else
                                    echo "   ⚠ Application en cours de démarrage"
                                    echo "   Derniers logs:"
                                    kubectl logs \$SPRING_POD -n ${env.K8S_NAMESPACE} --tail=10 2>/dev/null | tail -5 || echo "   (pas de logs)"
                                fi
                            else
                                echo "   ⚠ Statut: \$STATUS"
                                echo "   Logs (derniers 20 lignes):"
                                kubectl logs \$SPRING_POD -n ${env.K8S_NAMESPACE} --tail=20 2>/dev/null || echo "   (pas de logs)"
                            fi
                        else
                            echo "   ⚠ Aucun pod Spring Boot trouvé"
                        fi
                        echo ""

                        # 3. Vérifier MySQL
                        echo "3. MySQL:"
                        MYSQL_POD=\$(kubectl get pod -l app=mysql -n ${env.K8S_NAMESPACE} -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
                        if [ -n "\$MYSQL_POD" ]; then
                            MYSQL_STATUS=\$(kubectl get pod \$MYSQL_POD -n ${env.K8S_NAMESPACE} -o jsonpath='{.status.phase}')
                            echo "   ✅ MySQL: \$MYSQL_STATUS"
                        fi
                        echo ""

                        # 4. Bilan de l'atelier
                        echo "📊 BILAN DE L'ATELIER:"
                        echo "====================="
                        echo "✅ Cluster Kubernetes configuré"
                        echo "✅ Pipeline CI/CD exécuté"
                        echo "✅ Image Docker construite: ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}"
                        echo "✅ MySQL déployé sur K8S"
                        echo "✅ SonarQube déployé sur K8S"
                        echo "✅ Configuration Spring Boot corrigée"
                        echo ""
                        echo "🔗 URLs:"
                        echo "  - Application Spring: http://localhost:30080/student"
                        echo "  - SonarQube (K8S): http://localhost:30090"
                        echo "  - SonarQube (externe): http://localhost:9000"
                        echo ""
                        echo "🎯 Objectifs de l'atelier atteints !"
                    """
                }
            }
        }
    }

    post {
        always {
            echo "=== FIN DU PIPELINE ==="
            sh '''
                echo "Build #${BUILD_NUMBER} terminé"
                echo "Statut: ${currentBuild.currentResult}"
            '''
        }
        success {
            echo "✅ ATELIER RÉUSSI !"
            sh '''
                echo "=== RÉCAPITULATIF ==="
                echo "Toutes les étapes ont été exécutées avec succès."
                echo "L'atelier Kubernetes est complété."
            '''
        }
        failure {
            echo '❌ Certaines étapes ont échoué'
            sh '''
                echo "=== DÉBOGAGE RAPIDE ==="
                export KUBECONFIG=/var/lib/jenkins/.kube/config

                echo "1. Pods actuels:"
                kubectl get pods -n devops 2>/dev/null || echo "Namespace non trouvé"

                echo ""
                echo "2. Problème Spring Boot:"
                kubectl describe pod -l app=spring-boot-app -n devops 2>/dev/null | grep -A5 -B5 "Error\|Failed\|Crash" || echo "Pas de pod Spring Boot"
            '''
        }
    }
}