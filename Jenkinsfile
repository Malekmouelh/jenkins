pipeline {
    agent any
    
    tools {
        maven 'M2_HOME'
    }
    
    environment {
        DOCKER_IMAGE = 'malekmouelhi7/student-management'
        DOCKER_TAG = "${env.BUILD_NUMBER}"
        K8S_NAMESPACE = 'devops'
    }

    stages {
        stage('Checkout & Setup') {
            steps {
                git branch: 'master', url: 'https://github.com/Malekmouelh/jenkins.git'
                sh '''
                    echo "=== Configuration ==="
                    mvn --version
                    export DOCKER_HOST=unix:///var/run/docker.sock

                    # Corriger application.properties
                    echo "spring.application.name=student-management" > src/main/resources/application.properties
                    echo "server.port=8080" >> src/main/resources/application.properties
                    echo "server.servlet.context-path=/student" >> src/main/resources/application.properties
                    echo "" >> src/main/resources/application.properties
                    echo "# Configuration MySQL" >> src/main/resources/application.properties
                    echo "spring.datasource.url=jdbc:mysql://localhost:3306/studentdb?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" >> src/main/resources/application.properties
                    echo "spring.datasource.username=root" >> src/main/resources/application.properties
                    echo "spring.datasource.password=" >> src/main/resources/application.properties
                    echo "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver" >> src/main/resources/application.properties
                    echo "" >> src/main/resources/application.properties
                    echo "# Configuration JPA" >> src/main/resources/application.properties
                    echo "spring.jpa.hibernate.ddl-auto=update" >> src/main/resources/application.properties
                    echo "spring.jpa.show-sql=true" >> src/main/resources/application.properties
                    echo "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect" >> src/main/resources/application.properties

                    # Configuration de test H2
                    mkdir -p src/test/resources/
                    cat > src/test/resources/application-test.properties << "EOF"
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
server.port=0
EOF
                '''
            }
        }

        stage('Build & Test') {
            steps {
                sh '''
                    echo "=== Build & Test ==="

                    # Nettoyer
                    mvn clean

                    # Compiler
                    echo "Compilation..."
                    mvn compile -DskipTests -Dfile.encoding=UTF-8

                    # Exécuter les tests avec configuration simple
                    echo "Exécution des tests..."
                    mvn test -Dfile.encoding=UTF-8

                    # Vérifier le coverage
                    echo "Vérification du coverage..."
                    if [ -f "target/site/jacoco/jacoco.xml" ]; then
                        echo "✅ COVERAGE GÉNÉRÉ !"
                        echo "   Fichier: target/site/jacoco/jacoco.xml"
                    else
                        echo "⚠ Coverage non généré"
                        mvn jacoco:report
                    fi

                    # Package
                    mvn package -DskipTests
                '''
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    sh '''
                        echo "=== Analyse SonarQube ==="

                        if [ -f "target/site/jacoco/jacoco.xml" ]; then
                            echo "Lancement de SonarQube avec coverage..."
                            mvn sonar:sonar \
                                -Dsonar.projectKey=student-management \
                                -Dsonar.host.url=http://localhost:9000 \
                                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                        else
                            echo "Lancement de SonarQube sans coverage..."
                            mvn sonar:sonar \
                                -Dsonar.projectKey=student-management \
                                -Dsonar.host.url=http://localhost:9000
                        fi
                    '''
                }
            }
        }

        stage('Docker & Kubernetes') {
            steps {
                sh '''
                    echo "=== Docker & Kubernetes ==="

                    # Build Docker
                    if [ -f "target/student-management-0.0.1-SNAPSHOT.jar" ]; then
                        docker build -t malekmouelhi7/student-management:${BUILD_NUMBER} .
                        docker tag malekmouelhi7/student-management:${BUILD_NUMBER} malekmouelhi7/student-management:latest
                        echo "✅ Image Docker créée"
                    fi

                    # Kubernetes (optionnel)
                    kubectl create namespace devops --dry-run=client -o yaml | kubectl apply -f - 2>/dev/null || true
                '''
            }
        }
    }

    post {
        always {
            sh '''
                echo "=== RÉSUMÉ DU BUILD #${BUILD_NUMBER} ==="
                echo ""
                echo "📊 RÉSULTATS:"

                # Tests
                if [ -d "target/surefire-reports" ]; then
                    echo "✅ Tests exécutés"
                else
                    echo "❌ Tests non exécutés"
                fi

                # Coverage
                if [ -f "target/site/jacoco/jacoco.xml" ]; then
                    echo "✅ Coverage généré"
                    echo "   📈 Rapport: target/site/jacoco/index.html"
                else
                    echo "❌ Coverage non généré"
                fi

                # JAR
                if [ -f "target/student-management-0.0.1-SNAPSHOT.jar" ]; then
                    echo "✅ Application packagée"
                else
                    echo "❌ Application non packagée"
                fi

                # SonarQube
                echo "✅ Analyse SonarQube initiée"

                echo ""
                echo "🔗 SonarQube: http://localhost:9000"
            '''
        }

        success {
            echo "🎉 SUCCÈS ! Coverage généré et analyse SonarQube effectuée."
        }

        failure {
            echo "❌ Échec - Vérifiez les logs ci-dessus"
        }
    }
}