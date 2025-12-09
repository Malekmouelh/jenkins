pipeline {
    agent any
    
    tools {
        maven 'M2_HOME'
    }
    
    environment {
        DOCKER_IMAGE = 'malekmouelhi7/student-management'
        DOCKER_TAG = "${env.BUILD_NUMBER}"
        K8S_NAMESPACE = 'devops'
        SONAR_HOST = 'http://localhost:9000'
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'master',
                    url: 'https://github.com/Malekmouelh/jenkins.git'
            }
        }

        stage('Setup Environment') {
            steps {
                script {
                    sh """
                        echo "=== Configuration de l'environnement ==="

                        # Vérifier Maven
                        mvn --version || echo "⚠ Maven non disponible"

                        # Configurer Docker
                        export DOCKER_HOST=unix:///var/run/docker.sock

                        # Vérifier les fichiers de test
                        echo "Vérification des fichiers de configuration..."
                        ls -la src/test/resources/ || echo "⚠ Pas de répertoire test/resources"
                    """
                }
            }
        }

        stage('Build & Test') {
            steps {
                script {
                    sh """
                        echo "=== Build & Test ==="

                        # Nettoyer d'abord
                        mvn clean

                        # Compiler sans tests
                        mvn compile -DskipTests

                        # Exécuter les tests avec JaCoCo
                        echo "Exécution des tests..."
                        mvn test -Dspring.profiles.active=test

                        # Vérifier les résultats
                        echo "Vérification des résultats des tests..."
                        if [ -f "target/surefire-reports/TEST-all.xml" ] || [ -f "target/surefire-reports/*.xml" ]; then
                            echo "✅ Tests exécutés"
                        else
                            echo "⚠ Aucun rapport de test trouvé"
                        fi

                        # Vérifier JaCoCo
                        echo "Vérification JaCoCo..."
                        if [ -f "target/site/jacoco/jacoco.xml" ]; then
                            echo "✅ Rapport JaCoCo généré"
                            echo "📊 Fichier: target/site/jacoco/jacoco.xml"
                        else
                            echo "❌ Rapport JaCoCo NON généré"
                            echo "Recherche des fichiers .exec..."
                            find target -name "*.exec" 2>/dev/null || echo "Aucun fichier .exec"
                        fi

                        # Package final
                        mvn package -DskipTests
                    """
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    script {
                        sh """
                            echo "=== Analyse SonarQube ==="

                            # Vérifier si le rapport JaCoCo existe
                            if [ -f "target/site/jacoco/jacoco.xml" ]; then
                                echo "✅ Rapport JaCoCo disponible, lancement de SonarQube..."
                                mvn sonar:sonar \
                                    -Dsonar.projectKey=student-management \
                                    -Dsonar.host.url=${SONAR_HOST} \
                                    -Dsonar.login=admin \
                                    -Dsonar.password=admin \
                                    -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                            else
                                echo "⚠ Rapport JaCoCo manquant, génération..."
                                # Regénérer le rapport
                                mvn jacoco:report

                                # Essayer quand même SonarQube
                                mvn sonar:sonar \
                                    -Dsonar.projectKey=student-management \
                                    -Dsonar.host.url=${SONAR_HOST} \
                                    -Dsonar.login=admin \
                                    -Dsonar.password=admin
                            fi
                        """
                    }
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh """
                    echo "=== Construction de l'image Docker ==="
                    export DOCKER_HOST=unix:///var/run/docker.sock

                    # Vérifier que le JAR existe
                    if [ -f "target/student-management-0.0.1-SNAPSHOT.jar" ]; then
                        echo "✅ JAR trouvé, construction de l'image..."
                        docker build -t ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} .
                        docker tag ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} ${env.DOCKER_IMAGE}:latest
                    else
                        echo "❌ JAR non trouvé!"
                        ls -la target/*.jar || echo "Aucun JAR dans target/"
                    fi
                """
            }
        }

        stage('Push Docker Image') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USERNAME',
                    passwordVariable: 'DOCKER_PASSWORD'
                )]) {
                    sh """
                        echo "=== Push vers Docker Hub ==="
                        export DOCKER_HOST=unix:///var/run/docker.sock
                        echo \$DOCKER_PASSWORD | docker login -u \$DOCKER_USERNAME --password-stdin

                        # Vérifier l'image
                        docker images | grep ${env.DOCKER_IMAGE} || echo "⚠ Image non trouvée localement"

                        docker push ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} || echo "⚠ Push tag échoué"
                        docker push ${env.DOCKER_IMAGE}:latest || echo "⚠ Push latest échoué"
                    """
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    sh """
                        echo "=== Déploiement sur Kubernetes ==="

                        # Configuration temporaire
                        export KUBECONFIG=/root/.kube/config

                        echo "1. Vérification du cluster..."
                        kubectl cluster-info || echo "⚠ Impossible de se connecter au cluster"

                        echo "2. Création du namespace si nécessaire..."
                        kubectl create namespace ${env.K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f - 2>/dev/null || echo "Namespace déjà existant ou erreur"

                        echo "3. Déploiement des ressources..."
                        for file in *.yaml; do
                            if [ -f "\$file" ]; then
                                echo "   - Tentative avec \$file"
                                # Ajouter le namespace au déploiement
                                sed "s/namespace:.*/namespace: ${env.K8S_NAMESPACE}/g" "\$file" | kubectl apply -f - 2>/dev/null || \
                                kubectl apply -f "\$file" -n ${env.K8S_NAMESPACE} 2>/dev/null || echo "     ⚠ Échec avec \$file"
                            fi
                        done

                        echo "4. Attente..."
                        sleep 10

                        echo "5. État des pods:"
                        kubectl get pods -n ${env.K8S_NAMESPACE} 2>/dev/null || echo "   ⚠ Impossible d'obtenir les pods"

                        echo "6. État des services:"
                        kubectl get svc -n ${env.K8S_NAMESPACE} 2>/dev/null || echo "   ⚠ Impossible d'obtenir les services"
                    """
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                script {
                    sh """
                        echo "=== VÉRIFICATION FINALE ==="
                        echo ""
                        echo "📊 Résumé du build #${env.BUILD_NUMBER}:"
                        echo ""

                        # Vérifier les tests
                        if [ -f "target/surefire-reports"/*.xml 2>/dev/null ]; then
                            echo "✅ Tests exécutés"
                        else
                            echo "⚠ Tests non vérifiés"
                        fi

                        # Vérifier JaCoCo
                        if [ -f "target/site/jacoco/jacoco.xml" ]; then
                            echo "✅ Coverage généré"
                        else
                            echo "❌ Coverage NON généré"
                        fi

                        # Vérifier le JAR
                        if [ -f "target/student-management-0.0.1-SNAPSHOT.jar" ]; then
                            echo "✅ Application packagée"
                        else
                            echo "❌ Application NON packagée"
                        fi

                        echo ""
                        echo "🔗 URLs d'accès :"
                        echo "   - SonarQube: http://localhost:9000"
                        echo "   - Application: http://localhost:30080 (si déployée)"
                        echo ""
                        echo "📦 Image Docker: ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}"
                        echo ""

                        # Vérification finale
                        echo "=== DIAGNOSTIC COVERAGE ==="
                        if [ -f "target/site/jacoco/jacoco.xml" ]; then
                            echo "✅ SUCCÈS: Coverage disponible pour SonarQube"
                            echo "   Emplacement: target/site/jacoco/jacoco.xml"
                        else
                            echo "❌ ÉCHEC: Coverage non généré"
                            echo "   Causes possibles:"
                            echo "   1. Tests non exécutés"
                            echo "   2. Problème de configuration H2"
                            echo "   3. JaCoCo non configuré correctement"
                        fi
                    """
                }
            }
        }
    }

    post {
        always {
            script {
                echo "=== RÉSUMÉ DU BUILD #${env.BUILD_NUMBER} ==="
                echo "État: ${currentBuild.currentResult}"

                // Sauvegarder les logs de test
                sh '''
                    echo "📋 Logs disponibles:"
                    echo "   - Tests: target/surefire-reports/"
                    echo "   - Coverage: target/site/jacoco/"
                    echo "   - Build: target/student-management-*.jar"

                    # Nettoyage léger
                    docker system prune -f 2>/dev/null || true
                '''
            }
        }

        success {
            script {
                echo "🎉 Build réussi!"
                echo "📊 Prochaines étapes:"
                echo "   1. Vérifier SonarQube: ${SONAR_HOST}"
                echo "   2. Vérifier le coverage dans le rapport"
                echo "   3. Tester l'application déployée"
            }
        }

        failure {
            script {
                echo '❌ Build échoué'

                sh '''
                    echo "=== DÉBOGAGE DÉTAILLÉ ==="

                    echo "1. Structure du projet:"
                    find . -name "*.java" -type f | head -20
                    echo ""

                    echo "2. Fichiers de configuration:"
                    ls -la src/main/resources/ 2>/dev/null || echo "   ⚠ Pas de main/resources"
                    ls -la src/test/resources/ 2>/dev/null || echo "   ⚠ Pas de test/resources"
                    echo ""

                    echo "3. Résultats Maven:"
                    ls -la target/ 2>/dev/null || echo "   ⚠ Pas de répertoire target"
                    echo ""

                    echo "4. Fichiers de test:"
                    find . -name "*Test*.java" -type f
                    echo ""

                    echo "5. Logs récents:"
                    tail -50 /var/log/jenkins/jenkins.log 2>/dev/null || echo "   ⚠ Logs Jenkins non accessibles"
                '''
            }
        }
    }
}