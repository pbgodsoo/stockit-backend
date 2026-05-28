<div align="center">

# 📦 STOCKIT Backend

### 매장 · 본사 · 물류창고의 재고/발주/입출고/순환재고를 통합 관리하는 백엔드 서비스

</div>

<br>

> ### 🔗 Project Links
> 🌐 **Web Service** : [STOCKIT 공식 서비스 접속하기](https://www.stockit.kro.kr)<br/>
> 📘 **API Docs** : [Swagger API 명세서](https://www.stockit.kro.kr/swagger-ui/index.html) (`/api/swagger-ui/index.html` 경로로 프록시될 수 있습니다)<br/>
> 🗂️ **Repository** : [be24-fin-Stockers-Stockit-BE](https://github.com/beyond-sw-camp/be24-fin-Stockers-Stockit-BE)

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

| 기술 스택 | 선정 이유 및 활용 방안 |
| :--- | :--- |
| **Java 17 / Spring Boot 3.5.11** | Java 17 LTS와 Spring Boot 3.x 기반으로 안정적인 런타임을 확보하고, REST API · Security · JPA · Batch · Actuator 등 스프링 생태계 기능을 일관되게 사용 |
| **Spring Security + JWT + RBAC** | 서버 세션 의존도를 낮춘 Stateless 인증 구조를 적용하고, `HQ` · `STORE` · `WAREHOUSE` 역할별 API 접근 범위를 분리 |
| **Spring Data JPA + MariaDB** | 매장, 본사, 물류창고의 재고·발주·입출고 이력을 관계형 모델로 관리하고 트랜잭션 기반 정합성을 확보 |
| **Spring Batch + ShedLock** | 본사 발주 상태 자동 전환과 반복 작업을 배치로 처리하며, 다중 파드 환경에서도 중복 실행을 방지 |
| **Elasticsearch 8.13.4** | 순환재고 거래처 검색/추천의 Read-side 성능을 높이고, Outbox 기반 재시도로 검색 인덱스 동기화 안정성 확보 |
| **Spring AI + OpenAI** | 순환재고 판매 시 거래처 추천을 위해 임베딩과 추천 사유 생성을 연동 |
| **SSE Notification** | 재고 부족, 계정 승인, 업무 상태 변경 등 주요 이벤트를 사용자에게 실시간 알림으로 전달 |
| **Actuator + Prometheus** | Kubernetes Probe와 Prometheus Scrape를 통해 헬스체크, 메트릭 수집, 운영 모니터링 기반 제공 |
| **Docker + Jenkins + Kubernetes** | Gradle 빌드, Kaniko 이미지 빌드/푸시, Kubernetes Blue-Green 배포를 자동화하여 배포 안정성 강화 |

---

## 📚 Documents & Wiki

> **프로젝트의 상세한 내용은 아래 문서에서 확인하실 수 있습니다.**

* 🎯 [**프로젝트 기획서 (Project Overview)**](../프로젝트%20기획서.md)
* ✨ [**도메인 공통 코딩 규칙 및 컨벤션**](docs/codex/common/도메인_공통_코딩규칙_컨벤션.md)
* 🧾 [**매장 발주 테이블 스키마**](docs/table/store_order_schema.sql)
* 📦 [**매장 입고 테이블 스키마**](docs/table/store_inbound_schema.sql)
* 💰 [**매장 판매 테이블 스키마**](docs/table/store_sales_schema.sql)
* 🚚 [**물류창고 출고 테이블 스키마**](docs/table/wh_outbound_schema.sql)
* 🚀 [**순환재고 거래처 추천 ES 성능 개선 기획서**](../성능개선/순환재고_거래처_추천_ES_성능개선_기획서.md)
* 📈 [**AS-IS 부하 테스트 결과**](../성능개선/순환재고_거래처_추천_ES_성능개선_ASIS_부하테스트_결과.md)
* 📊 [**TO-BE 부하 테스트 결과**](../성능개선/순환재고_거래처_추천_ES_성능개선_TOBE_부하테스트_결과.md)

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
18. **AI 거래처 추천** : Spring AI와 OpenAI 임베딩을 활용해 순환재고 특성에 맞는 거래처를 추천하고 추천 사유를 생성합니다.
19. **ES 동기화 재시도** : 순환재고 거래처 데이터 변경 시 Elasticsearch 인덱스 동기화를 수행하고 실패 건은 재시도합니다.
20. **ESG 데이터 관리** : 탄소 가격, 배출권, 소재 계수, 순환 매출 등 ESG 관련 데이터를 조회합니다.

---

## 🚀 CI/CD Pipeline

### 🔹 Build
1. Jenkins Pipeline은 Kubernetes Agent에서 실행됩니다.
2. `gradle:8.9-jdk17` 컨테이너가 소스를 체크아웃하고 Gradle Wrapper 권한을 설정합니다.
3. `./gradlew --no-daemon clean bootJar` 명령으로 Spring Boot 실행 JAR를 빌드합니다.

### 🔹 Image Build & Push
4. Kaniko 컨테이너가 `CICD/docker/Dockerfile`을 사용해 Docker 이미지를 빌드합니다.
5. 이미지는 Docker Hub의 `sunyeoplee/stockit-backend:${BUILD_NUMBER}`와 `sunyeoplee/stockit-backend:latest`로 푸시됩니다.
6. Dockerfile은 `eclipse-temurin:17-jre` 기반으로 빌드된 JAR를 `/app/app.jar`에 복사하고 8080 포트로 실행합니다.

### 🔹 Blue-Green Deployment
7. Kubernetes에는 `stockit-be-blue`, `stockit-be-green` 두 Deployment가 존재합니다.
8. 현재 Service selector의 `color` 값을 확인해 활성 색상을 판단하고, 반대 색상 Deployment를 새 이미지로 업데이트합니다.
9. Target Deployment를 2개 replica로 확장한 뒤 rollout 상태와 Pod Ready 상태를 확인합니다.
10. Service selector를 일시적으로 양쪽 색상으로 확장한 뒤 기존 색상을 scale down하고, 최종적으로 target 색상만 바라보도록 selector를 좁힙니다.
11. 배포 실패 시 이전 active color를 다시 scale up하고 Service selector를 롤백합니다.

### 🔹 Health Check & Monitoring
12. Kubernetes Probe는 `/actuator/health/readiness`, `/actuator/health/liveness`를 사용합니다.
13. 운영 프로필에서는 `/actuator/prometheus`를 노출하고, Pod annotation으로 Prometheus scrape를 활성화합니다.
14. Blue-Green 전환 중 진행 중인 SSE 연결을 정리할 수 있도록 `preStop`과 `terminationGracePeriodSeconds`를 설정합니다.

---

## ⚙️ Local Run

```bash
./gradlew bootRun
```

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
