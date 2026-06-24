- **작업 범위**:
  - 품목 마스터 카드의 활성/비활성 품목 수를 현재 페이지 기준이 아닌 전체 품목 기준으로 수정한다.
  - 대시보드 카드의 라인 생산량을 공정별 실적 합산이 아닌 최종 생산 입고 수량 기준으로 수정한다.
  - 대시보드 카드의 출하 대기를 출하 상태가 `READY`인 지시 건수 기준으로 수정한다.
  - 대시보드 우선 확인 인사이트에 사용자가 이해할 수 있는 지표명을 표시한다.
  - 현재고 모니터링 목록을 활성 상태 품목 마스터 전체 기준으로 표시하고, 현재고가 0인 품목도 포함한다.

- **현황 진단**:
  - `frontend/src/views/ItemMasterView.vue`는 `itemMasterStore.items`만 필터링해 활성/비활성 카드를 계산하고 있어 현재 페이지 데이터만 반영한다.
  - 거래처 마스터는 `GET /api/partners/stats`와 `partnerMasterStore.stats`를 통해 전체 기준 통계를 별도로 조회한다.
  - `backend/src/main/java/com/ssafy/demo_app/domain/dashboard/service/DashboardServiceImpl.java`는 라인별 생산 실적 행의 `totalQty`를 합산해 여러 공정을 통과한 같은 제품을 중복 계산한다.
  - 생산 최종 입고는 마지막 공정 실적 등록 시 `InventoryTransactionHistory`에 `PRODUCTION_RECEIPT`로 기록된다.
  - 출하 대기 카드는 기간 조건과 여러 출하 진행 상태를 함께 집계하고 있어, 화면 라벨인 `READY` 상태 출하 지시와 기준이 다르다.
  - `CurrentInventoryRepository.findInventorySummaries`는 품목 마스터를 기준으로 left join하므로 0개 품목 포함 구조는 갖춰져 있으나 활성 품목 조건이 없다.

- **구현 계획**:
  - 품목 통계 DTO와 `GET /api/items/stats` 엔드포인트를 추가한다.
  - `ItemMasterRepository`에 상태별 count 메서드를 추가하고, `ItemService`에서 전체/활성/비활성 통계를 반환한다.
  - 프론트엔드 품목 API, 서비스, 스토어에 통계 조회 흐름을 추가한다.
  - 품목 마스터 화면은 최초 진입과 신규 등록 후 목록/통계를 함께 갱신하고, 카드는 `itemMasterStore.stats`를 사용한다.
  - `InventoryTransactionHistoryRepository`에 선택 기간의 순 생산 입고 수량 합계 쿼리를 추가한다.
  - 생산 입고 취소는 음수 이력으로 저장되므로 `PRODUCTION_RECEIPT`와 `PRODUCTION_RECEIPT_CANCEL`을 합산해 순 최종 생산량을 계산한다.
  - `OutboundShippingRepository`에 `READY` 상태 카운트 메서드를 추가하고 대시보드 출하 대기 카드에서 사용한다.
  - 대시보드 인사이트의 원천 테이블명은 프론트엔드에서 한국어 지표명으로 변환해 표시한다.
  - 현재고 요약 SQL에 `i.item_status = 'ACTIVE'` 조건을 추가해 활성 품목만 조회한다.

- **검증 계획**:
  - 백엔드 `compileJava`로 API/서비스/쿼리 컴파일 정합성을 확인한다.
  - 프론트엔드 `npm run build`로 타입과 번들 정합성을 확인한다.
  - 변경 후 작업 히스토리 로그를 기록하고 커밋 대상에 포함한다.
