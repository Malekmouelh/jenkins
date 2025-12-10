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
    KUBECONFIG = '/var/lib/jenkins/.kube/config'
    DOCKER_PUSH = "false" // mettre "true" si tu veux push vers un registry
    DOCKER_REGISTRY_CREDENTIALS = 'docker-hub-credentials' // optionnel
  }

  stages {
    stage('Préparation') {
      steps {
        checkout scm
        sh '''
          echo "Vérif outils"
          command -v kubectl || echo "kubectl missing"
          command -v minikube || echo "minikube missing"
          command -v docker || echo "docker missing"
        '''
      }
    }

    stage('Fix Encoding (quick)') {
      steps {
        sh '''
          if [ -f src/main/resources/application.properties ]; then
            file -bi src/main/resources/application.properties || true
            if command -v iconv >/dev/null 2>&1; then
              iconv -f ISO-8859-1 -t UTF-8 src/main/resources/application.properties -o /tmp/app.props.utf8 || true
              mv /tmp/app.props.utf8 src/main/resources/application.properties || true
            fi
            if command -v dos2unix >/dev/null 2>&1; then
              dos2unix src/main/resources/application.properties || true
            fi
          fi
        '''
      }
    }

    stage('Build Application') {
      steps {
        sh '''
          mvn -Dproject.build.sourceEncoding=UTF-8 -Dfile.encoding=UTF-8 -B clean package
        '''
      }
    }

    stage('Build Docker') {
      steps {
        script {
          sh '''
            set -e
            echo "=== Build Docker - check minikube ==="
            if minikube status >/dev/null 2>&1 && minikube status | grep -q "host: Running"; then
              echo "Minikube is running - using its docker daemon"
              eval "$(minikube docker-env)" || true
              docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
              docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest
            else
              echo "Minikube not running or not accessible from this agent."
              if docker info >/dev/null 2>&1; then
                echo "Using local Docker daemon"
                docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest
                if [ "${DOCKER_PUSH}" = "true" ]; then
                  echo "Pushing images to registry (DOCKER_PUSH=true)"
                  # login will be handled by Jenkins credentials when DOCKER_PUSH=true
                else
                  echo "Image built locally but not pushed. Set DOCKER_PUSH=true to push to registry."
                fi
              else
                echo "No Docker daemon available and minikube not running. Cannot build image."
                exit 2
              fi
            fi
          '''
        }
      }
    }

    stage('Push Docker (optionnel)') {
      when { expression { return env.DOCKER_PUSH == "true" } }
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
        sh '''
          if kubectl version --short >/dev/null 2>&1; then
            export KUBECONFIG=${KUBECONFIG}
            kubectl create namespace ${K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
            sed -i.bak "s|image:.*|image: ${DOCKER_IMAGE}:${DOCKER_TAG}|" k8s/spring-deployment.yaml || true
            kubectl apply -f k8s/mysql-deployment.yaml -n ${K8S_NAMESPACE} || true
            kubectl apply -f k8s/spring-deployment.yaml -n ${K8S_NAMESPACE} || true
            kubectl apply -f k8s/sonarqube-deployment.yaml -n ${K8S_NAMESPACE} || true
          else
            echo "kubectl cannot reach a Kubernetes API server from this agent; skipping k8s deploy."
          fi
        '''
      }
    }

    stage('Wait for Pods') {
      steps {
        sh '''
          if kubectl version --short >/dev/null 2>&1; then
            kubectl -n ${K8S_NAMESPACE} wait --for=condition=ready pod -l app=spring-app --timeout=180s || true
            kubectl -n ${K8S_NAMESPACE} get pods -o wide || true
          else
            echo "kubectl not available -> skip waiting pods"
          fi
        '''
      }
    }

    stage('Run SonarQube Scan (as K8s Job)') {
      steps {
        sh '''
          if kubectl version --short >/dev/null 2>&1; then
            kubectl apply -f k8s/sonar-scan-job.yaml -n ${K8S_NAMESPACE} || true
            kubectl -n ${K8S_NAMESPACE} wait --for=condition=complete job/sonar-scan --timeout=600s || true
            kubectl logs job/sonar-scan -n ${K8S_NAMESPACE} || true
          else
            echo "kubectl not available -> skip sonar scan on cluster"
          fi
        '''
      }
    }

    stage('Vérification et tests') {
      steps {
        sh '''
          if kubectl version --short >/dev/null 2>&1; then
            kubectl get all -n ${K8S_NAMESPACE} || true
            NODE_PORT=$(kubectl get svc spring-service -n ${K8S_NAMESPACE} -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "")
            MINIKUBE_IP=$(minikube ip 2>/dev/null || echo "")
            if [ -n "$NODE_PORT" ] && [ -n "$MINIKUBE_IP" ]; then
              echo "App URL: http://${MINIKUBE_IP}:${NODE_PORT}/student"
            fi
          else
            echo "kubectl not available -> skipping verification steps"
          fi
        '''
      }
    }
  }

  post {
    always {
      sh '''
        export KUBECONFIG=${KUBECONFIG}
        if kubectl version --short >/dev/null 2>&1; then
          kubectl get all -n ${K8S_NAMESPACE} || true
        else
          echo "Kubernetes inaccessible from this agent (kubectl failed)."
        fi
      '''
    }
    success { echo "🎉 Pipeline terminé avec succès." }
    failure { echo "❌ Pipeline échoué — voir logs." }
  }
}