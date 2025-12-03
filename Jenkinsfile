pipeline {
    agent any

    tools {
        maven 'M2_HOME'
    }

    environment {
        MAVEN_HOME = "${tool 'M2_HOME'}"
        PATH = "${env.MAVEN_HOME}/bin:${env.PATH}"
        DOCKER_IMAGE = "malekmouelhi7/student-management"
        DOCKER_TAG = "${env.BUILD_NUMBER}"
        SONAR_HOST_URL = "http://localhost:9000"
    }

    stages {
        stage('Compile') {
            steps {
                sh 'mvn compile'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Code Coverage') {
            steps {
                sh 'mvn jacoco:prepare-agent test jacoco:report'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    sh 'mvn sonar:sonar \
                        -Dsonar.projectKey=student-management \
                        -Dsonar.projectName=student-management \
                        -Dsonar.java.binaries=target/classes \
                        -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: false
                }
            }
        }

        stage('Package') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh """
                    docker build -t ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} .
                    docker tag ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} ${env.DOCKER_IMAGE}:latest
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
                        echo \$DOCKER_PASSWORD | docker login -u \$DOCKER_USERNAME --password-stdin
                        docker push ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
                        docker push ${env.DOCKER_IMAGE}:latest
                    """
                }
            }
        }
    }

    post {
        success {
            echo "Build ${env.BUILD_NUMBER} réussi!"
            echo "Image Docker: ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}"
        }
        failure {
            echo 'Build échoué!'
        }
    }
}
