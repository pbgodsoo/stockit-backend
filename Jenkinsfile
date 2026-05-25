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

                        TARGET_REPLICAS=2
                        SOURCE_REPLICAS=0
                        POD_WAIT_TIMEOUT=300s
                        PROPAGATION_WAIT=20

                        log() {
                          echo "[\$(date '+%Y-%m-%d %H:%M:%S %z')] \$*"
                        }

                        log "[BlueGreen] active=\${ACTIVE_COLOR}, target=\${TARGET_COLOR}"
                        echo "\${ACTIVE_COLOR}" > .active_color

                        # Step 1: 새 이미지로 target 파드 배포 및 Ready 확인
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

                        log "[BlueGreen] waiting for target pods Ready"
                        if ! ./kubectl wait pod \
                          -l app=stockit-be,color=\${TARGET_COLOR} \
                          --namespace=${K8S_NAMESPACE} \
                          --for=condition=Ready \
                          --timeout=\${POD_WAIT_TIMEOUT}; then
                          log "[BlueGreen][ERROR] target pod readiness timeout"
                          ./kubectl get pods -l app=stockit-be,color=\${TARGET_COLOR} --namespace=${K8S_NAMESPACE} -o wide || true
                          exit 1
                        fi
                        log "[BlueGreen] target pods are Ready"
                        ./kubectl get pods -l app=stockit-be,color=\${TARGET_COLOR} --namespace=${K8S_NAMESPACE} -o wide

                        # Step 2: 셀렉터를 wide로 변경 (양쪽 색상 모두 트래픽 수신)
                        # JSON merge patch에서 기존 필드를 제거하려면 null을 명시해야 함.
                        # --type=merge 없이 color 필드를 생략하면 기존 color: green이 그대로 남아
                        # selector가 변경되지 않는다 (= "no change" 원인).
                        log "[BlueGreen] widening service selector to both colors (removing color filter)"
                        ./kubectl patch svc stockit-be \
                          --namespace=${K8S_NAMESPACE} \
                          --type=merge \
                          -p "{\\\"spec\\\":{\\\"selector\\\":{\\\"app\\\":\\\"stockit-be\\\",\\\"color\\\":null}}}"

                        log "[BlueGreen] waiting \${PROPAGATION_WAIT}s for Istio/kube-proxy to propagate"
                        sleep \${PROPAGATION_WAIT}

                        # Step 3: old 파드 종료 및 제거 확인
                        log "[BlueGreen] scale down old color: \${ACTIVE_COLOR} -> \${SOURCE_REPLICAS}"
                        ./kubectl scale deployment/stockit-be-\${ACTIVE_COLOR} \
                          --replicas=\${SOURCE_REPLICAS} \
                          --namespace=${K8S_NAMESPACE}

                        log "[BlueGreen] waiting for old pods to terminate"
                        ./kubectl wait pod \
                          -l app=stockit-be,color=\${ACTIVE_COLOR} \
                          --namespace=${K8S_NAMESPACE} \
                          --for=delete \
                          --timeout=\${POD_WAIT_TIMEOUT} || true
                        log "[BlueGreen] old color pods terminated"

                        # Step 4: old 파드 제거 후 라우팅 전파 대기
                        log "[BlueGreen] waiting \${PROPAGATION_WAIT}s for old pod route removal"
                        sleep \${PROPAGATION_WAIT}

                        # Step 5: 셀렉터를 target color로 좁히기
                        log "[BlueGreen] narrowing service selector to \${TARGET_COLOR}"
                        ./kubectl patch svc stockit-be \
                          --namespace=${K8S_NAMESPACE} \
                          -p "{\\\"spec\\\":{\\\"selector\\\":{\\\"app\\\":\\\"stockit-be\\\",\\\"color\\\":\\\"\${TARGET_COLOR}\\\"}}}"

                        log "[BlueGreen] deployment complete: \${ACTIVE_COLOR} -> \${TARGET_COLOR}"
                        ./kubectl get deploy stockit-be-blue stockit-be-green \
                          --namespace=${K8S_NAMESPACE} \
                          -o custom-columns=NAME:.metadata.name,READY:.status.readyReplicas,DESIRED:.spec.replicas,UPDATED:.status.updatedReplicas,AVAILABLE:.status.availableReplicas
                        ./kubectl get endpoints stockit-be --namespace=${K8S_NAMESPACE} -o wide
                        ./kubectl get svc stockit-be --namespace=${K8S_NAMESPACE} -o jsonpath='selector: {.spec.selector}{\"\\n\"}'
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

                        echo "[Rollback] scaling active color back up: \${ACTIVE_COLOR} -> 2"
                        ./kubectl scale deployment/stockit-be-\${ACTIVE_COLOR} \
                          --replicas=2 \
                          --namespace=${K8S_NAMESPACE}

                        ./kubectl rollout status deployment/stockit-be-\${ACTIVE_COLOR} \
                          --namespace=${K8S_NAMESPACE} \
                          --timeout=300s || true

                        ./kubectl wait pod \
                          -l app=stockit-be,color=\${ACTIVE_COLOR} \
                          --namespace=${K8S_NAMESPACE} \
                          --for=condition=Ready \
                          --timeout=120s || true

                        echo "[Rollback] patching service selector back to \${ACTIVE_COLOR}"
                        ./kubectl patch svc stockit-be \
                          --namespace=${K8S_NAMESPACE} \
                          -p "{\\\"spec\\\":{\\\"selector\\\":{\\\"app\\\":\\\"stockit-be\\\",\\\"color\\\":\\\"\${ACTIVE_COLOR}\\\"}}}"

                        if [ -n "\${TARGET_COLOR}" ]; then
                          echo "[Rollback] scaling failed target color down: \${TARGET_COLOR} -> 0"
                          ./kubectl scale deployment/stockit-be-\${TARGET_COLOR} \
                            --replicas=0 \
                            --namespace=${K8S_NAMESPACE}
                        fi

                        ./kubectl get deploy stockit-be-blue stockit-be-green \
                          --namespace=${K8S_NAMESPACE} \
                          -o custom-columns=NAME:.metadata.name,READY:.status.readyReplicas,DESIRED:.spec.replicas,UPDATED:.status.updatedReplicas,AVAILABLE:.status.availableReplicas || true
                        ./kubectl get svc stockit-be --namespace=${K8S_NAMESPACE} -o jsonpath='selector: {.spec.selector}{"\n"}' || true
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