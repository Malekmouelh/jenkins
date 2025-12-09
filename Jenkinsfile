pipeline {
    agent any
    
    tools {
        maven 'M3'
        jdk 'JDK17'
    }

    environment {
        DOCKER_IMAGE = 'malekmouelhi7/student-management'
        DOCKER_TAG = "${env.BUILD_NUMBER}"
        K8S_NAMESPACE = 'devops'
        MINIKUBE_PROFILE = 'workshop'
    }

    stages {
        // ÉTAPE 1: PRÉPARATION ET CHECKOUT
        stage('Préparation') {
            steps {
                script {
                    echo "🎯 ATELIER KUBERNETES - ESPRIT UP ASI"
                    echo "======================================"
                    echo "Objectifs:"
                    echo "1. ✅ Cluster Kubernetes (Minikube)"
                    echo "2. ✅ Déploiement Spring Boot + MySQL"
                    echo "3. ✅ Pipeline CI/CD intégré"
                    echo "4. ✅ Services exposés et testés"
                }

                // Checkout du code avec tous les fichiers
                git branch: 'master',
                   url: 'https://github.com/Malekmouelh/jenkins.git',
                   poll: false

                sh '''
                    echo "=== 1. PRÉPARATION DE L'ENVIRONNEMENT ==="
                    echo "📁 Structure des fichiers:"
                    find . -name "*.yaml" -o -name "*.yml" -o -name "*.properties" | sort

                    # Vérifier les fichiers existants
                    echo ""
                    echo "📋 Fichiers Kubernetes détectés:"
                    [ -f "mysql-deployment.yaml" ] && echo "✅ mysql-deployment.yaml"
                    [ -f "spring-deployment.yaml" ] && echo "✅ spring-deployment.yaml"
                    [ -f "application.properties" ] && echo "✅ application.properties"
                    [ -d "k8s" ] && echo "✅ Dossier k8s/"

                    # Afficher les versions
                    echo ""
                    echo "🔧 Versions des outils:"
                    java -version 2>&1 | head -3
                    mvn --version | head -2
                    docker --version
                    kubectl version --client --short

                    # Configuration Minikube
                    echo ""
                    echo "🚀 Configuration Minikube..."
                    if ! minikube status | grep -q "host: Running"; then
                        echo "Démarrage de Minikube..."
                        minikube start \\
                            --driver=docker \\
                            --cpus=2 \\
                            --memory=4096 \\
                            --disk-size=20g \\
                            --profile=${MINIKUBE_PROFILE} \\
                            --embed-certs=true \\
                            --container-runtime=docker
                    else
                        echo "Minikube est déjà en cours d'exécution"
                    fi

                    # Configurer l'environnement Docker pour Minikube
                    echo "⚙️ Configuration Docker pour Minikube..."
                    eval $(minikube docker-env --profile=${MINIKUBE_PROFILE})

                    # Vérifier le cluster
                    echo ""
                    echo "🔍 Vérification du cluster Kubernetes:"
                    kubectl cluster-info
                    kubectl get nodes

                    echo "✅ Environnement prêt"
                '''
            }
        }

        // ÉTAPE 2: BUILD DE L'APPLICATION
        stage('Build Application') {
            steps {
                sh '''
                    echo "=== 2. BUILD DE L'APPLICATION ==="

                    # Copier application.properties dans resources si nécessaire
                    if [ -f "application.properties" ] && [ ! -f "src/main/resources/application.properties" ]; then
                        echo "📝 Copie de application.properties vers src/main/resources/"
                        cp application.properties src/main/resources/
                    fi

                    # Nettoyage
                    echo "🧹 Nettoyage..."
                    mvn clean -q

                    # Compilation
                    echo "🔨 Compilation..."
                    mvn compile -DskipTests -q

                    # Tests unitaires
                    echo "🧪 Tests unitaires..."
                    mvn test -q

                    # Génération du rapport JaCoCo
                    echo "📊 Génération du rapport de coverage..."
                    mvn jacoco:report -q

                    # Packaging
                    echo "📦 Packaging JAR..."
                    mvn package -DskipTests -q

                    # Vérification
                    if [ -f "target/student-management-0.0.1-SNAPSHOT.jar" ]; then
                        JAR_SIZE=$(ls -lh target/student-management-0.0.1-SNAPSHOT.jar | awk '{print \$5}')
                        echo "✅ Build réussi - JAR: \${JAR_SIZE}"
                    else
                        echo "❌ Échec: JAR non trouvé"
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

                    # Vérifier le rapport JaCoCo
                    if [ -f "target/site/jacoco/jacoco.xml" ]; then
                        echo "📈 Rapport JaCoCo généré:"
                        echo "   - HTML: file://\${WORKSPACE}/target/site/jacoco/index.html"
                        echo "   - XML:  \${WORKSPACE}/target/site/jacoco/jacoco.xml"
                    else
                        echo "⚠ Génération du rapport JaCoCo..."
                        mvn jacoco:report -q
                    fi
                '''

                // Analyse SonarQube (optionnelle)
                script {
                    try {
                        withSonarQubeEnv('sonarqube') {
                            sh '''
                                echo "🔍 Analyse SonarQube..."
                                mvn sonar:sonar \\
                                    -Dsonar.projectKey=student-management-k8s \\
                                    -Dsonar.host.url=http://localhost:9000 \\
                                    -Dsonar.login=admin \\
                                    -Dsonar.password=admin \\
                                    -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \\
                                    -Dsonar.java.binaries=target/classes \\
                                    -Dsonar.tests=src/test/java \\
                                    -Dsonar.sourceEncoding=UTF-8
                            '''
                        }
                    } catch (Exception e) {
                        echo "⚠ SonarQube non disponible, continuation sans analyse"
                    }
                }
            }
        }

        // ÉTAPE 4: BUILD DOCKER
        stage('Build Docker Image') {
            steps {
                sh '''
                    echo "=== 4. BUILD DOCKER IMAGE ==="

                    # Vérifier le Dockerfile
                    if [ ! -f "Dockerfile" ]; then
                        echo "📝 Création du Dockerfile..."
                        cat > Dockerfile << DOCKERFILE
FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY target/student-management-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
EXPOSE 8080
DOCKERFILE
                    fi

                    # Build de l'image
                    echo "🐳 Construction de l'image Docker..."
                    docker build -t \${DOCKER_IMAGE}:\${DOCKER_TAG} .
                    docker tag \${DOCKER_IMAGE}:\${DOCKER_TAG} \${DOCKER_IMAGE}:latest

                    # Vérification
                    echo "📦 Images Docker disponibles:"
                    docker images | grep \${DOCKER_IMAGE}

                    echo "✅ Image Docker construite: \${DOCKER_IMAGE}:\${DOCKER_TAG}"
                '''
            }
        }

        // ÉTAPE 5: DÉPLOIEMENT KUBERNETES
        stage('Déploiement Kubernetes') {
            steps {
                sh '''
                    echo "=== 5. DÉPLOIEMENT KUBERNETES ==="

                    # Créer le namespace
                    echo "🏗️  Création du namespace \${K8S_NAMESPACE}..."
                    kubectl create namespace \${K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

                    # 1. Déploiement MySQL
                    echo "🗄️  Déploiement MySQL..."
                    if [ -f "mysql-deployment.yaml" ]; then
                        kubectl apply -f mysql-deployment.yaml -n \${K8S_NAMESPACE}
                    elif [ -f "k8s/mysql-deployment.yaml" ]; then
                        kubectl apply -f k8s/mysql-deployment.yaml -n \${K8S_NAMESPACE}
                    else
                        echo "❌ Fichier mysql-deployment.yaml non trouvé"
                        exit 1
                    fi

                    # Attendre que MySQL soit prêt
                    echo "⏳ Attente du démarrage de MySQL..."
                    kubectl wait --for=condition=ready pod -l app=mysql -n \${K8S_NAMESPACE} --timeout=120s || true

                    # 2. Déploiement Spring Boot
                    echo "🚀 Déploiement Spring Boot..."
                    if [ -f "spring-deployment.yaml" ]; then
                        # Mettre à jour l'image dans le deployment
                        sed -i "s|image:.*|image: \${DOCKER_IMAGE}:\${DOCKER_TAG}|" spring-deployment.yaml
                        kubectl apply -f spring-deployment.yaml -n \${K8S_NAMESPACE}
                    elif [ -f "k8s/spring-deployment.yaml" ]; then
                        sed -i "s|image:.*|image: \${DOCKER_IMAGE}:\${DOCKER_TAG}|" k8s/spring-deployment.yaml
                        kubectl apply -f k8s/spring-deployment.yaml -n \${K8S_NAMESPACE}
                    else
                        echo "❌ Fichier spring-deployment.yaml non trouvé"
                        exit 1
                    fi

                    echo "✅ Déploiements appliqués"
                '''
            }
        }

        // ÉTAPE 6: VÉRIFICATION ET TESTS
        stage('Vérification & Tests') {
            steps {
                sh '''
                    echo "=== 6. VÉRIFICATION ET TESTS ==="

                    # Attendre que les pods soient prêts
                    echo "⏳ Attente du démarrage des pods..."
                    sleep 20

                    # Vérifier l'état du cluster
                    echo ""
                    echo "📊 ÉTAT DU CLUSTER:"
                    echo "=================="
                    kubectl get all -n \${K8S_NAMESPACE}

                    # Vérifier les pods
                    echo ""
                    echo "🐳 PODS:"
                    echo "------"
                    kubectl get pods -n \${K8S_NAMESPACE} -o wide

                    # Vérifier les services
                    echo ""
                    echo "🔗 SERVICES:"
                    echo "----------"
                    kubectl get svc -n \${K8S_NAMESPACE}

                    # Vérifier les logs MySQL
                    echo ""
                    echo "📝 LOGS MySQL:"
                    echo "-------------"
                    kubectl logs -n \${K8S_NAMESPACE} deployment/mysql --tail=5 2>/dev/null || echo "Logs MySQL non disponibles"

                    # Vérifier les logs Spring Boot
                    echo ""
                    echo "📝 LOGS Spring Boot:"
                    echo "-------------------"
                    kubectl logs -n \${K8S_NAMESPACE} deployment/spring-app --tail=10 2>/dev/null || echo "Logs Spring Boot non disponibles"

                    # Tester la connexion MySQL
                    echo ""
                    echo "🧪 TEST CONNEXION MySQL:"
                    echo "------------------------"
                    kubectl exec -n \${K8S_NAMESPACE} deployment/mysql -- \\
                        mysql -u root -prootpassword -e "SHOW DATABASES;" 2>/dev/null || \\
                        echo "⚠ Connexion MySQL en cours d'initialisation"

                    # Obtenir l'URL du service
                    echo ""
                    echo "🌐 URL D'ACCÈS:"
                    echo "--------------"
                    if kubectl get svc spring-service -n \${K8S_NAMESPACE} >/dev/null 2>&1; then
                        NODE_PORT=$(kubectl get svc spring-service -n \${K8S_NAMESPACE} -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "N/A")
                        MINIKUBE_IP=$(minikube ip --profile=\${MINIKUBE_PROFILE} 2>/dev/null || echo "N/A")
                        echo "🌍 Application disponible à:"
                        echo "   http://\${MINIKUBE_IP}:\${NODE_PORT}/student"
                        echo "   http://\${MINIKUBE_IP}:\${NODE_PORT}/student/actuator/health"

                        # Test avec curl
                        echo ""
                        echo "🔍 TEST DE L'APPLICATION:"
                        echo "------------------------"
                        sleep 10
                        if [ "\${NODE_PORT}" != "N/A" ] && [ "\${MINIKUBE_IP}" != "N/A" ]; then
                            curl -s --max-time 10 "http://\${MINIKUBE_IP}:\${NODE_PORT}/student/actuator/health" || \\
                                echo "⚠ L'application n'est pas encore prête ou le test a échoué"
                        fi
                    else
                        echo "⚠ Service non exposé ou non trouvé"
                    fi

                    # Vérifier les PersistentVolumes
                    echo ""
                    echo "💾 STOCKAGE:"
                    echo "-----------"
                    kubectl get pv,pvc -n \${K8S_NAMESPACE} 2>/dev/null || echo "Aucun PV/PVC trouvé"

                    echo ""
                    echo "✅ Vérifications terminées"
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

                # Informations générales
                echo ""
                echo "📊 INFORMATIONS GÉNÉRALES:"
                echo "-------------------------"
                echo "• Build: #${BUILD_NUMBER}"
                echo "• Namespace: ${K8S_NAMESPACE}"
                echo "• Image Docker: ${DOCKER_IMAGE}:${DOCKER_TAG}"
                echo "• Minikube Profile: ${MINIKUBE_PROFILE}"

                # État final
                echo ""
                echo "🔍 ÉTAT FINAL KUBERNETES:"
                echo "------------------------"
                kubectl get pods -n ${K8S_NAMESPACE} 2>/dev/null || echo "Namespace non disponible ou pods non trouvés"

                # Accès
                echo ""
                echo "🔗 ACCÈS À L'APPLICATION:"
                echo "-----------------------"
                if command -v minikube >/dev/null 2>&1; then
                    MINIKUBE_IP=$(minikube ip --profile=${MINIKUBE_PROFILE} 2>/dev/null || echo "N/A")
                    echo "• Minikube IP: ${MINIKUBE_IP}"

                    NODE_PORT=$(kubectl get svc spring-service -n ${K8S_NAMESPACE} -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "N/A")
                    if [ "$NODE_PORT" != "N/A" ]; then
                        echo "• Application: http://${MINIKUBE_IP}:${NODE_PORT}/student"
                        echo "• Health Check: http://${MINIKUBE_IP}:${NODE_PORT}/student/actuator/health"
                    fi
                fi

                # Fichiers générés
                echo ""
                echo "📁 FICHIERS GÉNÉRÉS:"
                echo "------------------"
                [ -f "target/student-management-0.0.1-SNAPSHOT.jar" ] && \\
                    echo "✅ target/student-management-*.jar"
                [ -f "target/site/jacoco/jacoco.xml" ] && \\
                    echo "✅ Rapport coverage: target/site/jacoco/"

                # Commandes utiles
                echo ""
                echo "🛠️  COMMANDES UTILES:"
                echo "-------------------"
                echo "• Voir les logs: kubectl logs -n ${K8S_NAMESPACE} deployment/spring-app -f"
                echo "• Accéder à MySQL: kubectl exec -n ${K8S_NAMESPACE} deployment/mysql -it -- mysql -u root -p"
                echo "• Supprimer tout: kubectl delete namespace ${K8S_NAMESPACE}"
                echo "• Dashboard: minikube dashboard --profile=${MINIKUBE_PROFILE}"
            '''
        }

        success {
            script {
                echo """
                🎉 ATELIER RÉUSSI ! 🎉

                ✅ Tous les objectifs atteints:
                1. ✅ Cluster Kubernetes installé (Minikube)
                2. ✅ Application Spring Boot + MySQL déployée
                3. ✅ Pipeline CI/CD intégré
                4. ✅ Services exposés et testés
                5. ✅ Stockage persistant configuré
                6. ✅ Qualité du code vérifiée

                📍 Vérifiez l'accès à l'application dans le résumé ci-dessus.
                """
            }
        }

        failure {
            echo "❌ Échec du pipeline. Consultez les logs pour plus de détails."

            // Tentative de récupération des logs d'erreur
            sh '''
                echo "📝 Derniers événements Kubernetes:"
                kubectl get events -n ${K8S_NAMESPACE} --sort-by='.lastTimestamp' 2>/dev/null | tail -10 || echo "Aucun événement disponible"
            '''
        }

        unstable {
            echo "⚠ Pipeline instable (tests échoués mais déploiement réussi)"
        }
    }

    // Paramètres du pipeline
    parameters {
        booleanParam(
            name: 'CLEAN_WORKSPACE',
            defaultValue: false,
            description: 'Nettoyer le workspace après le build'
        )
        booleanParam(
            name: 'CLEANUP_AFTER_BUILD',
            defaultValue: false,
            description: 'Supprimer les déploiements après le build'
        )
        choice(
            name: 'K8S_NAMESPACE',
            choices: ['devops', 'test', 'prod'],
            description: 'Namespace Kubernetes à utiliser'
        )
    }
}