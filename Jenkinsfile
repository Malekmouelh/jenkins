pipeline {
    agent any

    tools {
        maven 'M2_HOME'
        jdk 'JAVA_HOME'
    }

    environment {
        DOCKER_IMAGE = 'malekmouelhi7/student-management' // change si besoin
        DOCKER_TAG = "${env.BUILD_NUMBER}"
        K8S_NAMESPACE = 'devops'
        KUBECONFIG = '/var/lib/jenkins/.kube/config' // assure-toi que Jenkins a ce fichier
        DOCKER_REGISTRY_CREDENTIALS = 'docker-hub-credentials' // optional: credentials id in Jenkins
        DOCKER_PUSH = "false" // set "true" to push to registry
    }

    stages {
        stage('Préparation') {
          steps {
            checkout scm
            sh '''
              echo "Vérif outils"
              command -v kubectl || echo "kubectl missing"
              command -v minikube || echo "minikube missing"
            '''
          }
        }

        stage('Fix Encoding (quick)') {
          steps {
            sh '''
              # si le fichier existe, détecter son encodage et tenter une conversion vers UTF-8
              if [ -f src/main/resources/application.properties ]; then
                echo "Vérification encodage: src/main/resources/application.properties"
                file -bi src/main/resources/application.properties || true

                # essayer iconv (commence par iso-8859-1/common Windows CP1252)
                if command -v iconv >/dev/null 2>&1; then
                  iconv -f ISO-8859-1 -t UTF-8 src/main/resources/application.properties -o /tmp/app.props.utf8 || true
                  mv /tmp/app.props.utf8 src/main/resources/application.properties || true
                fi

                # enlever CR (dos2unix)
                if command -v dos2unix >/dev/null 2>&1; then
                  dos2unix src/main/resources/application.properties || true
                fi

                # forcer java encoding sur UTF-8 à la build (voir mvn invocation)
              else
                echo "Aucun application.properties dans src/main/resources"
              fi
            '''
          }
        }

        stage('Build Application') {
          steps {
            sh '''
              echo "=== Build Maven ==="
              # Force l'encodage à UTF-8 via property pour maven
              mvn -Dproject.build.sourceEncoding=UTF-8 -Dfile.encoding=UTF-8 -B clean package
            '''
          }
        }

        stage('Build Docker') {
            steps {
                script {
                    // Option: utiliser minikube docker-env si disponible
                    try {
                        sh '''
                            if minikube status >/dev/null 2>&1; then
                                echo "Utilisation du docker daemon de minikube pour builder (si possible)"
                                eval $(minikube docker-env)
                            fi
                            docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                            docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest
                        '''
                    } catch (err) {
                        error "Erreur build docker: ${err}"
                    }
                }
            }
        }

        stage('Push Docker (optionnel)') {
            when {
                expression { return env.DOCKER_PUSH == "true" }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: "${DOCKER_REGISTRY_CREDENTIALS}", usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PSW')]) {
                    sh '''
                        echo "${DOCKER_PSW}" | docker login -u "${DOCKER_USER}" --password-stdin
                        docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                        docker push ${DOCKER_IMAGE}:latest
                    '''
                }
            }
        }

        stage('Deploy K8s') {
            steps {
                script {
                    // s'assurer que kubeconfig est lisible par Jenkins
                    sh '''
                        export KUBECONFIG=${KUBECONFIG}
                        kubectl create namespace ${K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
                        # Appliquer MySQL et Sonar et Spring manifests
                        kubectl apply -f k8s/mysql-deployment.yaml
                        kubectl apply -f k8s/sonarqube-deployment.yaml
                        # Mettre à jour l'image dynamique dans le manifest spring
                        sed -i.bak "s|image: .*|image: ${DOCKER_IMAGE}:${DOCKER_TAG}|" k8s/spring-deployment.yaml || true
                        kubectl apply -f k8s/spring-deployment.yaml
                        mv k8s/spring-deployment.yaml.bak k8s/spring-deployment.yaml || true
                    '''
                }
            }
        }

        stage('Wait for Pods') {
            steps {
                sh '''
                    export KUBECONFIG=${KUBECONFIG}
                    echo "Attente des pods..."
                    kubectl -n ${K8S_NAMESPACE} wait --for=condition=ready pod -l app=mysql --timeout=120s || true
                    kubectl -n ${K8S_NAMESPACE} wait --for=condition=ready pod -l app=spring-app --timeout=180s || true
                    kubectl -n ${K8S_NAMESPACE} get pods
                '''
            }
        }

        stage('Run SonarQube Scan (as K8s Job)') {
            steps {
                sh '''
                    export KUBECONFIG=${KUBECONFIG}
                    # Crée et lance un job K8s qui clone et exécute mvn sonar:sonar à l'intérieur du cluster
                    kubectl apply -f k8s/sonar-scan-job.yaml -n ${K8S_NAMESPACE}
                    kubectl -n ${K8S_NAMESPACE} wait --for=condition=complete job/sonar-scan --timeout=600s || true
                    kubectl logs job/sonar-scan -n ${K8S_NAMESPACE} || true
                '''
            }
        }

        stage('Vérification et tests') {
            steps {
                sh '''
                    export KUBECONFIG=${KUBECONFIG}
                    echo "Pods:"
                    kubectl get pods -n ${K8S_NAMESPACE} -o wide
                    echo "Services:"
                    kubectl get svc -n ${K8S_NAMESPACE}
                    # Récupérer URL (minikube)
                    NODE_PORT=$(kubectl get svc spring-service -n ${K8S_NAMESPACE} -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "")
                    MINIKUBE_IP=$(minikube ip 2>/dev/null || echo "")
                    if [ -n "$NODE_PORT" ] && [ -n "$MINIKUBE_IP" ]; then
                        echo "Application disponible: http://${MINIKUBE_IP}:${NODE_PORT}/student"
                    fi
                '''
            }
        }
    }

    post {
        always {
            sh '''
                export KUBECONFIG=${KUBECONFIG}
                echo "Résumé build"
                kubectl get all -n ${K8S_NAMESPACE} || true
            '''
        }
        success {
            echo "🎉 Atelier terminé avec succès."
        }
        failure {
            echo "❌ Pipeline échoué — consulter les logs."
        }
    }
}