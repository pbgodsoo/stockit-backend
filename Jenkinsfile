pipeline {
    agent any

    environment {
        DOCKER_HUB_CREDENTIALS = credentials('dockerhub-credentials')
        DOCKER_IMAGE = "sunyeoplee/stockit-backend"
        IMAGE_TAG = "${BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Docker Build & Push') {
            steps {
                sh """
                    docker build -t ${DOCKER_IMAGE}:${IMAGE_TAG} -f CICD/docker/Dockerfile .
                    docker build -t ${DOCKER_IMAGE}:latest -f CICD/docker/Dockerfile .
                    echo ${DOCKER_HUB_CREDENTIALS_PSW} | docker login -u ${DOCKER_HUB_CREDENTIALS_USR} --password-stdin
                    docker push ${DOCKER_IMAGE}:${IMAGE_TAG}
                    docker push ${DOCKER_IMAGE}:latest
                    docker logout
                """
            }
        }

        stage('Deploy to k8s') {
            steps {
                sh """
                    kubectl set image deployment/stockit-backend \
                    stockit-backend=${DOCKER_IMAGE}:${IMAGE_TAG} \
                    --namespace=stockit
                """
            }
        }
    }

    post {
        failure {
            echo 'Pipeline 실패!'
        }
        success {
            echo 'Pipeline 성공!'
        }
    }
}