### BOM 기준정보 생산 연계 개선 (Codex)
- **User Intent**: BOM 기준정보 페이지 분석 결과를 바탕으로 작업지시 BOM 버전 연결, 수량 단위 처리, 다단계 전개, 삭제 정책, 검색/상태 관리, 역전개 UI, API 정합성, 테스트 추가를 구현하되 변경 이력/스냅샷 항목은 제외하도록 요청
- **Agent Context**: 기존 BOM 생산 연계가 동일 품목의 모든 버전을 합산하고 `BigDecimal` 수량을 정수 절삭하는 문제가 있어, 작업지시가 선택한 활성 BOM 버전만 기준으로 생산 소요량을 계산하도록 백엔드 서비스와 프론트 화면을 함께 정리
- **Key Decisions**:
  - 작업지시 엔티티에 `bomVersion`을 직접 보관 — 생산/불출 시점에 선택한 BOM 버전을 명확히 전달하기 위해 기존 생산 도메인 서비스 흐름을 유지하면서 필드만 확장
  - BOM 삭제는 하드 삭제 대신 `INACTIVE` 상태 전환으로 구현 — 과거 생산/분석 기준정보 연결을 끊지 않으면서 기존 DELETE API 사용 흐름을 보존
  - 다단계 BOM 전개는 BOM 도메인 서비스에서 재귀 계산으로 집중 — 생산 서비스가 BOM 계층 구조를 직접 해석하지 않도록 도메인 책임을 분리
  - 프론트엔드는 기존 Pinia Store와 Service 계층을 유지 — `agent/project/frontend.md`의 API/Service/Store/View 계층 구조를 따르며 View의 직접 상태 대입을 Store 액션으로 대체
- **Affected Files**: <details><summary>26개 파일</summary>

  - **Created**:
    - `backend/src/main/java/com/ssafy/demo_app/api/bom/dto/BomStatusUpdateRequest.java` — BOM 활성/비활성 상태 변경 요청 DTO
    - `backend/src/main/java/com/ssafy/demo_app/domain/bom/service/BomRequirement.java` — 생산 소요량 계산 결과 record
    - `backend/src/test/java/com/ssafy/demo_app/domain/bom/service/BomServiceTest.java` — BOM 생성, 중복, 순환 참조, 비활성, 트리 조회 테스트
  - **Modified**:
    - `agent/project/backend.md` (+5/-3) — BOM API 경로와 권한 정책 문서 정합성 수정
    - `backend/src/main/java/com/ssafy/demo_app/api/bom/BomApi.java` (+12/-3) — BOM 단건 조회 경로 통일 및 상태 변경 API 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/bom/BomController.java` (+8/-1) — BOM 비활성 삭제 응답과 상태 변경 엔드포인트 연결
    - `backend/src/main/java/com/ssafy/demo_app/api/bom/dto/BomResponse.java` (+2/-0) — BOM 상태 응답 필드 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/production/dto/WorkOrderCreateRequest.java` (+3/-0) — 작업지시 생성 BOM 버전 입력 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/production/dto/WorkOrderResponse.java` (+2/-0) — 작업지시 응답 BOM 버전 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/production/dto/WorkOrderUpdateRequest.java` (+3/-0) — 작업지시 수정 BOM 버전 입력 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/bom/entity/BomStructure.java` (+9/-0) — BOM 활성 상태 필드와 enum 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/bom/repository/BomStructureRepository.java` (+43/-0) — 활성 BOM 조회, 버전 조회, 조건 검색 JPQL 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/bom/service/BomService.java` (+5/-0) — 상태 변경, 버전 검증, 소요량 계산 계약 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/bom/service/BomServiceImpl.java` (+145/-23) — 활성 BOM 기준 검색, 삭제 정책, 다단계 소요량 계산 구현
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/entity/WorkOrder.java` (+3/-0) — 작업지시 BOM 버전 저장 필드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/service/ProductionExecutionServiceImpl.java` (+16/-20) — 생산 실적 투입량 계산을 선택 BOM 버전 기반으로 변경
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/service/WorkOrderServiceImpl.java` (+39/-26) — 작업지시 생성/수정 검증과 자재 불출 계산을 BOM 서비스로 위임
    - `backend/src/main/java/com/ssafy/demo_app/infrastructure/security/config/SecurityConfig.java` (+4/-1) — BOM 조회/등록/수정/삭제 권한을 기준정보 수준으로 정리
    - `backend/src/test/java/com/ssafy/demo_app/domain/production/service/WorkOrderServiceTest.java` (+105/-3) — BOM 버전, 다단계 전개, 소수 수량 올림 테스트 추가
    - `frontend/src/api/bomMasterApi.ts` (+9/-1) — BOM 상태 타입과 단건 조회 경로, 상태 변경 API 추가
    - `frontend/src/api/workOrderApi.ts` (+2/-0) — 작업지시 BOM 버전 타입 추가
    - `frontend/src/services/bomMasterService.ts` (+9/-0) — BOM 상태 변경 서비스 추가
    - `frontend/src/state/bomMasterStore.ts` (+32/-1) — BOM 상태 변경 액션과 검색 상태 초기화 액션 추가
    - `frontend/src/views/BomMasterView.vue` (+109/-25) — 서버 검색, 권한별 버튼, 상태 표시, 역전개 사용처 목록, CSS 변수 수정
    - `frontend/src/views/WorkOrderView.vue` (+48/-8) — 작업지시 BOM 버전 선택과 상세 표시 추가
  - **Deleted**:
    - 없음

  </details>

### BOM 목록 검색 자동완성 복구 (Codex)
- **User Intent**: BOM 목록 검색 조건에서 키워드를 입력해도 관련 품목 후보가 뜨지 않아, 기존처럼 키워드 입력 시 관련 항목을 선택할 수 있도록 복구 요청
- **Agent Context**: BOM 목록 검색 필터는 단순 input만 남아 있고, 정전개/역전개에서 사용하는 품목 후보 picker 로직이 연결되어 있지 않았다. 기존 parent/child 후보 조회 Store 액션과 picker 스타일을 재사용해 목록 필터에도 자동완성을 추가
- **Key Decisions**:
  - 목록 필터 전용 picker open 상태와 후보 computed를 추가 — 기존 검색 문자열 필터 흐름을 유지하면서 후보 메뉴만 되살리기 위함
  - 후보 선택 시 품목 코드를 필터 input에 반영 — 기존 BOM 목록 API의 keyword 검색과 호환되고, 사용자가 직접 입력한 키워드 검색도 계속 가능하게 하기 위함
  - 검색 조건 패널에 `bom-query-panel`을 적용 — picker 메뉴가 패널 overflow에 잘리지 않도록 기존 정전개/역전개 패널 스타일을 재사용
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/BomMasterView.vue` — BOM 목록 상위 품목/구성 품목 검색 필터에 품목 후보 자동완성과 선택 기능 복구
  - **Deleted**:
    - 없음

  </details>

### BOM 페이지네이션 UI 통일 (Codex)
- **User Intent**: BOM 목록 페이지네이션 버튼 디자인을 첨부 이미지처럼 거래처 마스터 관리 페이지의 페이지네이션 디자인과 동일하게 변경 요청
- **Agent Context**: BOM 목록은 기존에 `이전/다음` 버튼과 현재 페이지 숫자만 표시하는 별도 `.bom-pagination` 스타일을 사용하고 있었다. 거래처 마스터와 같은 정보 문구와 `처음/이전/다음/마지막` 버튼 구조로 교체
- **Key Decisions**:
  - 거래처 마스터의 Tailwind 클래스 기반 페이지네이션 마크업을 BOM 목록에 적용 — 기준정보 화면 간 UI 일관성을 맞추기 위함
  - `pageStart`와 `pageEnd` 계산값을 View computed로 추가 — 총 건수, 표시 범위, 페이지 정보를 거래처 화면과 같은 형식으로 보여주기 위함
  - 더 이상 사용하지 않는 `.bom-pagination` scoped CSS 제거 — 같은 화면 안에서 중복 페이지네이션 스타일이 남지 않도록 정리
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/BomMasterView.vue` — BOM 목록 페이지네이션을 거래처 마스터와 같은 표시 문구 및 처음/이전/다음/마지막 버튼 UI로 변경
  - **Deleted**:
    - 없음

  </details>

### BOM 통계 카드 전체 기준 수정 (Codex)
- **User Intent**: BOM 목록 상단 통계 카드의 상위 품목, 구성 품목, BOM 버전 수가 현재 페이지에 표시된 목록 기준으로 바뀌어 페이지 1과 페이지 2에서 서로 다른 값으로 보이는 문제 수정 요청
- **Agent Context**: `BomMasterView.vue`의 통계 계산이 페이지네이션된 `bomGroups.content`에서 파생된 `bomParentGroups`만 사용하고 있었다. 프론트 Store에 필터 전체 결과를 담는 통계 전용 상태를 추가하고, 카드 계산은 해당 전체 결과를 기준으로 하도록 변경
- **Key Decisions**:
  - 페이지 목록 상태와 통계 상태를 분리 — 목록 페이지네이션은 유지하면서 통계 카드만 전체 필터 결과 기준으로 계산하기 위함
  - 통계 조회는 기존 `getBomGroups` 서비스와 API를 재사용 — `agent/project/frontend.md`의 API/Service/Store/View 계층 구조를 지키면서 백엔드 API 추가 없이 해결하기 위함
  - 상위 품목 수는 `parentItemId` 중복 제거 기준으로 계산 — 동일 상위 품목의 여러 BOM 버전이 있을 때 그룹 수가 아니라 실제 상위 품목 수를 표시하기 위함
- **Affected Files**: <details><summary>2개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/state/bomMasterStore.ts` — BOM 통계 전용 전체 그룹 상태와 로딩 액션 추가
    - `frontend/src/views/BomMasterView.vue` — 통계 카드를 현재 페이지가 아닌 전체 필터 결과 기준으로 계산하도록 수정
  - **Deleted**:
    - 없음

  </details>

### BOM 등록 모달 스크롤 개선 (Codex)
- **User Intent**: BOM 등록 또는 수정 작업창이 길어질 때 화면 하단 항목과 버튼이 보이지 않고 스크롤도 되지 않아 모든 입력 항목을 확인할 수 없는 문제 수정 요청
- **Agent Context**: `BomMasterView.vue`의 BOM 등록 모달은 fixed backdrop 중앙 정렬과 고정 높이 제한 없는 form 구조라, 구성 품목 목록이 늘어나면 모달이 뷰포트를 넘어가고 내부 스크롤이 생기지 않았다. 프론트 View 스타일 범위에서 모달 높이와 overflow만 조정
- **Key Decisions**:
  - 모달 backdrop에 `overflow-y: auto`와 상단 정렬을 적용 — 작은 화면이나 긴 구성 품목 목록에서도 모달 전체 접근성을 확보하기 위함
  - 모달 form에 `max-height: calc(100vh - 2rem)`와 `overflow-y: auto` 적용 — 페이지 전체가 아닌 작업창 내부만 스크롤되도록 제한하기 위함
  - 모달 헤더와 액션 영역을 sticky 처리 — 긴 입력 목록을 스크롤해도 제목과 저장/취소 버튼 접근성을 유지하기 위함
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/BomMasterView.vue` — BOM 등록/수정 모달의 뷰포트 높이 제한, 내부 스크롤, sticky 헤더/하단 액션 적용
  - **Deleted**:
    - 없음

  </details>

### BOM 상세 화면 UI 개선 (Codex)
- **User Intent**: BOM 기준정보 상세 페이지의 정전개 트리와 상세 UI가 단순 텍스트 나열처럼 보여 거래처/품목 상세 페이지와 비슷한 UI로 개선하고, 상세 페이지에서도 비활성화/삭제가 가능하도록 요청
- **Agent Context**: 기존 BOM 상세 화면은 별도 scoped `.bom-*` CSS 중심이라 품목/거래처 상세 화면의 Tailwind + `app-*` 토큰 기반 패턴과 맞지 않았다. 백엔드와 프론트 Store에는 이미 BOM 상태 변경과 삭제성 비활성화 API가 연결되어 있어 프론트 화면 구조와 액션 UX만 우선 정리
- **Key Decisions**:
  - BOM 상세 화면을 헤더 액션, 요약 카드, 탭 패널, 확인 모달 구조로 재구성 — `agent/project/frontend.md`의 View 계층 책임 안에서 기존 Store 액션을 재사용하면서 품목/거래처 상세 화면 패턴과 맞추기 위함
  - 그룹 단위 활성/비활성 처리는 현재 상세의 구성 line 전체에 기존 `updateBomStatus`와 `deleteBom` 액션을 반복 호출 — 백엔드 그룹 API를 새로 늘리지 않고 사용자가 요청한 프론트 우선 수정 범위를 준수
  - BOM 트리 컴포넌트는 `--bom-*` 전용 스타일 의존을 제거하고 공통 색상 토큰 기반 카드형 노드로 변경 — 다른 기준정보 화면과 시각 체계를 맞추고 첨부 이미지의 큰 여백과 단순 텍스트 느낌을 줄이기 위함
- **Affected Files**: <details><summary>2개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/BomMasterDetailView.vue` (+315/-0) — BOM 상세 화면을 품목/거래처 상세 패턴의 헤더, 요약 카드, 탭, 그룹 비활성화 모달 구조로 재작성
    - `frontend/src/ui/BomTreeList.vue` (+64/-35) — 정전개 트리 노드를 공통 앱 토큰 기반 카드형 UI로 변경
  - **Deleted**:
    - 없음

  </details>

### BOM 그룹 상세와 일괄 등록 개선 (Codex)
- **User Intent**: BOM 등록 시 상위 품목 1개에 구성 품목 여러 개를 한 번에 포함하고, 다른 기준정보처럼 목록/상세 페이지를 분리하며, 목록은 10개 단위 페이지네이션으로 제공하도록 요청
- **Agent Context**: 기존 BOM 화면은 `BomStructure` row 단위로 목록과 상세가 한 화면에 섞여 있어 사용자가 인식하는 BOM 단위와 맞지 않았다. 사용자 기준 BOM 단위를 `상위 품목 + BOM 버전` 그룹으로 정의하고, 기존 row 기반 생산/불출 로직은 유지하면서 그룹 API와 상세 화면을 추가
- **Key Decisions**:
  - BOM 그룹 식별자를 `parentItemId + bomVersion`으로 사용 — 단일 `bomId`는 구성품 line 1개만 의미하므로 목록/상세 UX의 BOM 단위로 부적합
  - 일괄 등록은 신규 `/api/boms/bulk` API로 추가 — 기존 단건 등록/수정 API 호환성을 유지하면서 상위 품목 1개와 다건 구성 품목을 하나의 트랜잭션으로 처리
  - 목록은 `/api/boms/groups`에서 `PageResponse`로 반환 — 다른 기준정보와 같은 페이지 응답 형식을 사용하고 기본 10개 단위 조회를 프론트 Store에서 적용
  - 상세 페이지는 `/master/boms/:parentItemId?bomVersion=...` 라우트 사용 — BOM 그룹 상세가 URL로 직접 접근 가능하고 목록 클릭 시 라우팅되도록 구성
- **Affected Files**: <details><summary>16개 파일</summary>

  - **Created**:
    - `backend/src/main/java/com/ssafy/demo_app/api/bom/dto/BomGroupResponse.java` — BOM 그룹 목록 응답 DTO
    - `backend/src/main/java/com/ssafy/demo_app/api/bom/dto/BomBulkRequest.java` — BOM 일괄 등록 요청 DTO
    - `backend/src/main/java/com/ssafy/demo_app/api/bom/dto/BomBulkLineRequest.java` — BOM 일괄 등록 구성품 line 요청 DTO
    - `frontend/src/views/BomMasterDetailView.vue` — BOM 그룹 상세 페이지
  - **Modified**:
    - `agent/project/backend.md` — BOM 그룹 목록/상세/일괄 등록 API 문서 추가
    - `agent/project/frontend.md` — BOM 목록/상세 라우트 문서 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/bom/BomApi.java` — BOM 그룹 목록/상세 및 일괄 등록 엔드포인트 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/bom/BomController.java` — 신규 BOM 그룹/일괄 등록 API 연결
    - `backend/src/main/java/com/ssafy/demo_app/domain/bom/service/BomService.java` — BOM 그룹 페이지와 일괄 등록 서비스 계약 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/bom/service/BomServiceImpl.java` — 그룹 페이지 생성, 요청 내 중복 검증, 일괄 저장 구현
    - `backend/src/test/java/com/ssafy/demo_app/domain/bom/service/BomServiceTest.java` — 일괄 등록, 중복 line 차단, 10개 단위 그룹 페이지 테스트 추가
    - `frontend/src/api/bomMasterApi.ts` — BOM 그룹/일괄 등록 API 타입 및 호출 추가
    - `frontend/src/services/bomMasterService.ts` — BOM 그룹/일괄 등록 서비스 추가
    - `frontend/src/state/bomMasterStore.ts` — BOM 그룹 페이지 상태와 상세 조회/일괄 등록 액션 추가
    - `frontend/src/router/index.ts` — BOM 상세 라우트 추가
    - `frontend/src/views/BomMasterView.vue` — 그룹 목록 10개 페이지네이션, 상세 라우팅, 다건 구성품 등록 UI 적용
  - **Deleted**:
    - 없음

  </details>

### BOM 정수 수량 정책 반영 (Codex)
- **User Intent**: 기초 시스템 범위에서는 `kg`, `l`, `m`, `ea`, `box` 등 단위와 무관하게 재고, 주문, 작업지시, BOM 소요량을 모두 정수 단위로 통일하도록 요청
- **Agent Context**: 이전 변경에서 BOM 수량의 소수 계산을 보존하고 재고 불출 시 올림 처리했으나, 현재 정책은 소수 자체를 허용하지 않는 방향이므로 BOM 엔티티/DTO/응답/화면 입력/테스트를 모두 `Integer` 기준으로 재정렬
- **Key Decisions**:
  - BOM 소요량 타입을 `BigDecimal`에서 `Integer`로 변경 — 재고/작업지시/생산 실적 수량이 이미 정수 기반인 현재 도메인 모델과 일치시키기 위함
  - 생산 소요량 계산에서 올림 로직 제거 — 소수 결과가 발생하지 않도록 BOM 단위 소요량과 작업지시 수량을 정수 곱셈으로만 계산
  - 프론트 입력을 `min=1`, `step=1`로 제한 — 사용자가 BOM 등록 단계에서 소수 수량을 입력하지 못하도록 UI 정책을 API 정책과 일치
- **Affected Files**: <details><summary>15개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `agent/project/backend.md` — BOM 수량 정수 정책 문서화
    - `backend/src/main/java/com/ssafy/demo_app/domain/bom/entity/BomStructure.java` — BOM 소요량 필드를 `Integer`로 변경
    - `backend/src/main/java/com/ssafy/demo_app/api/bom/dto/BomRequest.java` — BOM 요청 수량을 정수 검증으로 변경
    - `backend/src/main/java/com/ssafy/demo_app/api/bom/dto/BomResponse.java` — BOM 응답 수량을 정수로 변경
    - `backend/src/main/java/com/ssafy/demo_app/api/bom/dto/BomReverseResponse.java` — 역전개 응답 수량을 정수로 변경
    - `backend/src/main/java/com/ssafy/demo_app/api/bom/dto/BomTreeResponse.java` — 정전개 트리 수량을 정수로 변경
    - `backend/src/main/java/com/ssafy/demo_app/api/bom/dto/BomReverseTreeResponse.java` — 역전개 트리 수량을 정수로 변경
    - `backend/src/main/java/com/ssafy/demo_app/api/production/dto/WorkOrderMaterialRequirementResponse.java` — 작업지시 자재 소요량 응답을 정수로 변경
    - `backend/src/main/java/com/ssafy/demo_app/domain/bom/service/BomRequirement.java` — BOM 소요량 계산 record를 정수로 변경
    - `backend/src/main/java/com/ssafy/demo_app/domain/bom/service/BomServiceImpl.java` — 재귀 소요량 누적과 필요 수량 계산을 정수 연산으로 변경
    - `backend/src/test/java/com/ssafy/demo_app/domain/bom/service/BomServiceTest.java` — BOM 테스트 데이터를 정수 수량으로 변경
    - `backend/src/test/java/com/ssafy/demo_app/domain/production/service/WorkOrderServiceTest.java` — 소수 올림 테스트를 정수 소요량 테스트로 변경
    - `frontend/src/views/BomMasterView.vue` — BOM 소요량 입력과 표시를 정수 기준으로 변경
    - `frontend/src/ui/BomTreeList.vue` — BOM 트리 수량 표시를 정수 기준으로 변경
    - `frontend/src/views/MaterialIssueView.vue` — 자재 불출 화면 BOM 소요량 표시를 정수 기준으로 변경
  - **Deleted**:
    - 없음

  </details>

### BOM 그룹 조회 정확성 및 검색 보강 (Codex)
- **User Intent**: BOM 그룹 상세가 parentItemId exact match가 아니라 keyword 검색으로 동작하여 다른 품목이 섞일 수 있고, BOM 그룹 목록이 전체 매칭 row를 메모리에 올린 뒤 수동 페이징하며, 품목 선택 검색이 100개 이후 후보를 누락하는 문제 수정을 요청.
- **Agent Context**: BOM 그룹 상세는 상위 품목 ID와 BOM 버전 전용 서비스 메서드로 분리하고, 그룹 목록은 Repository projection 기반 Page 조회로 이동했으며, 프론트 품목 검색은 첫 페이지 이후 전체 페이지를 추가 조회하도록 보강.
- **Key Decisions**:
  - 그룹 상세 조회를 `getBomGroup(parentItemId, bomVersion)`로 분리 — API 계층은 Domain Service를 통해 비즈니스 조회를 수행해야 한다는 backend 계층 규칙을 유지하면서 keyword contains 검색과 exact 조회 책임을 분리.
  - 그룹 목록을 `Page<BomGroupResponse>` JPQL projection으로 조회 — 전체 List 로딩 후 subList를 제거하고 DB count/page 응답을 `PageResponse`로 변환해 기존 페이징 응답 패턴을 재사용.
  - 품목 선택 검색은 Service 계층에서 추가 페이지를 병렬 조회 — frontend 계층 규칙에 맞춰 API는 단일 HTTP 호출만 담당하고, 응답 조합은 Service 계층에서 처리.
- **Affected Files**: <details><summary>8개 파일</summary>

  - **Created**:
    - `backend/src/main/java/com/ssafy/demo_app/api/bom/dto/BomGroupResponse.java` — BOM 그룹 목록 projection 응답 DTO
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/api/bom/BomController.java` (+32/-0) — 그룹 목록/상세 API를 서비스 메서드에 연결
    - `backend/src/main/java/com/ssafy/demo_app/domain/bom/repository/BomStructureRepository.java` (+54/-0) — DB 그룹 조회/count JPQL 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/bom/service/BomService.java` (+12/-0) — 그룹 목록/상세 서비스 계약 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/bom/service/BomServiceImpl.java` (+73/-15) — 그룹 목록 DB 페이징 및 exact 상세 조회 적용
    - `backend/src/test/java/com/ssafy/demo_app/domain/bom/service/BomServiceTest.java` (+112/-11) — 그룹 페이징 및 exact 상세 조회 회귀 테스트 추가
    - `frontend/src/api/bomMasterApi.ts` (+46/-3) — 그룹 API 타입과 품목 검색 page 파라미터 추가
    - `frontend/src/services/bomMasterService.ts` (+42/-2) — 품목 검색 결과 전체 페이지 조합 처리
  - **Deleted**:
    - 없음

  </details>
