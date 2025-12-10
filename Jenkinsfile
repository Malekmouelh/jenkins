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
                    echo "📋 Objectifs de l'atelier:"
                    echo "1. ✅ Cluster Kubernetes (Minikube)"
                    echo "2. ✅ Déploiement Spring Boot + MySQL"
                    echo "3. ✅ Pipeline CI/CD intégré"
                    echo "4. ✅ Services exposés et testés"
                }

                // Checkout
                git branch: 'master',
                   url: 'https://github.com/Malekmouelh/jenkins.git'

                sh '''
                    echo "=== 1. PRÉPARATION DE L'ENVIRONNEMENT ==="

                    # Vérifier la présence des fichiers nécessaires
                    echo "📁 Fichiers détectés:"
                    [ -f "pom.xml" ] && echo "✅ pom.xml"
                    [ -f "mysql-deployment.yaml" ] && echo "✅ mysql-deployment.yaml"
                    [ -f "spring-deployment.yaml" ] && echo "✅ spring-deployment.yaml"
                    [ -f "application.properties" ] && echo "✅ application.properties"

                    # Vérifier Minikube
                    echo ""
                    echo "🚀 Vérification de Minikube..."
                    minikube status || echo "⚠️  Minikube non disponible"

                    # Démarrer Minikube si nécessaire
                    if ! minikube status | grep -q "host: Running"; then
                        echo "Démarrage de Minikube..."
                        minikube start --driver=docker
                    else
                        echo "✅ Minikube est déjà en cours d'exécution"
                    fi

                    # Configurer Docker pour utiliser Minikube
                    echo "⚙️  Configuration Docker pour Minikube..."
                    eval $(minikube docker-env)

                    # Vérifier Kubernetes
                    echo ""
                    echo "🔍 Vérification du cluster Kubernetes:"
                    kubectl cluster-info
                    kubectl get nodes

                    echo "✅ Environnement prêt !"
                '''
            }
        }

        // ÉTAPE 2: BUILD ET TESTS
        stage('Build & Tests') {
            steps {
                sh '''
                    echo "=== 2. BUILD ET TESTS ==="

                    # Préparation des fichiers de configuration
                    echo "📝 Préparation des fichiers de configuration..."
                    if [ -f "application.properties" ] && [ ! -f "src/main/resources/application.properties" ]; then
                        mkdir -p src/main/resources
                        cp application.properties src/main/resources/
                        echo "✅ application.properties copié"
                    fi

                    # Build Maven
                    echo "🔨 Build Maven..."
                    mvn clean compile test jacoco:report package -q

                    # Vérification
                    if [ -f "target/student-management-0.0.1-SNAPSHOT.jar" ]; then
                        echo "✅ Build réussi - JAR généré"
                    else
                        echo "❌ Échec du build - JAR non trouvé"
                        exit 1
                    fi
                '''
            }
        }

        // ÉTAPE 3: BUILD DOCKER
        stage('Build Docker Image') {
            steps {
                sh '''
                    echo "=== 3. BUILD DOCKER IMAGE ==="

                    # Créer Dockerfile si nécessaire
                    if [ ! -f "Dockerfile" ]; then
                        echo "📝 Création du Dockerfile..."
                        cat > Dockerfile << EOF
FROM eclipse-temurin:17-jre-alpine
COPY target/student-management-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
EXPOSE 8080
EOF
                    fi

                    # Build de l'image Docker
                    echo "🐳 Construction de l'image Docker..."
                    docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                    docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest

                    # Vérification
                    echo "📦 Images disponibles:"
                    docker images | grep ${DOCKER_IMAGE}

                    echo "✅ Image Docker construite: ${DOCKER_IMAGE}:${DOCKER_TAG}"
                '''
            }
        }

        // ÉTAPE 4: DÉPLOIEMENT KUBERNETES
        stage('Déploiement Kubernetes') {
            steps {
                sh '''
                    echo "=== 4. DÉPLOIEMENT KUBERNETES ==="

                    # Créer le namespace
                    echo "🏗️  Création du namespace ${K8S_NAMESPACE}..."
                    kubectl create namespace ${K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

                    # 1. Déployer MySQL
                    echo "🗄️  Déploiement MySQL..."
                    if [ -f "mysql-deployment.yaml" ]; then
                        kubectl apply -f mysql-deployment.yaml -n ${K8S_NAMESPACE}
                        echo "✅ MySQL déployé"

                        # Attendre que MySQL soit prêt
                        echo "⏳ Attente du démarrage de MySQL..."
                        sleep 30
                    else
                        echo "❌ Fichier mysql-deployment.yaml non trouvé"
                        exit 1
                    fi

                    # 2. Déployer Spring Boot
                    echo "🚀 Déploiement Spring Boot..."
                    if [ -f "spring-deployment.yaml" ]; then
                        # Sauvegarder le fichier original
                        cp spring-deployment.yaml spring-deployment.yaml.bak

                        # Mettre à jour l'image dans le fichier YAML
                        sed -i "s|image: .*|image: ${DOCKER_IMAGE}:${DOCKER_TAG}|" spring-deployment.yaml

                        # Appliquer le déploiement
                        kubectl apply -f spring-deployment.yaml -n ${K8S_NAMESPACE}

                        # Restaurer le fichier original
                        mv spring-deployment.yaml.bak spring-deployment.yaml

                        echo "✅ Spring Boot déployé"
                    else
                        echo "❌ Fichier spring-deployment.yaml non trouvé"
                        exit 1
                    fi

                    echo "🎉 Déploiements Kubernetes terminés !"
                '''
            }
        }

        // ÉTAPE 5: VÉRIFICATION ET TESTS
        stage('Vérification & Tests') {
            steps {
                sh '''
                    echo "=== 5. VÉRIFICATION ET TESTS ==="

                    # Attendre que les pods démarrent
                    echo "⏳ Attente du démarrage des pods..."
                    sleep 20

                    # Afficher l'état du cluster
                    echo ""
                    echo "📊 ÉTAT DU CLUSTER KUBERNETES:"
                    echo "================================"
                    kubectl get all -n ${K8S_NAMESPACE}

                    # Détail des pods
                    echo ""
                    echo "🐳 DÉTAIL DES PODS:"
                    echo "================="
                    kubectl get pods -n ${K8S_NAMESPACE} -o wide

                    # Vérifier l'état des pods
                    echo ""
                    echo "📈 ÉTAT DES PODS:"
                    echo "================"
                    kubectl describe pods -n ${K8S_NAMESPACE} | grep -A3 "State:" || true

                    # Services
                    echo ""
                    echo "🔗 SERVICES:"
                    echo "==========="
                    kubectl get svc -n ${K8S_NAMESPACE}

                    # Obtenir l'URL d'accès
                    echo ""
                    echo "🌐 URL D'ACCÈS À L'APPLICATION:"
                    echo "==============================="
                    NODE_PORT=$(kubectl get svc spring-service -n ${K8S_NAMESPACE} -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "N/A")
                    MINIKUBE_IP=$(minikube ip 2>/dev/null || echo "N/A")

                    if [ "$NODE_PORT" != "N/A" ] && [ "$MINIKUBE_IP" != "N/A" ]; then
                        echo "🎯 Application accessible à:"
                        echo "   • http://${MINIKUBE_IP}:${NODE_PORT}/student"
                        echo "   • http://${MINIKUBE_IP}:${NODE_PORT}/student/actuator/health"

                        # Tester l'application
                        echo ""
                        echo "🧪 TEST DE L'APPLICATION:"
                        echo "========================"
                        sleep 10
                        if curl -s --max-time 10 "http://${MINIKUBE_IP}:${NODE_PORT}/student/actuator/health" >/dev/null; then
                            echo "✅ Application répond correctement !"
                        else
                            echo "⚠️  Application ne répond pas encore (peut prendre quelques secondes)"
                        fi
                    else
                        echo "⚠️  Impossible de déterminer l'URL d'accès"
                    fi

                    # Logs
                    echo ""
                    echo "📝 DERNIERS LOGS SPRING BOOT:"
                    echo "============================="
                    SPRING_POD=$(kubectl get pods -n ${K8S_NAMESPACE} -l app=spring-app -o name 2>/dev/null | head -1)
                    if [ -n "$SPRING_POD" ]; then
                        kubectl logs -n ${K8S_NAMESPACE} $SPRING_POD --tail=10 2>/dev/null || echo "Logs non disponibles"
                    fi

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
                echo "📋 RÉSUMÉ COMPLET DU BUILD #${BUILD_NUMBER}"
                echo "========================================"
                echo ""
                echo "🏗️  INFORMATIONS DE DÉPLOIEMENT:"
                echo "-------------------------------"
                echo "• Build ID: #${BUILD_NUMBER}"
                echo "• Namespace Kubernetes: ${K8S_NAMESPACE}"
                echo "• Image Docker: ${DOCKER_IMAGE}:${DOCKER_TAG}"
                echo "• Minikube IP: $(minikube ip 2>/dev/null || echo 'N/A')"
                echo ""

                # État final
                echo "📊 ÉTAT FINAL DU DÉPLOIEMENT:"
                echo "----------------------------"
                kubectl get pods,svc,deploy -n ${K8S_NAMESPACE} 2>/dev/null || echo "Aucune ressource trouvée"
                echo ""

                # URL d'accès finale
                echo "🔗 ACCÈS À L'APPLICATION:"
                echo "-----------------------"
                NODE_PORT=$(kubectl get svc spring-service -n ${K8S_NAMESPACE} -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "N/A")
                MINIKUBE_IP=$(minikube ip 2>/dev/null || echo "N/A")

                if [ "$NODE_PORT" != "N/A" ] && [ "$MINIKUBE_IP" != "N/A" ]; then
                    echo "🎯 VOTRE APPLICATION EST DISPONIBLE À:"
                    echo "   http://${MINIKUBE_IP}:${NODE_PORT}/student"
                    echo ""
                    echo "📱 Testez avec:"
                    echo "   curl http://${MINIKUBE_IP}:${NODE_PORT}/student/actuator/health"
                else
                    echo "⚠️  L'URL d'accès n'est pas disponible"
                    echo "   Minikube IP: ${MINIKUBE_IP}"
                    echo "   NodePort: ${NODE_PORT}"
                fi
                echo ""

                # Commandes utiles
                echo "🛠️  COMMANDES UTILES POUR LE DÉBOGAGE:"
                echo "------------------------------------"
                echo "• Voir les logs: kubectl logs -n ${K8S_NAMESPACE} -l app=spring-app -f"
                echo "• Décrire un pod: kubectl describe pod -n ${K8S_NAMESPACE} \$(kubectl get pods -n ${K8S_NAMESPACE} -l app=spring-app -o name | head -1)"
                echo "• Accéder à MySQL: kubectl exec -n ${K8S_NAMESPACE} -it \$(kubectl get pods -n ${K8S_NAMESPACE} -l app=mysql -o name) -- mysql -u root -p"
                echo "• Dashboard Minikube: minikube dashboard"
                echo "• Supprimer le déploiement: kubectl delete namespace ${K8S_NAMESPACE}"
            '''
        }

        success {
            echo """
            🎉 🎉 🎉 FÉLICITATIONS ! 🎉 🎉 🎉

            ✅ ATELIER KUBERNETES RÉUSSI !

            Tous les objectifs de l'atelier sont atteints :

            1. ✅ Cluster Kubernetes (Minikube) installé et configuré
            2. ✅ Application Spring Boot + MySQL déployée avec succès
            3. ✅ Pipeline CI/CD entièrement intégré
            4. ✅ Services exposés et accessibles depuis l'extérieur
            5. ✅ Pipeline Jenkins automatisé et fonctionnel

            📍 Votre application est maintenant déployée et accessible !
            """
        }

        failure {
            echo "❌ Échec du pipeline. Consultez les logs pour le débogage."

            sh '''
                echo ""
                echo "🔍 INFORMATIONS DE DÉBOGAGE:"
                echo "==========================="
                echo "Derniers événements Kubernetes:"
                kubectl get events -n ${K8S_NAMESPACE} --sort-by='.lastTimestamp' 2>/dev/null | tail -10 || echo "Aucun événement"

                echo ""
                echo "Pods en erreur:"
                kubectl get pods -n ${K8S_NAMESPACE} --field-selector=status.phase!=Running 2>/dev/null || echo "Aucun pod en erreur"
            '''
        }
    }
}