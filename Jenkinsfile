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
                    sh """#!/bin/bash
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

                        TARGET_REPLICAS=4
                        SOURCE_REPLICAS=0
                        ENDPOINT_WAIT_TIMEOUT=60
                        SCALE_STEP_WAIT_SECONDS=15

                        log() {
                          echo "[\$(date '+%Y-%m-%d %H:%M:%S %z')] \$*"
                        }

                        get_ready_endpoint_count() {
                          ./kubectl get endpoints stockit-be \
                            --namespace=${K8S_NAMESPACE} \
                            -o jsonpath='{range .subsets[*].addresses[*]}{.ip}{"\n"}{end}' 2>/dev/null | \
                            grep -cE '^[0-9a-fA-F:.]+\$' || true
                        }

                        log "[BlueGreen] active=\${ACTIVE_COLOR}, target=\${TARGET_COLOR}"
                        echo "\${ACTIVE_COLOR}" > .active_color

                        log "[BlueGreen] set image to \${IMAGE_NAME}:\${IMAGE_TAG} on \${TARGET_COLOR}"
                        ./kubectl set image deployment/stockit-be-\${TARGET_COLOR} \
                          stockit-be=${IMAGE_NAME}:${IMAGE_TAG} \
                          --namespace=${K8S_NAMESPACE}

                        log "[BlueGreen] scale up stockit-be-\${TARGET_COLOR} to \${TARGET_REPLICAS}"
                        ./kubectl scale deployment/stockit-be-\${TARGET_COLOR} \
                          --replicas=\${TARGET_REPLICAS} \
                          --namespace=${K8S_NAMESPACE}

                        log "[BlueGreen] waiting rollout for stockit-be-\${TARGET_COLOR}"
                        ./kubectl rollout status deployment/stockit-be-\${TARGET_COLOR} \
                          --namespace=${K8S_NAMESPACE} \
                          --timeout=900s

                        log "[BlueGreen] patching service selector to \${TARGET_COLOR}"
                        ./kubectl patch svc stockit-be \
                          --namespace=${K8S_NAMESPACE} \
                          -p "{\\\"spec\\\":{\\\"selector\\\":{\\\"app\\\":\\\"stockit-be\\\",\\\"color\\\":\\\"\${TARGET_COLOR}\\\"}}}"

                        log "[BlueGreen] waiting for endpoint stabilization (timeout=\${ENDPOINT_WAIT_TIMEOUT}s)"
                        START_TS=\$(date +%s)
                        while true; do
                          READY_COUNT=\$(get_ready_endpoint_count)
                          NOW_TS=\$(date +%s)
                          ELAPSED=\$((NOW_TS - START_TS))
                          log "[BlueGreen] target ready endpoints=\${READY_COUNT}, elapsed=\${ELAPSED}s"

                          if [ "\${READY_COUNT}" -ge "\${TARGET_REPLICAS}" ]; then
                            log "[BlueGreen] endpoint stabilization complete"
                            break
                          fi

                          if [ "\${ELAPSED}" -ge "\${ENDPOINT_WAIT_TIMEOUT}" ]; then
                            log "[BlueGreen][ERROR] endpoint stabilization timeout"
                            ./kubectl get endpoints stockit-be --namespace=${K8S_NAMESPACE} -o wide || true
                            ./kubectl get deploy stockit-be-\${TARGET_COLOR} --namespace=${K8S_NAMESPACE} -o wide || true
                            ./kubectl get pods -l app=stockit-be,color=\${TARGET_COLOR} --namespace=${K8S_NAMESPACE} -o wide || true
                            exit 1
                          fi

                          sleep 2
                        done

                        log "[BlueGreen] scale-down old color in steps: \${ACTIVE_COLOR} 4 -> 2"
                        ./kubectl scale deployment/stockit-be-\${ACTIVE_COLOR} \
                          --replicas=2 \
                          --namespace=${K8S_NAMESPACE}
                        sleep \${SCALE_STEP_WAIT_SECONDS}

                        log "[BlueGreen] final scale-down old color: \${ACTIVE_COLOR} 2 -> \${SOURCE_REPLICAS}"
                        ./kubectl scale deployment/stockit-be-\${ACTIVE_COLOR} \
                          --replicas=\${SOURCE_REPLICAS} \
                          --namespace=${K8S_NAMESPACE}
                        log "[BlueGreen] old color scale-down complete"

                        ./kubectl get deploy stockit-be-blue stockit-be-green \
                          --namespace=${K8S_NAMESPACE} \
                          -o custom-columns=NAME:.metadata.name,READY:.status.readyReplicas,DESIRED:.spec.replicas,UPDATED:.status.updatedReplicas,AVAILABLE:.status.availableReplicas
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
                        if [ "\${ACTIVE_COLOR}" = "blue" ]; then
                          TARGET_COLOR=green
                        elif [ "\${ACTIVE_COLOR}" = "green" ]; then
                          TARGET_COLOR=blue
                        else
                          TARGET_COLOR=""
                        fi

                        ./kubectl scale deployment/stockit-be-\${ACTIVE_COLOR} \
                          --replicas=4 \
                          --namespace=${K8S_NAMESPACE}

                        if [ -n "\${TARGET_COLOR}" ]; then
                          ./kubectl scale deployment/stockit-be-\${TARGET_COLOR} \
                            --replicas=0 \
                            --namespace=${K8S_NAMESPACE}
                        fi

                        ./kubectl patch svc stockit-be \
                          --namespace=${K8S_NAMESPACE} \
                          -p "{\\\"spec\\\":{\\\"selector\\\":{\\\"app\\\":\\\"stockit-be\\\",\\\"color\\\":\\\"\${ACTIVE_COLOR}\\\"}}}"

                        ./kubectl get deploy stockit-be-blue stockit-be-green \
                          --namespace=${K8S_NAMESPACE} \
                          -o custom-columns=NAME:.metadata.name,READY:.status.readyReplicas,DESIRED:.spec.replicas,UPDATED:.status.updatedReplicas,AVAILABLE:.status.availableReplicas || true
                        ./kubectl get endpoints stockit-be --namespace=${K8S_NAMESPACE} -o wide || true
                        ./kubectl get pods -l app=stockit-be --namespace=${K8S_NAMESPACE} -o wide || true
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
