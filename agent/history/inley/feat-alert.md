### Discord CI/CD 알림 추가 (Claude Code)
- **User Intent**: 3개 워크플로우(backend-ci, frontend-ci, deploy-main)에 빌드/배포 결과를 Discord로 전송하는 알림 기능을 추가 요청. 기존 프로젝트에서 사용하던 Discord embed 포맷을 그대로 재사용.
- **Agent Context**: 현재 모든 워크플로우에 Discord 알림이 전혀 없는 상태였음. 기존 코드는 전혀 수정하지 않고, 각 job의 마지막 step 뒤에 Notify step만 append 방식으로 추가.
- **Key Decisions**:
  - PR 전용 CI(backend-ci, frontend-ci)는 간단한 curl+고정 포맷 사용 — 트리거가 PR로 고정되어 PR 컨텍스트 항상 사용 가능
  - PR+Push 혼용(deploy-main.yml)은 bash 조건문으로 PR/push 분기 처리 — `github.event_name`으로 PR 컨텍스트와 push 컨텍스트 각각 적절한 description 포맷 적용
  - Deploy job의 알림은 PR/push 구분 없이 단일 포맷 사용 — `github.ref_name`, `github.actor`, `github.sha`로 항상 식별 가능
- **Affected Files**: <details><summary>3개 파일</summary>

  - **Modified**:
    - `.github/workflows/backend-ci.yml` (+31/-0) — PR→dev 빌드 성공/실패 Discord 알림 step 추가 (success+failure)
    - `.github/workflows/frontend-ci.yml` (+31/-0) — PR→dev 빌드 성공/실패 Discord 알림 step 추가 (success+failure)
    - `.github/workflows/deploy-main.yml` (+118/-0) — backend/frontend/deploy 3개 job 각각 성공/실패 Discord 알림 추가, deploy-main은 PR/push 이벤트 모두 대응

  </details>
