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
        stage('Préparation') {
            steps {
                git branch: 'master', url: 'https://github.com/Malekmouelh/jenkins.git'
                sh '''
                    minikube start --driver=docker --cpus=4 --memory=4096 --disk-size=20g --force
                    eval $(minikube docker-env)
                    kubectl create namespace ${K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
                '''
            }
        }

        stage('Build Application') {
            steps {
                sh 'mvn clean package -q'
            }
        }

        stage('Build Docker') {
            steps {
                sh """
                    docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                    docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest
                """
            }
        }

        stage('Déploiement MySQL') {
            steps {
                sh 'kubectl apply -f mysql-deployment.yaml -n ${K8S_NAMESPACE}'
            }
        }

        stage('Déploiement Spring Boot') {
            steps {
                sh """
                    sed -i.bak "s|image:.*|image: ${DOCKER_IMAGE}:${DOCKER_TAG}|" spring-deployment.yaml
                    kubectl apply -f spring-deployment.yaml -n ${K8S_NAMESPACE}
                    mv spring-deployment.yaml.bak spring-deployment.yaml
                """
            }
        }

        stage('Déploiement SonarQube') {
            steps {
                sh '''
                    kubectl apply -f sonarqube-deployment.yaml -n ${K8S_NAMESPACE} || true
                    kubectl apply -f sonarqube-service.yaml -n ${K8S_NAMESPACE} || true
                    sleep 300
                '''
            }
        }

        stage('Vérification') {
            steps {
                sh '''
                    kubectl get pods -n ${K8S_NAMESPACE}
                    kubectl get svc -n ${K8S_NAMESPACE}
                    NODE_PORT=$(kubectl get svc spring-service -n ${K8S_NAMESPACE} -o jsonpath='{.spec.ports[0].nodePort}')
                    MINIKUBE_IP=$(minikube ip)
                    echo "Application URL: http://${MINIKUBE_IP}:${NODE_PORT}/student"
                '''
            }
        }
    }

    post {
        always {
            sh 'kubectl get all -n ${K8S_NAMESPACE} || echo "Aucune ressource"'
        }
    }
}
