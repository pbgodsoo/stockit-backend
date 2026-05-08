pipeline {
    agent {
        kubernetes {
            label "gradle-kaniko-${UUID.randomUUID().toString()}"
            defaultContainer 'gradle'
            yaml """
apiVersion: v1
kind: Pod
spec:
  restartPolicy: Never
  containers:
    - name: gradle
      image: gradle:8.9-jdk17
      command: ['cat']
      tty: true
      volumeMounts:
        - name: gradle-cache
          mountPath: /home/gradle/.gradle
        - name: workspace
          mountPath: /workspace
    - name: kaniko
      image: gcr.io/kaniko-project/executor:debug
      command: ['/busybox/sh','-c','sleep infinity']
      tty: true
      volumeMounts:
        - name: docker-config
          mountPath: /kaniko/.docker
        - name: workspace
          mountPath: /workspace
  volumes:
    - name: gradle-cache
      emptyDir: {}
    - name: workspace
      emptyDir: {}
    - name: docker-config
      secret:
        secretName: dockerhub-cred
        items:
          - key: .dockerconfigjson
            path: config.json
"""
        }
    }

    environment {
        IMAGE_NAME = 'sunyeoplee/stockit-backend'
        IMAGE_TAG  = "${env.BUILD_NUMBER}"
        K8S_NAMESPACE = 'default'
        ACTIVE_COLOR = ''
        TARGET_COLOR = ''
    }

    stages {
        stage('Checkout') {
            steps {
                container('gradle') {
                    checkout scm
                }
            }
        }

        stage('Gradle Build') {
            steps {
                container('gradle') {
                    sh '''
                        chmod +x ./gradlew || true
                        ./gradlew --no-daemon clean bootJar
                    '''
                }
            }
        }

        stage('Kaniko Build & Push') {
            steps {
                container('kaniko') {
                    sh """
                        /kaniko/executor \
                          --context=${WORKSPACE} \
                          --dockerfile=${WORKSPACE}/CICD/docker/Dockerfile \
                          --destination=${IMAGE_NAME}:${IMAGE_TAG} \
                          --destination=${IMAGE_NAME}:latest \
                          --single-snapshot \
                          --use-new-run \
                          --cache=true \
                          --snapshotMode=redo
                    """
                }
            }
        }

        stage('Deploy to k8s') {
            steps {
                container('gradle') {
                    script {
                        env.ACTIVE_COLOR = sh(
                                script: """
                                    set -e
                                    if [ ! -x ./kubectl ]; then
                                      curl -L -o ./kubectl "https://dl.k8s.io/release/\$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
                                      chmod +x ./kubectl
                                    fi
                                    ./kubectl get svc stockit-be -n ${K8S_NAMESPACE} -o jsonpath='{.spec.selector.color}'
                                """,
                                returnStdout: true
                        ).trim()
                        env.TARGET_COLOR = (env.ACTIVE_COLOR == 'blue') ? 'green' : 'blue'
                    }

                    sh """
                        set -eux
                        ./kubectl set image deployment/stockit-be-${TARGET_COLOR} \
                          stockit-be=${IMAGE_NAME}:${IMAGE_TAG} \
                          --namespace=${K8S_NAMESPACE}
                        ./kubectl rollout status deployment/stockit-be-${TARGET_COLOR} \
                          --namespace=${K8S_NAMESPACE} \
                          --timeout=180s
                        ./kubectl patch svc stockit-be \
                          --namespace=${K8S_NAMESPACE} \
                          -p '{"spec":{"selector":{"app":"stockit-be","color":"${TARGET_COLOR}"}}}'
                    """
                }
            }
        }
    }

    post {
        failure {
            echo 'Pipeline 실패! 서비스 셀렉터 롤백 시도'
            container('gradle') {
                sh '''
                    set +e
                    if [ -x ./kubectl ] && [ -n "${ACTIVE_COLOR}" ]; then
                      ./kubectl patch svc stockit-be \
                        --namespace=${K8S_NAMESPACE} \
                        -p "{\"spec\":{\"selector\":{\"app\":\"stockit-be\",\"color\":\"${ACTIVE_COLOR}\"}}}"
                    fi
                '''
            }
        }
        success {
            echo "Pushed: ${IMAGE_NAME}:${IMAGE_TAG}"
        }
    }
}
