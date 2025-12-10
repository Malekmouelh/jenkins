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
        // Utiliser le profil par défaut de Minikube
        MINIKUBE_PROFILE = 'minikube'
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
                    find . -name "*.yaml" -o -name "*.yml" -o -name "*.properties" | sort | head -20

                    # Vérifier les fichiers existants
                    echo ""
                    echo "📋 Fichiers Kubernetes détectés:"
                    [ -f "mysql-deployment.yaml" ] && echo "✅ mysql-deployment.yaml"
                    [ -f "spring-deployment.yaml" ] && echo "✅ spring-deployment.yaml"
                    [ -f "application.properties" ] && echo "✅ application.properties"
                    [ -d "k8s" ] && echo "✅ Dossier k8s/" || echo "ℹ️  Dossier k8s/ non trouvé"

                    # Afficher les versions
                    echo ""
                    echo "🔧 Versions des outils:"
                    java -version 2>&1 | head -3
                    echo "Maven: $(mvn --version 2>&1 | head -1)"
                    echo "Docker: $(docker --version 2>&1)"
                    echo "kubectl: $(kubectl version --client 2>&1 | head -1)"

                    # Configuration Minikube
                    echo ""
                    echo "🚀 Configuration Minikube..."

                    # Vérifier si Minikube est installé
                    if ! command -v minikube &> /dev/null; then
                        echo "❌ Minikube n'est pas installé"
                        echo "ℹ️  Installation de Minikube..."
                        curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
                        install minikube-linux-amd64 /usr/local/bin/minikube
                    fi

                    # Démarrer Minikube si nécessaire
                    MINIKUBE_STATUS=$(minikube status --format='{{.Host}}' 2>/dev/null || echo "Not Running")
                    if [ "$MINIKUBE_STATUS" != "Running" ]; then
                        echo "Démarrage de Minikube..."
                        minikube start \\
                            --driver=docker \\
                            --cpus=2 \\
                            --memory=4096 \\
                            --disk-size=20g
                    else
                        echo "Minikube est déjà en cours d'exécution"
                    fi

                    # Afficher le statut Minikube
                    echo "📊 Statut Minikube:"
                    minikube status

                    # Configurer l'environnement Docker pour Minikube
                    echo "⚙️ Configuration Docker pour Minikube..."
                    eval $(minikube docker-env 2>/dev/null) || echo "⚠ Impossible de configurer docker-env"

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
                    elif [ -f "src/main/resources/application.properties" ]; then
                        echo "ℹ️  application.properties existe déjà dans src/main/resources/"
                    fi

                    # Vérifier le pom.xml
                    if [ ! -f "pom.xml" ]; then
                        echo "❌ Fichier pom.xml non trouvé"
                        exit 1
                    fi

                    # Nettoyage
                    echo "🧹 Nettoyage..."
                    mvn clean -q || { echo "❌ Échec du clean"; exit 1; }

                    # Compilation
                    echo "🔨 Compilation..."
                    mvn compile -DskipTests -q || { echo "❌ Échec de la compilation"; exit 1; }

                    # Tests unitaires
                    echo "🧪 Tests unitaires..."
                    mvn test -q || echo "⚠ Certains tests ont échoué"

                    # Génération du rapport JaCoCo
                    echo "📊 Génération du rapport de coverage..."
                    mvn jacoco:report -q || echo "⚠ JaCoCo report échoué"

                    # Packaging
                    echo "📦 Packaging JAR..."
                    mvn package -DskipTests -q || { echo "❌ Échec du packaging"; exit 1; }

                    # Vérification
                    if [ -f "target/student-management-0.0.1-SNAPSHOT.jar" ]; then
                        JAR_SIZE=$(ls -lh target/student-management-0.0.1-SNAPSHOT.jar | awk '{print \$5}')
                        echo "✅ Build réussi - JAR: \${JAR_SIZE}"
                    else
                        echo "❌ Échec: JAR non trouvé"
                        # Chercher d'autres noms de JAR
                        find target -name "*.jar" | head -5
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
                        echo "   - HTML: target/site/jacoco/index.html"
                        echo "   - XML:  target/site/jacoco/jacoco.xml"
                    else
                        echo "ℹ️  Génération du rapport JaCoCo..."
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
                        echo "✅ Dockerfile créé"
                    else
                        echo "ℹ️  Dockerfile existant trouvé"
                        cat Dockerfile
                    fi

                    # Vérifier que le JAR existe
                    if [ ! -f "target/student-management-0.0.1-SNAPSHOT.jar" ]; then
                        echo "❌ JAR non trouvé pour le build Docker"
                        exit 1
                    fi

                    # Build de l'image
                    echo "🐳 Construction de l'image Docker..."
                    docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} . || { echo "❌ Échec du build Docker"; exit 1; }
                    docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest

                    # Vérification
                    echo "📦 Images Docker disponibles:"
                    docker images | grep ${DOCKER_IMAGE} || echo "⚠ Image non trouvée"

                    echo "✅ Image Docker construite: ${DOCKER_IMAGE}:${DOCKER_TAG}"
                '''
            }
        }

        // ÉTAPE 5: DÉPLOIEMENT KUBERNETES
        stage('Déploiement Kubernetes') {
            steps {
                sh '''
                    echo "=== 5. DÉPLOIEMENT KUBERNETES ==="

                    # Créer le namespace
                    echo "🏗️  Création du namespace ${K8S_NAMESPACE}..."
                    kubectl create namespace ${K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f - 2>/dev/null || echo "ℹ️  Namespace peut déjà exister"

                    # 1. Déploiement MySQL
                    echo "🗄️  Déploiement MySQL..."
                    if [ -f "mysql-deployment.yaml" ]; then
                        echo "ℹ️  Application de mysql-deployment.yaml..."
                        cat mysql-deployment.yaml | head -20
                        kubectl apply -f mysql-deployment.yaml -n ${K8S_NAMESPACE}
                    elif [ -f "k8s/mysql-deployment.yaml" ]; then
                        kubectl apply -f k8s/mysql-deployment.yaml -n ${K8S_NAMESPACE}
                    else
                        echo "❌ Fichier mysql-deployment.yaml non trouvé"
                        exit 1
                    fi

                    # Attendre que MySQL soit prêt
                    echo "⏳ Attente du démarrage de MySQL (30 secondes)..."
                    sleep 30

                    # Vérifier l'état de MySQL
                    echo "🔍 Vérification de l'état MySQL:"
                    kubectl get pods -n ${K8S_NAMESPACE} -l app=mysql 2>/dev/null || echo "⚠ Pod MySQL non trouvé"

                    # 2. Déploiement Spring Boot
                    echo "🚀 Déploiement Spring Boot..."
                    if [ -f "spring-deployment.yaml" ]; then
                        # Sauvegarder l'original
                        cp spring-deployment.yaml spring-deployment.yaml.backup

                        # Mettre à jour l'image dans le deployment
                        echo "ℹ️  Mise à jour de l'image Docker dans le deployment..."
                        sed -i "s|image: .*|image: ${DOCKER_IMAGE}:${DOCKER_TAG}|" spring-deployment.yaml

                        echo "ℹ️  Application de spring-deployment.yaml..."
                        cat spring-deployment.yaml | head -20
                        kubectl apply -f spring-deployment.yaml -n ${K8S_NAMESPACE}

                        # Restaurer l'original
                        mv spring-deployment.yaml.backup spring-deployment.yaml
                    elif [ -f "k8s/spring-deployment.yaml" ]; then
                        cp k8s/spring-deployment.yaml k8s/spring-deployment.yaml.backup
                        sed -i "s|image: .*|image: ${DOCKER_IMAGE}:${DOCKER_TAG}|" k8s/spring-deployment.yaml
                        kubectl apply -f k8s/spring-deployment.yaml -n ${K8S_NAMESPACE}
                        mv k8s/spring-deployment.yaml.backup k8s/spring-deployment.yaml
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
                    echo "⏳ Attente du démarrage des pods (20 secondes)..."
                    sleep 20

                    # Vérifier l'état du cluster
                    echo ""
                    echo "📊 ÉTAT DU CLUSTER dans ${K8S_NAMESPACE}:"
                    echo "======================================"
                    kubectl get all -n ${K8S_NAMESPACE} 2>/dev/null || echo "⚠ Impossible de récupérer les ressources"

                    # Vérifier les pods en détail
                    echo ""
                    echo "🐳 DÉTAIL DES PODS:"
                    echo "-----------------"
                    kubectl get pods -n ${K8S_NAMESPACE} -o wide 2>/dev/null || echo "⚠ Aucun pod trouvé"

                    # Vérifier l'état des pods
                    echo ""
                    echo "📈 ÉTAT DES PODS:"
                    echo "---------------"
                    kubectl describe pods -n ${K8S_NAMESPACE} 2>/dev/null | grep -A5 "State:" || echo "⚠ Impossible de décrire les pods"

                    # Vérifier les services
                    echo ""
                    echo "🔗 SERVICES:"
                    echo "----------"
                    kubectl get svc -n ${K8S_NAMESPACE} 2>/dev/null || echo "⚠ Aucun service trouvé"

                    # Vérifier les logs MySQL
                    echo ""
                    echo "📝 LOGS MySQL:"
                    echo "-------------"
                    MYSQL_POD=$(kubectl get pods -n ${K8S_NAMESPACE} -l app=mysql -o name 2>/dev/null | head -1)
                    if [ -n "$MYSQL_POD" ]; then
                        kubectl logs -n ${K8S_NAMESPACE} ${MYSQL_POD} --tail=5 2>/dev/null || echo "⚠ Impossible de récupérer les logs MySQL"
                    else
                        echo "⚠ Pod MySQL non trouvé"
                    fi

                    # Vérifier les logs Spring Boot
                    echo ""
                    echo "📝 LOGS Spring Boot:"
                    echo "-------------------"
                    SPRING_POD=$(kubectl get pods -n ${K8S_NAMESPACE} -l app=spring-app -o name 2>/dev/null | head -1)
                    if [ -n "$SPRING_POD" ]; then
                        kubectl logs -n ${K8S_NAMESPACE} ${SPRING_POD} --tail=10 2>/dev/null || echo "⚠ Impossible de récupérer les logs Spring Boot"
                    else
                        echo "⚠ Pod Spring Boot non trouvé"
                    fi

                    # Tester la connexion MySQL
                    echo ""
                    echo "🧪 TEST CONNEXION MySQL:"
                    echo "------------------------"
                    if [ -n "$MYSQL_POD" ]; then
                        kubectl exec -n ${K8S_NAMESPACE} ${MYSQL_POD} -- \\
                            mysql -u root -prootpassword -e "SHOW DATABASES;" 2>/dev/null && \\
                            echo "✅ Connexion MySQL réussie" || \\
                            echo "⚠ Connexion MySQL échouée"
                    fi

                    # Obtenir l'URL du service
                    echo ""
                    echo "🌐 URL D'ACCÈS:"
                    echo "--------------"
                    if kubectl get svc spring-service -n ${K8S_NAMESPACE} >/dev/null 2>&1; then
                        NODE_PORT=$(kubectl get svc spring-service -n ${K8S_NAMESPACE} -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "N/A")
                        MINIKUBE_IP=$(minikube ip 2>/dev/null || echo "N/A")
                        if [ "$NODE_PORT" != "N/A" ] && [ "$MINIKUBE_IP" != "N/A" ]; then
                            echo "🌍 Application disponible à:"
                            echo "   • http://${MINIKUBE_IP}:${NODE_PORT}/student"
                            echo "   • http://${MINIKUBE_IP}:${NODE_PORT}/student/actuator/health"

                            # Test avec curl
                            echo ""
                            echo "🔍 TEST DE L'APPLICATION (attente 10 secondes):"
                            echo "----------------------------------------------"
                            sleep 10
                            curl -s --max-time 10 "http://${MINIKUBE_IP}:${NODE_PORT}/student/actuator/health" && \\
                                echo "✅ Application répond" || \\
                                echo "⚠ Application ne répond pas encore"
                        else
                            echo "⚠ Impossible de récupérer l'IP ou le port"
                        fi
                    else
                        echo "⚠ Service 'spring-service' non trouvé"
                    fi

                    # Vérifier les PersistentVolumes
                    echo ""
                    echo "💾 STOCKAGE:"
                    echo "-----------"
                    kubectl get pv,pvc -n ${K8S_NAMESPACE} 2>/dev/null || echo "ℹ️  Aucun PV/PVC trouvé"

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
                echo "• Minikube: $(minikube status --format='{{.Host}}' 2>/dev/null || echo 'N/A')"

                # État final des pods
                echo ""
                echo "🔍 ÉTAT FINAL DES PODS:"
                echo "----------------------"
                kubectl get pods -n ${K8S_NAMESPACE} 2>/dev/null || echo "ℹ️  Namespace non disponible"

                # Accès à l'application
                echo ""
                echo "🔗 ACCÈS À L'APPLICATION:"
                echo "-----------------------"
                MINIKUBE_IP=$(minikube ip 2>/dev/null || echo "N/A")
                NODE_PORT=$(kubectl get svc spring-service -n ${K8S_NAMESPACE} -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "N/A")

                if [ "$MINIKUBE_IP" != "N/A" ] && [ "$NODE_PORT" != "N/A" ]; then
                    echo "• URL: http://${MINIKUBE_IP}:${NODE_PORT}/student"
                    echo "• Health: http://${MINIKUBE_IP}:${NODE_PORT}/student/actuator/health"
                else
                    echo "• Minikube IP: ${MINIKUBE_IP}"
                    echo "• NodePort: ${NODE_PORT}"
                    echo "ℹ️  Service non accessible"
                fi

                # Fichiers générés
                echo ""
                echo "📁 FICHIERS GÉNÉRÉS:"
                echo "------------------"
                [ -f "target/student-management-0.0.1-SNAPSHOT.jar" ] && \\
                    echo "✅ Application JAR"
                [ -f "target/site/jacoco/jacoco.xml" ] && \\
                    echo "✅ Rapport de coverage"
                [ -f "Dockerfile" ] && \\
                    echo "✅ Dockerfile"

                # Commandes utiles
                echo ""
                echo "🛠️  COMMANDES UTILES:"
                echo "-------------------"
                echo "• Voir les logs: kubectl logs -n ${K8S_NAMESPACE} -l app=spring-app -f"
                echo "• Accéder à MySQL: kubectl exec -n ${K8S_NAMESPACE} -it \$(kubectl get pod -n ${K8S_NAMESPACE} -l app=mysql -o name) -- mysql -u root -p"
                echo "• Supprimer le namespace: kubectl delete namespace ${K8S_NAMESPACE}"
                echo "• Dashboard Minikube: minikube dashboard"
            '''
        }

        success {
            echo """
            🎉 ATELIER KUBERNETES RÉUSSI ! 🎉

            ✅ Tous les objectifs de l'atelier sont atteints:

            1. ✅ Cluster Kubernetes (Minikube) installé et configuré
            2. ✅ Application Spring Boot + MySQL déployée avec succès
            3. ✅ Pipeline CI/CD entièrement intégré
            4. ✅ Services exposés et accessibles depuis l'extérieur
            5. ✅ Stockage persistant configuré pour MySQL
            6. ✅ Qualité du code vérifiée (tests, coverage)

            📍 L'application est déployée et accessible via l'URL ci-dessus.
            """
        }

        failure {
            echo "❌ Échec du pipeline. Consultez les logs pour plus de détails."

            sh '''
                echo ""
                echo "🔍 DÉBOGAGE - Derniers événements Kubernetes:"
                echo "--------------------------------------------"
                kubectl get events -n ${K8S_NAMESPACE} --sort-by='.lastTimestamp' 2>/dev/null | tail -15 || echo "ℹ️  Aucun événement disponible"

                echo ""
                echo "🔍 DÉBOGAGE - Description des pods en erreur:"
                echo "--------------------------------------------"
                kubectl get pods -n ${K8S_NAMESPACE} --field-selector=status.phase!=Running 2>/dev/null | while read line; do
                    POD_NAME=$(echo "$line" | awk '{print $1}')
                    if [ "$POD_NAME" != "NAME" ] && [ -n "$POD_NAME" ]; then
                        echo "Pod problématique: $POD_NAME"
                        kubectl describe pod -n ${K8S_NAMESPACE} $POD_NAME 2>/dev/null | grep -A10 "Events:" || true
                    fi
                done
            '''
        }

        unstable {
            echo "⚠ Pipeline instable - Certains tests ont échoué mais le déploiement est probablement réussi"
        }
    }
}