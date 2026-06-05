# Infrastructure

> 인프라, 배포, Git 워크플로우.

---

# 프로젝트 실행

1. **DB 실행**: `docker-compose up -d mysql` (backend 디렉토리에서)
2. **백엔드 실행**: `./gradlew bootRun` (backend 디렉토리에서)
3. **프론트엔드 실행**: `npm run dev` (frontend 디렉토리에서)
4. **전체 실행**: `docker-compose up -d` (backend 디렉토리에서)

---

# Git 브랜치 전략

| 구분 | 설명 |
|------|------|
| 메인 브랜치 | `dev` — 프로젝트 기본 브랜치 |
| 작업 브랜치 | `task/{번호}` (예: `task/49`) |
| 커밋 컨벤션 | [commit-convention.md](../commit-convention.md) 참조 |
| 커밋 형식 | `[TASK-{번호}] {type}: {한글 설명}` |
| 커밋 타입 | `feat`, `fix`, `chore`, `test`, `refactor`, `init`, `clean`, `docs`, `style` |

## 브랜치 생성 및 병합 규칙

- 작업 시작 전 반드시 `dev`에서 새 `task/{번호}` 브랜치를 생성한다.
- 작업 완료 후 PR(Pull Request)을 통해 `dev`에 병합한다.
- 직접 `dev`에 push하는 행위는 절대 금지한다.
- 태스크 시작 시 `[TASK-XX] chore: start task-XX` 커밋을 반드시 작성한다.

---

# 환경 변수 파일

민감 정보가 포함될 수 있는 환경 변수 파일은 절대 버전 관리에 포함해서는 안 된다:

| 파일 | 용도 | Git 추적 |
|------|------|----------|
| `backend/.env` | 공개 환경 변수 (DB 호스트, 포트 등) | 선택적 (`.gitignore` 결정) |
| `backend/.secret.env` | 비밀 환경 변수 (DB 비밀번호, JWT 시크릿 등) | **절대 금지** |
| `backend/.env.example` | `.env` 템플릿 | Git 추적 |
| `backend/.secret.env.example` | `.secret.env` 템플릿 (실제 값 제외) | Git 추적 |

`.env`와 `.secret.env`가 실수로 커밋되지 않도록 `.gitignore`에 반드시 등록한다:

```gitignore
.env
.secret.env
!*.example
```

---

# 컨테이너 오케스트레이션

파일 위치: `backend/docker-compose.yml`

```yaml
services:
  mysql:                # MySQL 8.0
    port: 3316:3306
    healthcheck: mysqladmin ping
    volumes:
      - mysql_data:/var/lib/mysql
      - ./sql:/docker-entrypoint-initdb.d   # 초기화 SQL
  app:                  # Spring Boot
    build: Dockerfile (context: backend/)
    port: 8080:8080
    depends_on: mysql (condition: service_healthy)
    volumes:
      - ./logs:/app/logs
    env_file:
      - .env
      - .secret.env
```

## 서비스 구성 원칙

- **MySQL**: 포트 외부 3316 → 내부 3306. named volume으로 데이터 영속성. `backend/sql/01_init.sql`로 DB 초기화 (DB 생성만, 테이블은 JPA auto-ddl). healthcheck로 앱보다 먼저 준비 완료.
- **Spring Boot**: Dockerfile로 빌드. MySQL healthcheck 이후 시작. 로그 볼륨 마운트로 호스트 보존.
- **환경 변수**: `.env`와 `.secret.env`를 `env_file`로 주입.

## Dockerfile 구성 원칙

- 경량 베이스 이미지 사용.
- `WORKDIR`, `COPY`, `EXPOSE`, `ENTRYPOINT` 순서로 구성.
- 계층화(layered) 빌드 → `app.jar`.

---

# 빌드

- **빌드 도구**: Gradle, 멀티모듈 (`backend/settings.gradle` — 루트 `demo-app`, `:lib:web-starter`)
- **테스트**: `./gradlew test` (H2 인메모리 DB)
- **빌드**: `./gradlew build` (계층화 Jar 생성)
- **의존성 캐싱**: CI 환경에서 Gradle 캐시 활용.

---

# CI/CD

- GitHub Actions 사용 (`.github/` 디렉토리 존재)
- CI/CD 파이프라인에 반드시 포함되어야 하는 단계:
  1. **테스트**: 유닛/통합 테스트 실행
  2. **빌드**: 프로덕션용 아티팩트 생성
  3. **린트/포맷 검사** (선택): 코드 품질 게이트

---

# 보안 정책

- `.env`, `.secret.env` 파일은 어떠한 경우에도 버전 관리에 포함해서는 안 된다. 이 규칙을 위반할 경우 보안 사고로 간주한다.
- CI/CD 시크릿은 환경 변수 파일이 아닌 CI/CD 도구의 시크릿 관리 기능을 사용한다.
- Docker 볼륨 데이터(`mysql_data`, `logs` 등)는 `.gitignore`에 추가하여 실수로 커밋되지 않도록 한다.

---

# 주의사항

- DB 마이그레이션 도구(Flyway/Liquibase) 미사용. 엔티티 변경 시 테이블 스키마 정합성 수동 확인 필요.
- 실행 스크립트: `bash run_docker.sh` 또는 `./run_docker.ps1` 제공.
- 실행 전 환경 변수 파일 존재 여부 검증 로직 포함.
