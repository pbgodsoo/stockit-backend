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
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        K8S_NAMESPACE = 'default'
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
                          --snapshot-mode=redo
                    """
                }
            }
        }

        stage('Deploy to k8s') {
            steps {
                container('gradle') {
                    sh """
                        set -euxo pipefail

                        if [ ! -x ./kubectl ]; then
                          curl -L -o ./kubectl "https://dl.k8s.io/release/\$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
                          chmod +x ./kubectl
                        fi

                        ACTIVE_COLOR=\$(./kubectl get svc stockit-be -n ${K8S_NAMESPACE} -o jsonpath='{.spec.selector.color}' 2>/dev/null || true)
                        if [ -z "\${ACTIVE_COLOR}" ] || [ "\${ACTIVE_COLOR}" = "null" ]; then
                          ACTIVE_COLOR=blue
                        fi

                        if [ "\${ACTIVE_COLOR}" = "blue" ]; then
                          TARGET_COLOR=green
                        elif [ "\${ACTIVE_COLOR}" = "green" ]; then
                          TARGET_COLOR=blue
                        else
                          echo "Invalid active color: \${ACTIVE_COLOR}"
                          exit 1
                        fi

                        echo "[BlueGreen] active=\${ACTIVE_COLOR}, target=\${TARGET_COLOR}"
                        echo "\${ACTIVE_COLOR}" > .active_color

                        ./kubectl set image deployment/stockit-be-\${TARGET_COLOR} \
                          stockit-be=${IMAGE_NAME}:${IMAGE_TAG} \
                          --namespace=${K8S_NAMESPACE}

                        ./kubectl rollout status deployment/stockit-be-\${TARGET_COLOR} \
                          --namespace=${K8S_NAMESPACE} \
                          --timeout=240s

                        ./kubectl patch svc stockit-be \
                          --namespace=${K8S_NAMESPACE} \
                          -p "{\\\"spec\\\":{\\\"selector\\\":{\\\"app\\\":\\\"stockit-be\\\",\\\"color\\\":\\\"\${TARGET_COLOR}\\\"}}}"
                    """
                }
            }
        }
    }

    post {
        failure {
            echo 'Pipeline 실패! 서비스 셀렉터 롤백 시도'
            container('gradle') {
                sh """
                    set +e
                    if [ -x ./kubectl ] && [ -f .active_color ]; then
                      ACTIVE_COLOR=\$(cat .active_color)
                      if [ -n "\${ACTIVE_COLOR}" ] && [ "\${ACTIVE_COLOR}" != "null" ]; then
                        ./kubectl patch svc stockit-be \
                          --namespace=${K8S_NAMESPACE} \
                          -p "{\\\"spec\\\":{\\\"selector\\\":{\\\"app\\\":\\\"stockit-be\\\",\\\"color\\\":\\\"\${ACTIVE_COLOR}\\\"}}}"
                      fi
                    fi
                """
            }
        }
        success {
            echo "Pushed: ${IMAGE_NAME}:${IMAGE_TAG}"
        }
    }
}
