<div align="center">

# 📦 STOCKIT Backend

### 매장 · 본사 · 물류창고의 재고/판매/발주/입출고/순환재고를 통합 관리하는 ERP 서비스

</div>

<br>

> ### 🔗 Project Links
> 🌐 **Web Service** : [STOCKIT 공식 서비스 접속하기](https://www.stockit.kro.kr)<br/>
> 🗂️ **Backend Repository** : [be24-fin-Stockers-Stockit-BE](https://github.com/beyond-sw-camp/be24-fin-Stockers-Stockit-BE)<br/>
> 🖥️ **Frontend Repository** : [be24-fin-Stockers-Stockit-FE](https://github.com/beyond-sw-camp/be24-fin-Stockers-Stockit-FE)<br/>
> 📘 **API Docs** : [사용자 인증 API.pdf](https://github.com/user-attachments/files/28377814/API.pdf) / [매장 API.pdf](https://github.com/user-attachments/files/28377803/API.pdf) / [본사 API.pdf](https://github.com/user-attachments/files/28377809/API.pdf)  / [창고 API.pdf](https://github.com/user-attachments/files/28377815/API.pdf)


<br>

---

## 👥 Team STOCKIT

<table align="center" width="100%">
  <tr>
    <td align="center" width="25%">
      <a href="https://github.com/saralove20">
        <img src="https://github.com/saralove20.png" width="90" style="border-radius: 50%;"><br/>
        <strong>김사라</strong>
      </a>
    </td>
    <td align="center" width="25%">
      <a href="https://github.com/pbgodsoo">
        <img src="https://github.com/pbgodsoo.png" width="90" style="border-radius: 50%;"><br/>
        <strong>박범수</strong>
      </a>
    </td>
    <td align="center" width="25%">
      <a href="https://github.com/sunyeoplee0">
        <img src="https://github.com/sunyeoplee0.png" width="90" style="border-radius: 50%;"><br/>
        <strong>이선엽</strong>
      </a>
    </td>
    <td align="center" width="25%">
      <a href="https://github.com/sarapoba">
        <img src="https://github.com/sarapoba.png" width="90" style="border-radius: 50%;"><br/>
        <strong>이후경</strong>
      </a>
    </td>
  </tr>
</table>

---

## 🛠 Tech Stack

### 🔹 Backend
![Java](https://img.shields.io/badge/Java_17-007396?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/SpringBoot_3.5.11-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/SpringSecurity-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)
![JPA](https://img.shields.io/badge/JPA_Hibernate-59666C?style=flat-square)
![JWT](https://img.shields.io/badge/JWT_Auth-000000?style=flat-square&logo=jsonwebtokens&logoColor=white)
![Spring Batch](https://img.shields.io/badge/SpringBatch-6DB33F?style=flat-square&logo=spring&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring_AI-6DB33F?style=flat-square&logo=spring&logoColor=white)
![Swagger](https://img.shields.io/badge/Springdoc_OpenAPI-85EA2D?style=flat-square&logo=swagger&logoColor=black)

### 🔹 DBMS · Search · Monitoring
![MariaDB](https://img.shields.io/badge/MariaDB-003545?style=flat-square&logo=mariadb&logoColor=white)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch_8.13.4-005571?style=flat-square&logo=elasticsearch&logoColor=white)
![Actuator](https://img.shields.io/badge/Spring_Actuator-6DB33F?style=flat-square&logo=spring&logoColor=white)
![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=flat-square&logo=prometheus&logoColor=white)

### 🔹 Infra · Deployment
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)
![Jenkins](https://img.shields.io/badge/Jenkins-D24939?style=flat-square&logo=jenkins&logoColor=white)
![Kubernetes](https://img.shields.io/badge/Kubernetes-326CE5?style=flat-square&logo=kubernetes&logoColor=white)
![Kaniko](https://img.shields.io/badge/Kaniko-FFA000?style=flat-square)
![AWS](https://img.shields.io/badge/AWS_RDS-232F3E?style=flat-square&logo=amazonaws&logoColor=white)
![Blue Green](https://img.shields.io/badge/Blue--Green_Deployment-0099CC?style=flat-square)

### 🔹 Collaboration
![Git](https://img.shields.io/badge/Git-F05032?style=flat-square&logo=git&logoColor=white)
![GitHub](https://img.shields.io/badge/GitHub-181717?style=flat-square&logo=github&logoColor=white)

---

| 기술 스택 | 선정 이유 및 활용 방안                                                                                  |
| :--- |:-----------------------------------------------------------------------------------------------|
| **Spring Security + JWT + RBAC** | JWT 기반 Stateless 인증으로 다중 Pod 환경 세션 부담을 제거하고, `HQ` · `STORE` · `WAREHOUSE` 역할별 URL 패턴 RBAC 으로 API 권한을 분리.    |
| **Spring Data JPA + MariaDB** | 매장, 본사, 물류창고의 재고·발주·입출고 이력을 관계형 모델로 관리하고 트랜잭션 기반 정합성을 확보                                       |
| **Spring Batch + ShedLock** | Pod 2개 환경에서 동일 발주건이 이중 승인되는 중복 실행 문제, for-loop 한 건 실패 시 전체 롤백 위험, 배치 실행 이력 추적 불가 -> 세 가지 구조적 결함을 해결, Spring Batch의 Partitioned Step으로 11개 지역창고를 독립 파티션으로 분리해 병렬 실행하여 기존 순차 처리 대비 이론상 최대 11배 처리 속도를 확보하며, 특정 지역창고 처리 지연이 전체 배치 시간에 영향을 주지 않음                      |
| **Elasticsearch 8.13.4** | 8.x에서 제공하는 벡터 검색 기능을 활용해 OpenAI 임베딩 기반 거래처 추천을 구현하고, 순환재고 거래처 검색의 Read-side 성능과 인덱스 동기화 안정성 확보 |
| **Spring AI + OpenAI** | 순환재고 판매 시 거래처 추천을 위해 임베딩과 추천 사유 생성을 연동                                                         |
| **SSE Notification** | 재고 부족, 계정 승인, 업무 상태 변경 등 주요 이벤트를 사용자에게 실시간 알림으로 전달                                             |
| **Actuator + Prometheus** | Kubernetes Probe와 Prometheus Scrape를 통해 헬스체크, 메트릭 수집, 운영 모니터링 기반 제공                            |
| **Docker + Jenkins + Kubernetes** | Gradle 빌드, Kaniko 이미지 빌드/푸시, Kubernetes Blue-Green 배포를 자동화하여 배포 안정성 강화                         |

---

## 📚 Documents & Wiki

> **프로젝트의 상세한 내용은 아래 문서에서 확인하실 수 있습니다.**

* 🎯 [**프로젝트 개요**](https://github.com/beyond-sw-camp/be24-fin-Stockers-Stockit-BE/wiki/1.-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8-%EA%B0%9C%EC%9A%94)
* ✨ [**코딩 컨벤션**]([docs/codex/common/도메인_공통_코딩규칙_컨벤션.md](https://github.com/beyond-sw-camp/be24-fin-Stockers-Stockit-BE/wiki/4.-%EC%BD%94%EB%94%A9-%EC%BB%A8%EB%B2%A4%EC%85%98))
* 🧾 [**기능 테스트**](https://github.com/beyond-sw-camp/be24-fin-Stockers-Stockit-BE/wiki/5.-%EA%B8%B0%EB%8A%A5-%ED%85%8C%EC%8A%A4%ED%8A%B8)
* 📦 [**무중단 배포**](https://github.com/beyond-sw-camp/be24-fin-Stockers-Stockit-BE/wiki/6.-%EB%AC%B4%EC%A4%91%EB%8B%A8-%EB%B0%B0%ED%8F%AC)
* 💰 [**Elasticsearch 도입**](https://github.com/beyond-sw-camp/be24-fin-Stockers-Stockit-BE/wiki/7.-Elasticsearch-%EB%8F%84%EC%9E%85)
* 🚚 [**매장 발주 승인 배치**](https://github.com/beyond-sw-camp/be24-fin-Stockers-Stockit-BE/wiki/8.-%EB%A7%A4%EC%9E%A5-%EB%B0%9C%EC%A3%BC-%EC%8A%B9%EC%9D%B8-%EB%B0%B0%EC%B9%98:-%EC%8A%A4%EC%BC%80%EC%A5%B4%EB%9F%AC-%E2%80%90---Spring-Batch%EB%A1%9C-%EC%A0%84%ED%99%98)
* 🚀 [**Spring Security**](https://github.com/beyond-sw-camp/be24-fin-Stockers-Stockit-BE/wiki/9.-Spring-Security)

---

## 🏗 System Architecture

<div align="center">

<img width="794" height="525" alt="image" src="https://github.com/user-attachments/assets/ed50ce43-ff30-4a15-a9cc-e65c964dca4d" />

</div>

### 🔹 Architecture Summary 선정 이유

| 구분 | 요약                                                                                                                                                                   |
| :--- |:---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Ingress 기반 라우팅** | `/` 요청은 Frontend, `/api` 요청은 Backend로 분기하여 서비스별 독립 스케일링이 가능하도록 구성했습니다. 외부 트래픽은 ALB와 Ingress에서 먼저 받아 클러스터 내부로 안전하게 전달하고, SSL 종료는 ALB에서 처리해 내부 서비스는 HTTP 통신에 집중합니다.    |
| **Nginx 정적 서버** | Ingress가 라우팅을 담당하므로 Frontend Nginx는 Vue 빌드 결과물 정적 서빙에만 집중합니다. 라우팅 관심사를 분리해 결합도를 낮추고, 빌드 산출물만 Nginx 이미지에 복사하는 단순한 컨테이너 구조를 유지합니다.                                     |
| **Blue/Green 배포** | 기존 Blue에 트래픽을 유지한 채 Green을 준비하고 헬스체크 후 한 번에 전환하여 무중단 배포를 진행합니다. 장애 발생 시 Service selector를 이전 색상으로 되돌리면 빠르게 롤백할 수 있고, 구버전과 신버전이 동시에 요청을 처리하는 상황을 줄입니다.                |
| **Spring Batch 분리** | 배치가 다른 기능들과 한 서버에 같이 있으면 배치 부하가 다른 API 서비스에 영향을 주기 때문에 별도로 분리했습니다. 배치를 별도 MSA 서버로 만들면 배치만 따로 독립 배포와 스케일링이 가능해지며, CronJob으로 운영하면  ShedLock 없이 K8s 자체가 배치의 중복 실행을 방지합니다.                                |
| **K8s CronJob** | `concurrencyPolicy: Forbid`로 중복 실행을 인프라 레벨에서 차단하고, 실행 완료 후 Pod를 자동 종료해 리소스 낭비를 줄입니다. 별도 분산 락 코드 없이 배치 중복 실행 방지 전략을 단순화합니다.                                           |
| **DB 인스턴스 공유 / 스키마 분리** | 스프링 배치 서버와 메인 서버는 인스턴스는 1개 공유하도록 하였고,  단 스키마는 논리적으로 분리 하였습니다(stockit, stockit_batch), `stockit-batch` 는 독립 도메인이 아니라 발주 데이터를 직접 처리하는 역할이므로  인스턴스를 분리하면 오히려 데이터 동기화 복잡성이 생기기 때문에 이 방식을 선택하였습니다.  커넥션은 서비스별로 분리 관리 (be=10/pod, batch=30/pod) 하여 배치실행 중 메인 서버 커넥션 잠식을 방지 하였고, 인스턴스 분리는 Grafana 모니터링에서 실제 병목이 확인될 때 검토할 예정입니다. |


---

## 📁 Project Structure

| 경로 | 역할 |
| :--- | :--- |
| `src/main/java/org/example/stockitbe/common` | 공통 응답 모델, 예외 처리, Spring Security 설정, JWT, CORS, Actuator Health 등 공통 기반 코드 |
| `src/main/java/org/example/stockitbe/user` | 회원가입, 로그인 이후 사용자 정보, 인증 사용자 모델, 마이페이지 기능 |
| `src/main/java/org/example/stockitbe/hq` | 본사 도메인: 계정 승인, 상품/카테고리/거래처, 인프라, 전사 재고, 본사 발주, 창고 이동, 순환재고, 분석, ESG |
| `src/main/java/org/example/stockitbe/store` | 매장 도메인: 매장 재고, 매장 발주, 판매 등록, 매장 입고 |
| `src/main/java/org/example/stockitbe/warehouse` | 물류창고 도메인: 창고 재고, 창고 입고, 창고 출고, 물류 대시보드 |
| `src/main/java/org/example/stockitbe/notification` | SSE 기반 실시간 알림, 알림 저장, 미읽음 카운트, 읽음 처리 |
| `src/main/resources` | 프로필별 설정, SQL seed/migration, ShedLock 테이블, Spring Batch 관련 리소스 |
| `CICD` | Dockerfile, docker-compose, Kubernetes manifest 등 배포 인프라 설정 |
| `docs` | 코딩 규칙, 테이블 스키마, 도메인별 설계/구현 문서 |

---

## 🔄 Service Flow

### 👤 사용자 및 권한 (Authentication & RBAC)
1. **회원가입 및 로그인** : 사용자는 가입 후 본사 승인 절차를 거쳐 사번 기반 계정을 사용합니다.
2. **JWT 인증** : 로그인 성공 시 Access Token과 Refresh Token을 발급받아 API 요청을 인증합니다.
3. **역할 기반 접근 제어** : `HQ`, `STORE`, `WAREHOUSE` 권한에 따라 본사·매장·물류창고 API 접근 범위를 분리합니다.

### 🏬 매장 관리자 (Store)
4. **매장 재고 조회** : 매장별 SKU 재고와 품목 정보를 조회하고 판매 가능 상태를 확인합니다.
5. **판매 등록** : 매장 판매 내역을 등록하여 재고 차감과 판매 이력을 관리합니다.
6. **발주 요청** : 매장 부족 재고를 기준으로 물류창고에 발주를 요청하고, 대기·승인·취소 상태를 추적합니다.
7. **입고 확정** : 물류창고에서 출고된 상품이 매장에 도착하면 검수 후 입고를 확정합니다.

### 🏢 본사 관리자 (HQ)
8. **기준정보 관리** : 상품, 카테고리, 거래처, 거래처 상품, 인프라 정보를 관리합니다.
9. **매장-창고 매핑** : 매장별 주 물류창고와 예비 물류창고를 지정하여 발주 라우팅 기준을 관리합니다.
10. **본사 발주 관리** : 거래처를 대상으로 창고 보충 발주를 생성하고 상태 전환을 관리합니다.
11. **창고 간 재고 이동** : 창고별 재고 불균형을 확인하고 창고 간 이동 지시를 실행합니다.
12. **순환재고 판매** : 장기 재고와 소재 정보를 기반으로 순환재고 판매를 등록하고 적합 거래처를 추천받습니다.

### 🚚 물류창고 관리자 (Warehouse)
13. **창고 입고 처리** : 본사 발주 또는 이동 지시에 따라 창고 입고 예정 건을 확인하고 입고 확정합니다.
14. **창고 출고 처리** : 매장 발주 또는 순환재고 판매 건에 대해 출고, 배송, 도착 상태를 관리합니다.
15. **창고 재고 및 대시보드** : 창고별 재고, 입출고 작업 현황, 운영 지표를 조회합니다.

### 🤖 자동화 · AI · 알림 (Automation & Intelligence)
16. **발주 자동 전환 배치** : 일정 시간이 지난 본사 발주를 승인, 배송 준비, 배송 중, 도착 상태로 자동 전환합니다.
17. **SSE 실시간 알림** : 재고 부족, 계정 승인, 업무 이벤트를 대상 역할·사용자에게 실시간으로 전송합니다.
18. **AI 거래처 추천** : OpenAI 임베딩을 활용해 순환재고 특성에 맞는 거래처를 추천하고 추천 사유를 생성합니다.
19. **ESG 데이터 관리** : 탄소 가격, 배출권, 소재 계수, 순환 매출 등 ESG 관련 데이터를 조회합니다.

---

## 🚀 CI/CD Pipeline

| 단계 | 내용 |
| :--- | :--- |
| **Build** | Jenkins Kubernetes Agent에서 `./gradlew --no-daemon clean bootJar`로 Spring Boot 실행 JAR를 빌드 |
| **Image** | Kaniko가 `CICD/docker/Dockerfile` 기반으로 이미지를 빌드하고 Docker Hub의 `sunyeoplee/stockit-backend:${BUILD_NUMBER}`, `latest`로 푸시 |
| **Deploy** | Kubernetes의 `stockit-be-blue` / `stockit-be-green` 중 비활성 색상에 새 이미지를 배포 |
| **Verify** | `/actuator/health/readiness`, `/actuator/health/liveness`로 Pod 상태를 확인한 뒤 Service selector를 새 색상으로 전환 |
| **Rollback** | 배포 실패 시 이전 active color를 다시 scale up하고 Service selector를 기존 색상으로 복구 |
| **Monitoring** | 운영 프로필에서 `/actuator/prometheus`를 노출하고 Prometheus scrape annotation으로 메트릭을 수집 |

---

## 🚀 Quickstart (Docker Compose)

Docker Desktop 만 있으면 풀스택(DB · ES · Kibana · Logstash · BE · FE) 을 한 번에 띄울 수 있습니다.

```bash
cp .env.example .env     # OPENAI_API_KEY 채우기
docker compose up -d
```

→ http://localhost:8888 접속. 시드 계정: `hq0001` / `st0001` / `wh0001` (비밀번호 `Stockit!2026`).

---

## ⚙️ Local Run
로컬 실행 시 아래 환경 변수가 필요합니다.

| 환경 변수 | 설명 |
| :--- | :--- |
| `DB_URL` | MariaDB JDBC URL |
| `DB_USER` | MariaDB 사용자 |
| `DB_PASS` | MariaDB 비밀번호 |
| `JWT_SECRET` | JWT 서명용 Secret |
| `JWT_ACCESS_EXPIRATION_MS` | Access Token 만료 시간 |
| `JWT_REFRESH_EXPIRATION_MS` | Refresh Token 만료 시간 |
| `OPENAI_API_KEY` | AI 거래처 추천 기능용 OpenAI API Key |
| `PUBLIC_DATA_API_KEY` | 탄소 가격 조회용 공공데이터 API Key |
| `ES_HOST` / `ES_PORT` | Elasticsearch 접속 정보 |
| `ES_USER` / `ES_PASSWORD` | Elasticsearch 인증 정보 |

<br>

> 최초 셋업 시 Spring Batch 메타 테이블과 ShedLock 테이블 초기화 설정을 확인한 뒤, 운영 환경에서는 자동 초기화 설정을 보수적으로 관리해야 합니다.

---

<div align="center">
  <br>
  <b>📦 Backend powered by STOCKIT</b>
</div>
