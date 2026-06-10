
### 품목 마스터 관리 고도화 (Codex)
- **User Intent**: 품목 마스터 기준관리 화면과 API에서 검색 선택 흐름, 선택 품목코드 등록, ITEM-____ 자동 채번, 코드 수정 금지, 안전한 삭제/비활성화, 페이지네이션, 상태 관리, 활용 정보, 중복 품목 경고, 목록/상세 페이지 분리를 구현하도록 요청.
- **Agent Context**: 기존 품목 마스터는 리스트 전체 조회, 분류 기반 코드 재생성, force 삭제, 단일 화면 상세 패널 구조로 구현되어 있어 기준정보 안정성과 대량 목록 UX가 부족했다. 백엔드 계약을 페이징 기반으로 확장하고, 프론트는 목록/상세 라우트로 분리해 조회와 운영 액션을 분리했다.
- **Key Decisions**:
  - 신규 품목 자동 코드는 `ITEM-____` 형식으로만 생성하고 기존 코드는 마이그레이션하지 않음 — 기존 업무 문서와 참조 데이터의 식별 안정성을 유지하기 위한 결정.
  - 품목 수정 DTO에서 `itemCode`를 제외함 — 품목 코드는 업무 식별자로 사용되므로 수정 중 코드 변경으로 BOM, 입고, 출고, 작업지시 해석이 흔들리지 않게 하기 위한 결정.
  - 참조 중 품목 삭제는 차단하고 상태 변경을 제공함 — 기준정보는 다른 도메인에서 광범위하게 참조되므로 물리 삭제보다 비활성화가 안전하다는 도메인 판단.
  - 목록 API는 `Pageable`과 `Specification`으로 전환함 — `agent/project/backend.md`의 JPA 계층 구조와 기존 페이징 응답 패턴을 따르면서 검색, 상태 필터, 정렬을 서버에서 처리하기 위한 결정.
  - 프론트는 `View → Store → Service → API` 흐름을 유지함 — `agent/project/frontend.md`의 계층 아키텍처를 지키기 위해 화면에서 HTTP 호출을 직접 수행하지 않도록 구성.
- **Affected Files**: <details><summary>33개 파일</summary>

  - **Created**:
    - `backend/src/main/java/com/ssafy/demo_app/api/item/dto/ItemDuplicateCheckResponse.java` — 중복 품목 검증 응답 DTO 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/item/dto/ItemReferenceResponse.java` — 품목 참조 현황 응답 DTO 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/item/dto/ItemStatusUpdateRequest.java` — 품목 활성/비활성 상태 변경 요청 DTO 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/item/dto/ItemUpdateRequest.java` — 품목 코드 수정 금지를 위한 수정 전용 DTO 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/item/dto/ItemUsageResponse.java` — 품목 활용 정보 응답 DTO 추가
    - `frontend/src/views/ItemMasterDetailView.vue` — 품목 상세 전용 화면 추가
    - `agent/history/sseoyeon-ssonia/fix-itemMaster.md` — 작업 히스토리 로그 추가
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/api/item/ItemApi.java` (+45/-11) — 페이징 목록, 중복 검증, 참조 현황, 활용 정보, 상태 변경 API 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/item/ItemController.java` (+42/-7) — 품목 API 신규 서비스 호출 연결
    - `backend/src/main/java/com/ssafy/demo_app/api/item/dto/ItemRequest.java` (+6/-0) — 등록 시 선택 품목코드 입력 필드 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/item/dto/ItemResponse.java` (+2/-0) — 품목 상태 응답 필드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/bom/repository/BomStructureRepository.java` (+2/-0) — BOM 참조 수 조회 메서드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/repository/CurrentInventoryRepository.java` (+1/-0) — 현재고 참조 수 조회 메서드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/repository/InboundReceiptRepository.java` (+1/-0) — 입고 참조 수 조회 메서드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/repository/InventoryTransactionHistoryRepository.java` (+2/-0) — 수불 참조 수와 최근 이력 조회 메서드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/item/entity/ItemMaster.java` (+9/-0) — 품목 상태 enum/필드와 DB 기본값 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/item/repository/ItemMasterRepository.java` (+7/-2) — Specification과 코드/중복 조회 메서드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/item/service/ItemService.java` (+23/-5) — 품목 서비스 계약 확장
    - `backend/src/main/java/com/ssafy/demo_app/domain/item/service/ItemServiceImpl.java` (+178/-78) — ITEM 자동 채번, 선택 코드 검증, 페이징 검색, 상태 변경, 참조/활용/중복 검증 구현
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/repository/WorkOrderRepository.java` (+3/-0) — 작업지시 참조 수와 사용처 조회 메서드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/shipping/repository/OutboundShippingRepository.java` (+1/-0) — 출하 참조 수 조회 메서드 추가
    - `backend/src/main/java/com/ssafy/demo_app/global/exception/ErrorCode.java` (+3/-1) — 품목 코드 형식, 참조 삭제, 참조 분류 변경 오류 추가
    - `backend/src/main/java/com/ssafy/demo_app/infrastructure/security/config/SecurityConfig.java` (+5/-0) — 품목 등록/수정/삭제 권한 정책 명시
    - `frontend/src/api/bomMasterApi.ts` (+3/-2) — 품목 목록 페이징 응답 대응
    - `frontend/src/api/inboundApi.ts` (+3/-2) — 품목 목록 페이징 응답 대응
    - `frontend/src/api/itemMasterApi.ts` (+90/-8) — 품목 마스터 API 타입과 신규 엔드포인트 추가
    - `frontend/src/router/index.ts` (+5/-0) — 품목 상세 라우트 추가
    - `frontend/src/services/bomMasterService.ts` (+1/-1) — 품목 목록 content 반환 처리
    - `frontend/src/services/inboundService.ts` (+1/-1) — 품목 목록 content 반환 처리
    - `frontend/src/services/itemMasterService.ts` (+48/-5) — 품목 신규 API 서비스 래핑 추가
    - `frontend/src/state/itemMasterStore.ts` (+82/-12) — 페이징/상세/참조/활용/중복 상태와 액션 추가
    - `frontend/src/views/ItemMasterView.vue` (+256/-1059) — 목록 전용 화면, 검색, 정렬, 페이지네이션, 등록/중복 경고 UX로 재구성
    - `frontend/src/views/WorkOrderView.vue` (+3/-3) — 품목 마스터 페이징 응답 대응
  - **Deleted**:
    - 없음

  </details>

### 품목 마스터 UX 추가 보강 (Codex)
- **User Intent**: 품목 마스터 목록 문구, 검색 자동완성, 상세 페이지 목록 이동 버튼, 수정 모달 품목코드 표현, 참조 중 품목 안내, ADMIN 강제 삭제 2단계 확인을 추가로 보강 요청.
- **Agent Context**: 직전 구현은 목록/상세 분리와 안전 삭제 흐름을 제공했지만, 검색 후보 선택과 상세 모달의 참조 데이터 가시성이 부족했다. 기존 API 계약에 force 삭제를 ADMIN 전용 보안 정책 아래 복원하고 프론트에서 참조 리스트와 2단계 확인을 제공했다.
- **Key Decisions**:
  - 검색 후보는 품목 Store 액션으로 조회함 — `agent/project/frontend.md`의 View → Store → Service → API 계층 흐름을 유지하기 위한 결정.
  - 강제 삭제는 `DELETE /api/items/{id}?force=true`로 복원함 — 사용자의 ADMIN 강제 삭제 요구를 반영하되 기존 SecurityConfig의 ADMIN DELETE 제한으로 접근 범위를 제어하기 위한 결정.
  - 참조 중 분류 변경/삭제 모달에는 참조 항목 리스트만 표시함 — 사용자가 위험도를 판단하는 데 필요한 정보를 직접 제공하고 불필요한 안내 문구를 줄이기 위한 결정.
- **Affected Files**: <details><summary>8개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/api/item/ItemApi.java` — force 삭제 파라미터 명세 복원
    - `backend/src/main/java/com/ssafy/demo_app/api/item/ItemController.java` — force 삭제 서비스 호출 연결
    - `backend/src/main/java/com/ssafy/demo_app/domain/item/service/ItemService.java` — force 삭제 계약 반영
    - `backend/src/main/java/com/ssafy/demo_app/domain/item/service/ItemServiceImpl.java` — ADMIN 강제 삭제 시 참조 데이터 삭제 처리 복원
    - `backend/src/main/java/com/ssafy/demo_app/global/exception/ErrorCode.java` — 참조 품목 삭제 메시지 조정
    - `frontend/src/api/itemMasterApi.ts` — force 삭제 파라미터 추가
    - `frontend/src/services/itemMasterService.ts` — force 삭제 서비스 파라미터 추가
    - `frontend/src/state/itemMasterStore.ts` — 검색 후보 상태/액션과 force 삭제 액션 추가
    - `frontend/src/views/ItemMasterView.vue` — 활성/비활성 문구 변경 및 검색 후보 드롭다운 추가
    - `frontend/src/views/ItemMasterDetailView.vue` — 목록 버튼 강조, 품목 코드 표시 개선, 참조 리스트, ADMIN 강제 삭제 2단계 확인 추가
  - **Deleted**:
    - 없음

  </details>

### 품목 상세 목록 버튼 디자인 보정 (Codex)
- **User Intent**: 품목 상세 화면의 `목록으로` 버튼 디자인이 어색하여, 첨부 이미지의 둥근 흰색 버튼과 하단 검정 shadow 스타일로 변경 요청.
- **Agent Context**: 기존 버튼은 우하단 offset shadow가 강해 첨부 이미지의 하단 받침형 버튼과 시각 방향이 달랐다. 단일 Vue 템플릿 클래스만 수정해 버튼 높이, 반경, border, 하단 shadow, active 눌림 효과를 조정했다.
- **Key Decisions**:
  - 컴포넌트 구조 변경 없이 Tailwind utility class만 수정함 — `agent/project/frontend.md`의 기존 Vue SFC 구조를 유지하며 변경 범위를 최소화하기 위한 결정.
  - 하단 shadow와 active 눌림 효과를 함께 적용함 — 두 번째 첨부 이미지의 물리 버튼 느낌을 재현하면서 인터랙션 피드백을 명확히 하기 위한 결정.
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/ItemMasterDetailView.vue` — `목록으로` 버튼 스타일을 pill 형태와 하단 shadow 디자인으로 변경
  - **Deleted**:
    - 없음

  </details>

### 품목 상세 목록 버튼 shadow 제거 (Codex)
- **User Intent**: 품목 상세 화면의 `목록으로` 버튼에서 shadow를 완전히 제거하고, 첨부 이미지처럼 흰 배경과 검정 테두리 중심의 버튼으로 변경 요청.
- **Agent Context**: 직전 버튼은 하단 shadow와 active 이동 효과가 남아 있어 사용자 의도와 달랐다. 버튼 class에서 shadow 및 translate 기반 눌림 효과를 제거하고 border, radius, hover 배경만 유지했다.
- **Key Decisions**:
  - Shadow 관련 Tailwind class를 전부 제거함 — 사용자가 명시적으로 shadow 미적용을 요구했으므로 시각 효과를 단순화하기 위한 결정.
  - Active 상태는 배경색 변화만 유지함 — 버튼 상호작용 피드백은 남기되 위치 이동이나 그림자 변화는 발생하지 않게 하기 위한 결정.
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/ItemMasterDetailView.vue` — `목록으로` 버튼 shadow/translate 효과 제거
  - **Deleted**:
    - 없음

  </details>

### 품목 상세 목록 이동 연결 복구 (Codex)
- **User Intent**: 품목 상세 화면의 `목록으로` 버튼을 눌러도 품목 목록으로 이동하지 않는 문제를 수정 요청.
- **Agent Context**: 상세 화면은 별도 라우트(`/master/items/:id`)로 분리되어 있으나 버튼 클릭이 라우터 이동이 아니라 로컬 `pageMode` 상태 변경만 수행하고 있어 실제 URL 이동이 발생하지 않았다. 버튼 클릭 핸들러를 `router.push({ name: 'item-master' })`로 복구했다.
- **Key Decisions**:
  - 로컬 상태 전환이 아닌 Vue Router 이동을 사용함 — 목록/상세가 별도 라우트로 분리된 현재 구조에서 실제 페이지 이동을 보장하기 위한 결정.
  - 버튼 UI와 아이콘은 유지하고 클릭 동작만 수정함 — 사용자 요청 범위를 목록 이동 연결 복구로 제한하기 위한 결정.
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/ItemMasterDetailView.vue` — `목록으로` 버튼 클릭 핸들러를 목록 라우트 이동으로 복구
  - **Deleted**:
    - 없음

  </details>

### 품목 검색 자동완성 후보 보강 (Codex)
- **User Intent**: 품목 마스터 검색어 입력 시 키워드가 겹치는 품목 후보가 첨부 이미지처럼 입력창 아래에 표시되도록 수정 요청.
- **Agent Context**: 기존 자동완성은 코드/품목명/규격 검색에만 의존해 `half`처럼 품목 분류 키워드와 겹치는 경우 후보가 충분히 표시되지 않을 수 있었다. 백엔드 검색 대상에 품목 분류와 단위를 추가하고, 프론트 후보 리스트를 검색 아이콘/포커스 링/메타 정보 구조로 정리했다.
- **Key Decisions**:
  - 백엔드 Specification 검색 범위에 `itemType`, `unit`을 포함함 — 후보 검색과 목록 검색이 동일한 서버 기준으로 동작하게 하기 위한 결정.
  - 후보 UI는 코드·품목명 1행과 분류·단위·안전재고 2행으로 구성함 — 첨부 이미지와 동일한 정보 밀도로 사용자가 빠르게 식별할 수 있게 하기 위한 결정.
- **Affected Files**: <details><summary>2개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/item/service/ItemServiceImpl.java` — 품목 검색 조건에 분류/단위 매칭 추가
    - `frontend/src/views/ItemMasterView.vue` — 검색 입력창 아이콘, 포커스 스타일, 후보 리스트 UI/메타 정보 표시 추가
  - **Deleted**:
    - 없음

  </details>

## 2026-06-10 16:53:11 KST - 품목 검색 자동완성 표시 복구

- 품목 마스터 검색 입력 시 드롭다운이 API 결과에만 의존해 표시되지 않을 수 있던 문제를 수정했다.
- 현재 목록 데이터와 서버 자동완성 검색 결과를 병합해 키워드가 겹치는 품목 후보를 즉시 표시하도록 변경했다.
- 자동완성 후보 매칭 기준에 품목 ID, 품목 코드, 품목명, 규격, 분류 코드, 분류 한글명, 단위를 포함했다.
- 후보 선택 이벤트를 `click`에서 `mousedown.prevent`로 바꿔 입력 포커스 변화와 무관하게 선택 동작이 안정적으로 실행되도록 했다.
- 검증: `frontend`에서 `npm run build` 통과.

## 2026-06-10 17:16:46 KST - 품목 강제 삭제 제거 및 비활성화 유도

- 품목 삭제 정책에서 강제 삭제를 제거하고, 참조 중인 품목은 항상 삭제할 수 없도록 백엔드 서비스를 수정했다.
- 품목 삭제 API 설명과 서비스 인터페이스에서 `force` 파라미터를 제거했다.
- 품목 상세 삭제 확인 모달에서 강제 삭제 확인/정말 강제 삭제 버튼을 제거했다.
- 참조 중인 품목의 삭제 확인 모달에는 비활성화 버튼만 표시되도록 변경했다.
- 참조가 없는 품목에 대해서만 삭제 버튼이 표시되고 삭제 API를 호출하도록 변경했다.
- 참조 중인 품목 삭제 에러 문구를 강제 삭제 안내에서 비활성화 안내로 변경했다.
- 검증: `frontend`에서 `npm run build` 통과, `backend`에서 `./gradlew test` 통과.

## 2026-06-10 17:22:36 KST - 품목 삭제 확인창 추가

- 참조가 없는 품목 삭제 실행 전에 브라우저 기본 확인창을 한 번 더 표시하도록 수정했다.
- 삭제 확인 문구는 `품목을 삭제하시겠습니까?`로 적용했다.
- 사용자가 확인창에서 취소하면 삭제 API를 호출하지 않도록 처리했다.
- 검증: `frontend`에서 `npm run build` 통과.
