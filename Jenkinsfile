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
        // Kubernetes configuration: uses Jenkins Global Environment Variable KUBECONFIG if set,
        // otherwise falls back to the local user's .kube/config path
        KUBECONFIG = "${env.KUBECONFIG ?: 'C:\\Users\\balad\\.kube\\config'}"
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

        stage('Diagnose Kubernetes Environment') {
            steps {
                echo 'Diagnosing Kubernetes context and cluster reachability...'
                bat """
                    @echo off
                    setlocal EnableDelayedExpansion

                    where kubectl >nul 2>&1
                    if errorlevel 1 (
                        if exist "%DOCKER_BIN_DIR%\\kubectl.exe" (
                            set "PATH=!PATH!;%DOCKER_BIN_DIR%"
                        ) else (
                            echo [ERROR] kubectl.exe not found in PATH or %DOCKER_BIN_DIR%.
                            exit /b 1
                        )
                    )

                    echo [STEP 1/4] Checking kubeconfig location...
                    if not defined KUBECONFIG set "KUBECONFIG=C:\\Users\\balad\\.kube\\config"
                    if not exist "%KUBECONFIG%" (
                        if exist "C:\\Users\\balad\\.kube\\config" set "KUBECONFIG=C:\\Users\\balad\\.kube\\config"
                    )

                    if not exist "%KUBECONFIG%" (
                        echo [ERROR] Kubeconfig not found at "%KUBECONFIG%".
                        echo Please configure KUBECONFIG in: Jenkins Dashboard / Manage Jenkins / System / Environment variables.
                        exit /b 1
                    )
                    echo [OK] Using kubeconfig: %KUBECONFIG%

                    echo [STEP 2/4] Verifying active Kubernetes context...
                    kubectl config current-context
                    if errorlevel 1 (
                        echo [ERROR] Failed to retrieve current Kubernetes context from %KUBECONFIG%.
                        exit /b 1
                    )

                    echo [STEP 3/4] Checking cluster control plane server endpoint...
                    kubectl config view --minify -o jsonpath="{.clusters[0].cluster.server}"
                    echo.

                    echo [STEP 4/4] Verifying cluster reachability and node status...
                    kubectl cluster-info
                    if errorlevel 1 (
                        echo [ERROR] Kubernetes cluster is unreachable or authentication failed.
                        echo Please ensure Docker Desktop Kubernetes is running.
                        exit /b 1
                    )

                    kubectl get nodes
                    if errorlevel 1 (
                        echo [ERROR] Failed to retrieve cluster nodes.
                        exit /b 1
                    )
                    echo [SUCCESS] Kubernetes environment diagnostic passed successfully.
                """
            }
        }

        stage('Validate Kubernetes Manifests') {
            steps {
                echo 'Validating Kubernetes manifests in k8s/ directory via OpenAPI schema dry-run...'
                bat """
                    @echo off
                    setlocal EnableDelayedExpansion

                    where kubectl >nul 2>&1
                    if errorlevel 1 (
                        if exist "%DOCKER_BIN_DIR%\\kubectl.exe" (
                            set "PATH=!PATH!;%DOCKER_BIN_DIR%"
                        )
                    )

                    if not defined KUBECONFIG set "KUBECONFIG=C:\\Users\\balad\\.kube\\config"

                    echo Validating all YAML manifests in k8s/ against cluster schema...
                    set "VALIDATION_FAILED=0"
                    for %%F in (k8s\\*.yaml) do (
                        echo Validating %%F...
                        kubectl apply --dry-run=client -f "%%F"
                        if errorlevel 1 (
                            echo [ERROR] Schema validation failed for %%F
                            set "VALIDATION_FAILED=1"
                        )
                    )

                    if !VALIDATION_FAILED! neq 0 (
                        echo [ERROR] One or more Kubernetes manifests failed validation.
                        exit /b 1
                    )
                    echo [SUCCESS] All Kubernetes manifests validated successfully.
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

                    if not defined KUBECONFIG set "KUBECONFIG=C:\\Users\\balad\\.kube\\config"

                    echo Applying Kubernetes manifests in sequence...
                    kubectl apply -f k8s/namespace.yaml
                    if errorlevel 1 ( echo [ERROR] Failed applying namespace.yaml & exit /b 1 )

                    kubectl apply -f k8s/mysql-secret.yaml
                    if errorlevel 1 ( echo [ERROR] Failed applying mysql-secret.yaml & exit /b 1 )

                    kubectl apply -f k8s/configmap.yaml
                    if errorlevel 1 ( echo [ERROR] Failed applying configmap.yaml & exit /b 1 )

                    kubectl apply -f k8s/mysql-pvc.yaml
                    if errorlevel 1 ( echo [ERROR] Failed applying mysql-pvc.yaml & exit /b 1 )

                    kubectl apply -f k8s/mysql-service.yaml
                    if errorlevel 1 ( echo [ERROR] Failed applying mysql-service.yaml & exit /b 1 )

                    kubectl apply -f k8s/mysql-deployment.yaml
                    if errorlevel 1 ( echo [ERROR] Failed applying mysql-deployment.yaml & exit /b 1 )

                    kubectl apply -f k8s/app-service.yaml
                    if errorlevel 1 ( echo [ERROR] Failed applying app-service.yaml & exit /b 1 )

                    kubectl apply -f k8s/app-deployment.yaml
                    if errorlevel 1 ( echo [ERROR] Failed applying app-deployment.yaml & exit /b 1 )

                    echo [SUCCESS] All manifests applied successfully.
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

                    if not defined KUBECONFIG set "KUBECONFIG=C:\\Users\\balad\\.kube\\config"

                    echo Checking MySQL deployment rollout...
                    kubectl rollout status deployment/library-mysql -n %K8S_NAMESPACE% --timeout=180s
                    if errorlevel 1 (
                        echo [ERROR] MySQL deployment rollout failed or timed out.
                        echo Displaying pod diagnostics:
                        kubectl describe pods -n %K8S_NAMESPACE% -l app=library-mysql
                        kubectl logs -n %K8S_NAMESPACE% -l app=library-mysql --tail=50
                        exit /b 1
                    )

                    echo Checking Spring Boot App deployment rollout...
                    kubectl rollout status deployment/library-app -n %K8S_NAMESPACE% --timeout=180s
                    if errorlevel 1 (
                        echo [ERROR] Spring Boot application rollout failed or timed out.
                        echo Displaying pod diagnostics:
                        kubectl describe pods -n %K8S_NAMESPACE% -l app=library-app
                        kubectl logs -n %K8S_NAMESPACE% -l app=library-app --tail=50
                        exit /b 1
                    )

                    echo [SUCCESS] Deployments verified! Active pods and services:
                    kubectl get pods -n %K8S_NAMESPACE% -o wide
                    kubectl get svc -n %K8S_NAMESPACE%

                    echo Verifying application health endpoint...
                    set "HEALTH_OK=0"
                    for /L %%i in (1,1,6) do (
                        if !HEALTH_OK! equ 0 (
                            curl.exe -s -f http://127.0.0.1:8082/actuator/health >nul 2>&1
                            if not errorlevel 1 (
                                set "HEALTH_OK=1"
                            ) else (
                                echo Attempt %%i/6: Waiting for health endpoint to respond...
                                timeout /t 5 /nobreak >nul 2>&1
                            )
                        )
                    )

                    if !HEALTH_OK! equ 1 (
                        echo [SUCCESS] Application health endpoint responded UP:
                        curl.exe -s http://127.0.0.1:8082/actuator/health
                        echo.
                    ) else (
                        echo [WARNING] Host port 8082 did not respond. Verifying active running pods...
                        kubectl get pods -n %K8S_NAMESPACE% -l app=library-app --no-headers | findstr /i "Running" >nul
                        if errorlevel 1 (
                            echo [ERROR] No Running pods found for library-app.
                            exit /b 1
                        )
                        echo [SUCCESS] library-app pod is confirmed Running in cluster.
                    )
                """
            }
        }
    }

    post {
        success {
            echo '================================================================='
            echo 'Automated Library Management System CI/CD Pipeline SUCCEEDED!'
            echo 'Application is live on Kubernetes LoadBalancer: http://localhost:8082'
            echo '================================================================='
        }
        failure {
            echo '================================================================='
            echo 'Pipeline execution encountered errors. Check logs above.'
            echo '================================================================='
        }
    }
}
