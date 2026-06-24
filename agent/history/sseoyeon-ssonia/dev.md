### 작업지시 마감 테스트 수정 (Codex)
- **User Intent**: `./gradlew clean test --refresh-dependencies --no-configuration-cache` 실행 시 `WorkOrderServiceTest.closeWorkOrder_underTargetRequiresRequestFlag`에서 `expected: 8 but was: 0` assertion 실패가 발생하여, 테스트를 서비스 규칙에 맞추는 방식으로 수정 요청.
- **Agent Context**: `data.sql`의 `창원제1공장/A라인`은 5개 공정을 가지지만 테스트가 3번째 공정을 마지막 공정으로 가정해, 서비스의 실제 마지막 공정 기준 집계 결과가 0으로 반환되는 원인을 확인하고 마지막 라우팅까지 실적을 등록하도록 테스트를 수정.
- **Key Decisions**:
  - 테스트가 seed 데이터의 고정 공정 인덱스에 의존하지 않고 `lineRoutings.size() - 1`을 마지막 공정으로 사용하도록 변경 — `agent/project/backend.md`의 실제 코드 우선 및 기존 서비스 규칙 준수.
  - 앞 공정은 양품 9개, 마지막 공정은 양품 8개와 불량 1개로 등록하도록 반복 처리 — `ProductionExecutionServiceImpl`의 이전 공정 양품 수량 기반 공정 진행 검증 규칙과 일치.
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
    - `agent/history/sseoyeon-ssonia/dev.md` — 작업 히스토리 신규 기록
  - **Modified**:
    - `backend/src/test/java/com/ssafy/demo_app/domain/production/service/WorkOrderServiceTest.java` (+8/-10) — 목표 미달 마감 테스트가 실제 마지막 공정까지 생산 실적을 등록하도록 수정
  - **Deleted**:

  </details>
