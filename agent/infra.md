# Infrastructure & Git

## Docker Compose

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
```

- **MySQL 포트**: 외부 3316 → 내부 3306
- **DB 초기화**: `backend/sql/01_init.sql` (데이터베이스 생성만, 테이블은 JPA auto-ddl)
- **환경 변수**: `.env` + `.secret.env`
- **실행**: `bash run_docker.sh` 또는 `./run_docker.ps1`

## Gradle 빌드

- **설정**: `backend/settings.gradle` — 루트 프로젝트명 `demo-app`, 멀티모듈 `:lib:web-starter` 포함
- **Jar**: `bootJar`에서 계층화(layered) 빌드 → `app.jar`
- **테스트**: `./gradlew test` (H2 인메모리 DB)
- **빌드**: `./gradlew build`

## CI/CD

- `.github/` 디렉토리 존재 (GitHub Actions)
- 구체적인 워크플로우는 `.github/workflows/` 확인 필요

## Git 브랜치 전략

| 구분 | 설명 |
|------|------|
| 메인 브랜치 | `dev` |
| 작업 브랜치 | `task/{번호}` |
| 커밋 컨벤션 | [commit-convention.md](commit-convention.md) 참조 |
| 커밋 형식 | `[TASK-{번호}] {type}: {한글 설명}` |
| 커밋 타입 | `feat`, `fix`, `chore`, `test`, `refactor`, `init`, `clean`, `docs`, `style` |
| 태스크 시작 | `[TASK-{번호}] chore: start task-{번호}` |

## 환경 변수 파일

| 파일 | 용도 |
|------|------|
| `backend/.env` | 공개 환경 변수 (DB 호스트, 포트 등) |
| `backend/.secret.env` | 비밀 환경 변수 (DB 비밀번호, JWT 시크릿 등) |
| `backend/.env.example` | `.env` 템플릿 |
| `backend/.secret.env.example` | `.secret.env` 템플릿 |

## 프로젝트 실행 방법

1. **DB 실행**: `docker-compose up -d mysql` (backend 디렉토리에서)
2. **백엔드 실행**: `./gradlew bootRun` (backend 디렉토리에서)
3. **프론트엔드 실행**: `npm run dev` (frontend 디렉토리에서)
4. **전체 실행**: `docker-compose up -d` (backend 디렉토리에서)
