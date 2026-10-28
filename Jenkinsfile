pipeline {
    agent any

    tools {
        maven 'Maven-3.10'
    }

    environment {
        IMAGE_NAME = 'library-management-system'
        IMAGE_TAG = 'latest'
        K8S_NAMESPACE = 'library-management'
        // Fallback search path for Docker Desktop on Windows if not already in system PATH
        DOCKER_BIN_DIR = 'C:\\Users\\balad\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code from Git repository...'
                checkout scm
            }
        }

        stage('Verify Java and Maven') {
            steps {
                echo 'Verifying Java and Maven runtime versions...'
                bat 'java -version'
                bat 'mvn -version'
            }
        }

        stage('Compile') {
            steps {
                echo 'Compiling Spring Boot application sources...'
                bat 'mvn -B clean compile'
            }
        }

        stage('Run Automated Tests') {
            steps {
                echo 'Executing unit and integration tests with H2 isolated database...'
                bat 'mvn -B test'
            }
        }

        stage('Package Executable JAR') {
            steps {
                echo 'Packaging executable Spring Boot JAR archive...'
                bat 'mvn -B package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                echo "Building production Docker image: ${IMAGE_NAME}:${IMAGE_TAG}..."
                bat """
                    @echo off
                    setlocal EnableDelayedExpansion

                    where docker >nul 2>&1
                    if errorlevel 1 (
                        if exist "%DOCKER_BIN_DIR%\\docker.exe" (
                            set "PATH=!PATH!;%DOCKER_BIN_DIR%"
                        ) else (
                            echo [WARNING] docker.exe not found in standard PATH or %DOCKER_BIN_DIR%.
                            echo To enable Docker in Jenkins on Windows:
                            echo 1. Add DockerDesktop resources\\bin to the Windows System PATH.
                            echo 2. Restart the Jenkins Windows service.
                            exit /b 1
                        )
                    )

                    docker info >nul 2>&1
                    if errorlevel 1 (
                        echo [ERROR] Docker daemon is not accessible from Jenkins.
                        echo Ensure Docker Desktop is running and named pipe is accessible.
                        exit /b 1
                    )

                    echo Docker CLI is reachable. Proceeding with image build...
                    docker build -t %IMAGE_NAME%:%IMAGE_TAG% .
                """
            }
        }

        stage('Validate Kubernetes Manifests') {
            steps {
                echo 'Validating Kubernetes manifests in k8s/ directory via dry-run...'
                bat """
                    @echo off
                    setlocal EnableDelayedExpansion

                    where kubectl >nul 2>&1
                    if errorlevel 1 (
                        if exist "%DOCKER_BIN_DIR%\\kubectl.exe" (
                            set "PATH=!PATH!;%DOCKER_BIN_DIR%"
                        ) else (
                            echo [WARNING] kubectl.exe not found in PATH or %DOCKER_BIN_DIR%.
                            exit /b 1
                        )
                    )

                    echo Validating manifests using dry-run (client-side)...
                    kubectl apply --dry-run=client -f k8s/
                """
            }
        }

        stage('Deploy to Local Kubernetes') {
            steps {
                echo "Deploying Library Management System to local cluster (namespace: ${K8S_NAMESPACE})..."
                bat """
                    @echo off
                    setlocal EnableDelayedExpansion

                    where kubectl >nul 2>&1
                    if errorlevel 1 (
                        if exist "%DOCKER_BIN_DIR%\\kubectl.exe" (
                            set "PATH=!PATH!;%DOCKER_BIN_DIR%"
                        )
                    )

                    kubectl cluster-info >nul 2>&1
                    if errorlevel 1 (
                        echo [ERROR] Kubernetes cluster is not reachable from this environment.
                        echo Please ensure Docker Desktop Kubernetes is running.
                        exit /b 1
                    )

                    echo Applying Kubernetes manifests in sequence...
                    kubectl apply -f k8s/namespace.yaml
                    kubectl apply -f k8s/mysql-secret.yaml
                    kubectl apply -f k8s/configmap.yaml
                    kubectl apply -f k8s/mysql-pvc.yaml
                    kubectl apply -f k8s/mysql-service.yaml
                    kubectl apply -f k8s/mysql-deployment.yaml
                    kubectl apply -f k8s/app-service.yaml
                    kubectl apply -f k8s/app-deployment.yaml
                """
            }
        }

        stage('Verify Rollout Success') {
            steps {
                echo 'Verifying rollout status for MySQL and Spring Boot App...'
                bat """
                    @echo off
                    setlocal EnableDelayedExpansion

                    where kubectl >nul 2>&1
                    if errorlevel 1 (
                        if exist "%DOCKER_BIN_DIR%\\kubectl.exe" (
                            set "PATH=!PATH!;%DOCKER_BIN_DIR%"
                        )
                    )

                    echo Checking MySQL deployment rollout...
                    kubectl rollout status deployment/library-mysql -n %K8S_NAMESPACE% --timeout=180s

                    echo Checking Spring Boot App deployment rollout...
                    kubectl rollout status deployment/library-app -n %K8S_NAMESPACE% --timeout=180s

                    echo [SUCCESS] Deployments verified! Active pods and services:
                    kubectl get pods -n %K8S_NAMESPACE% -o wide
                    kubectl get svc -n %K8S_NAMESPACE%
                """
            }
        }
    }

    post {
        success {
            echo '================================================================='
            echo 'Automated Library Management System CI/CD Pipeline SUCCEEDED!'
            echo 'Application is live on Kubernetes NodePort: http://localhost:30088'
            echo '================================================================='
        }
        failure {
            echo '================================================================='
            echo 'Pipeline execution encountered errors. Check logs above.'
            echo '================================================================='
        }
    }
}
