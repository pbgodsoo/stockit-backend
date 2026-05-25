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
                        ENDPOINT_WAIT_TIMEOUT=300

                        log() {
                          echo "[\$(date '+%Y-%m-%d %H:%M:%S %z')] \$*"
                        }

                        wait_for_pod_ips_in_service() {
                          local color="\$1"
                          local expected="\$2"
                          local timeout="\$3"
                          local deadline=\$((SECONDS + timeout))
                          local pod_ips=""
                          local endpoint_ips=""
                          local matched_count=0
                          local pod_ip=""

                          while [ "\${SECONDS}" -lt "\${deadline}" ]; do
                            pod_ips=\$(./kubectl get pods \
                              -l app=stockit-be,color=\${color} \
                              --namespace=${K8S_NAMESPACE} \
                              --field-selector=status.phase=Running \
                              -o jsonpath='{range .items[*]}{.status.podIP}{"\\n"}{end}' 2>/dev/null || true)
                            endpoint_ips=\$(./kubectl get endpoints stockit-be \
                              --namespace=${K8S_NAMESPACE} \
                              -o jsonpath='{range .subsets[*].addresses[*]}{.ip}{"\\n"}{end}' 2>/dev/null || true)
                            matched_count=0

                            for pod_ip in \${pod_ips}; do
                              if printf '%s\\n' "\${endpoint_ips}" | grep -Fxq "\${pod_ip}"; then
                                matched_count=\$((matched_count + 1))
                              fi
                            done

                            if [ "\${matched_count}" -ge "\${expected}" ]; then
                              log "[BlueGreen] service endpoints include \${color} pod IPs (\${matched_count}/\${expected})"
                              return 0
                            fi

                            sleep 2
                          done

                          log "[BlueGreen][ERROR] service endpoints did not include enough \${color} pod IPs"
                          ./kubectl get svc stockit-be --namespace=${K8S_NAMESPACE} -o wide || true
                          ./kubectl get endpoints stockit-be --namespace=${K8S_NAMESPACE} -o wide || true
                          ./kubectl get pods -l app=stockit-be --namespace=${K8S_NAMESPACE} -o wide || true
                          ./kubectl get pods -l app=stockit-be,color=\${color} --namespace=${K8S_NAMESPACE} -o wide || true
                          return 1
                        }

                        log "[BlueGreen] active=\${ACTIVE_COLOR}, target=\${TARGET_COLOR}"
                        echo "\${ACTIVE_COLOR}" > .active_color

                        log "[BlueGreen] set image to \${IMAGE_NAME}:\${IMAGE_TAG} on \${TARGET_COLOR}"
                        ./kubectl set image deployment/stockit-be-\${TARGET_COLOR} \
                          stockit-be=${IMAGE_NAME}:${IMAGE_TAG} \
                          --namespace=${K8S_NAMESPACE}

                        log "[BlueGreen] scale up stockit-be-\${TARGET_COLOR} to final replicas=\${TARGET_REPLICAS}"
                        ./kubectl scale deployment/stockit-be-\${TARGET_COLOR} \
                          --replicas=\${TARGET_REPLICAS} \
                          --namespace=${K8S_NAMESPACE}

                        log "[BlueGreen] waiting rollout for stockit-be-\${TARGET_COLOR}"
                        ./kubectl rollout status deployment/stockit-be-\${TARGET_COLOR} \
                          --namespace=${K8S_NAMESPACE} \
                          --timeout=900s

                        log "[BlueGreen] waiting for target pods Ready (timeout=\${ENDPOINT_WAIT_TIMEOUT}s)"
                        if ! ./kubectl wait pod \
                          -l app=stockit-be,color=\${TARGET_COLOR} \
                          --namespace=${K8S_NAMESPACE} \
                          --for=condition=Ready \
                          --timeout=\${ENDPOINT_WAIT_TIMEOUT}s; then
                          log "[BlueGreen][ERROR] target pod readiness timeout"
                          ./kubectl get endpoints stockit-be --namespace=${K8S_NAMESPACE} -o wide || true
                          ./kubectl get deploy stockit-be-\${TARGET_COLOR} --namespace=${K8S_NAMESPACE} -o wide || true
                          ./kubectl get pods -l app=stockit-be,color=\${TARGET_COLOR} --namespace=${K8S_NAMESPACE} -o wide || true
                          exit 1
                        fi
                        log "[BlueGreen] target pods are Ready"
                        ./kubectl get pods -l app=stockit-be,color=\${TARGET_COLOR} --namespace=${K8S_NAMESPACE} -o wide

                        log "[BlueGreen] widening service selector to both blue and green before traffic switch"
                        ./kubectl patch svc stockit-be \
                          --namespace=${K8S_NAMESPACE} \
                          -p "{\\\"spec\\\":{\\\"selector\\\":{\\\"app\\\":\\\"stockit-be\\\"}}}"

                        wait_for_pod_ips_in_service "\${TARGET_COLOR}" "\${TARGET_REPLICAS}" "\${ENDPOINT_WAIT_TIMEOUT}"

                        log "[BlueGreen] scale down old color after target endpoints joined service: \${ACTIVE_COLOR} -> \${SOURCE_REPLICAS}"
                        ./kubectl scale deployment/stockit-be-\${ACTIVE_COLOR} \
                          --replicas=\${SOURCE_REPLICAS} \
                          --namespace=${K8S_NAMESPACE}

                        wait_for_pod_ips_in_service "\${TARGET_COLOR}" "\${TARGET_REPLICAS}" "\${ENDPOINT_WAIT_TIMEOUT}"
                        log "[BlueGreen] old color scale-down complete"

                        log "[BlueGreen] narrowing service selector to \${TARGET_COLOR}"
                        ./kubectl patch svc stockit-be \
                          --namespace=${K8S_NAMESPACE} \
                          -p "{\\\"spec\\\":{\\\"selector\\\":{\\\"app\\\":\\\"stockit-be\\\",\\\"color\\\":\\\"\${TARGET_COLOR}\\\"}}}"

                        wait_for_pod_ips_in_service "\${TARGET_COLOR}" "\${TARGET_REPLICAS}" "\${ENDPOINT_WAIT_TIMEOUT}"

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

                        echo "[BlueGreen][Rollback] scaling active color back up before selector rollback: \${ACTIVE_COLOR}"
                        ./kubectl scale deployment/stockit-be-\${ACTIVE_COLOR} \
                          --replicas=2 \
                          --namespace=${K8S_NAMESPACE}

                        ./kubectl rollout status deployment/stockit-be-\${ACTIVE_COLOR} \
                          --namespace=${K8S_NAMESPACE} \
                          --timeout=900s || true

                        ./kubectl wait pod \
                          -l app=stockit-be,color=\${ACTIVE_COLOR} \
                          --namespace=${K8S_NAMESPACE} \
                          --for=condition=Ready \
                          --timeout=300s || true

                        echo "[BlueGreen][Rollback] patching service selector back to \${ACTIVE_COLOR}"
                        ./kubectl patch svc stockit-be \
                          --namespace=${K8S_NAMESPACE} \
                          -p "{\\\"spec\\\":{\\\"selector\\\":{\\\"app\\\":\\\"stockit-be\\\",\\\"color\\\":\\\"\${ACTIVE_COLOR}\\\"}}}"

                        if [ -n "\${TARGET_COLOR}" ]; then
                          echo "[BlueGreen][Rollback] scaling failed target color down: \${TARGET_COLOR}"
                          ./kubectl scale deployment/stockit-be-\${TARGET_COLOR} \
                            --replicas=0 \
                            --namespace=${K8S_NAMESPACE}
                        fi

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
