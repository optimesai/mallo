# 상차 완료 및 출하 확정 오류 수정 기획서

- **배경**
  - 피킹/상차 화면에서 차량 번호판을 입력하고 `배정 및 피킹 지시 실행` 버튼을 누르면 출하 지시 상태가 `PICKING`으로 변경된다.
  - 같은 단계에서 완제품 재고가 이미 차감된다.
  - 이후 출하 지시 화면 또는 피킹/상차 화면에서 `최종 출하 완료 처리` 버튼을 누르면 서버 오류가 발생한다.
  - 서버 오류로 인해 출하 건이 `출하 대기` 또는 `출하 완료` 상태로 정상 반영되지 않는다.

- **현상**
  - `assignPicking()`은 피킹 배정 시 FIFO 재고를 차감하고 `shippedQty`를 `0`으로 저장한다.
  - `completeShipping()`은 `shippedQty`가 `null`인 경우에만 요청 수량으로 보정한다.
  - 피킹 배정 API를 거친 정상 흐름에서는 `shippedQty`가 `0`이므로, 최종 완료 시 전체 출하가 아닌 부분 출하로 판정될 수 있다.
  - 최종 완료 단계에서 재고 차감이 아니라 상태 확정과 출하 이력 기록만 수행해야 하는 정책과 코드가 불일치한다.

- **목표**
  - `PICKING`, `PACKING`, `INSPECTING` 상태의 출하 건은 최종 완료 처리 시 요청 수량 전체를 출하 완료 수량으로 확정한다.
  - 피킹 단계에서 이미 차감된 재고를 완료 단계에서 다시 차감하지 않는다.
  - 최종 완료 시 상태를 `SHIPPED`로 변경하고 `shippedAt`, `worker`를 기록한다.
  - 피킹 배정 후 최종 완료까지의 실제 사용자 흐름을 테스트로 보강한다.

- **비목표**
  - 피킹 배정 단계의 FIFO 재고 차감 정책은 변경하지 않는다.
  - 출하 지시 등록, 수정, 취소 API 스키마는 변경하지 않는다.
  - 부분 출하 API의 상태 전이 정책은 이번 범위에서 변경하지 않는다.

- **구현 범위**
  - `backend/src/main/java/com/ssafy/demo_app/domain/shipping/service/OutboundShippingServiceImpl.java`
    - `completeShipping()`에서 최종 완료 수량을 `requestQty`로 확정한다.
    - 완료 상태는 전체 출하 완료를 의미하는 `SHIPPED`로 저장한다.
    - 완료 이력의 수량은 확정된 요청 수량을 사용한다.
  - `backend/src/test/java/com/ssafy/demo_app/domain/shipping/service/OutboundShippingServiceTest.java`
    - `assignPicking()` 이후 `completeShipping()`을 호출하는 통합 흐름 테스트를 추가한다.
    - 피킹 단계에서 차감된 재고가 완료 단계에서 추가 차감되지 않는지 검증한다.
    - 완료 후 상태, 출하 수량, 완료 일시가 정상 반영되는지 검증한다.

- **검증 계획**
  - `./gradlew test --tests com.ssafy.demo_app.domain.shipping.service.OutboundShippingServiceTest`를 실행한다.
  - 기존 출하 완료 성공/실패 테스트가 계속 통과하는지 확인한다.
  - 피킹 배정 후 완료 테스트에서 상태가 `SHIPPED`, 출하 수량이 요청 수량, 현재고가 피킹 차감분만 반영되는지 확인한다.
