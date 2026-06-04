# Backend Entities

## 엔티티-테이블 매핑

| Entity | Table | PK | Base Class |
|--------|-------|-----|------------|
| `User` | `users` | `user_id` (auto) | `BaseTimeEntity` |
| `ItemMaster` | `item_master` | `item_id` (auto) | `BaseCreatedTimeEntity` |
| `PartnerMaster` | `partner_master` | `partner_id` (auto) | `BaseCreatedTimeEntity` |
| `BomStructure` | `bom_structure` | `bom_id` (auto) | `BaseCreatedTimeEntity` |
| `FactoryRouting` | `factory_routing` | `routing_id` (auto) | `BaseCreatedTimeEntity` |
| `WorkOrder` | `work_order` | `order_id` (auto) | `BaseTimeEntity` |
| `ProductionExecution` | `production_execution` | 실행 ID (auto) | (확인 필요) |
| `InboundReceipt` | `inbound_receipt` | `inbound_id` (auto) | `BaseCreatedTimeEntity` |
| `CurrentInventory` | `current_inventory` | 재고 ID (auto) | (확인 필요) |
| `OutboundShipping` | `outbound_shipping` | `shipping_id` (auto) | `BaseCreatedTimeEntity` |
| `WarehouseLocation` | `warehouse_location` | `location_id` (auto) | `BaseCreatedTimeEntity` |
| `InventoryTransactionHistory` | `inventory_transaction_history` | 이력 ID (auto) | (확인 필요) |
| `AiQueryHistory` | (스켈레톤) | — | — |
| `DynamicBatchSchedule` | (스켈레톤) | — | — |

## 엔티티 상세

### User (`users`)

| 컬럼 | 타입 | 제약 |
|------|------|------|
| `user_id` | INT | PK, AUTO_INCREMENT |
| `employee_no` | VARCHAR | UNIQUE, NOT NULL |
| `user_name` | VARCHAR | NOT NULL |
| `department` | VARCHAR | NOT NULL |
| `password` | VARCHAR | NOT NULL |
| `role` | ENUM | `WORKER`, `MANAGER`, `ADMIN` |
| `created_at` | DATETIME | `BaseTimeEntity` |
| `updated_at` | DATETIME | `BaseTimeEntity` |

### ItemMaster (`item_master`)

| 컬럼 | 타입 | 제약 |
|------|------|------|
| `item_id` | INT | PK, AUTO_INCREMENT |
| `item_code` | VARCHAR | UNIQUE, NOT NULL |
| `item_name` | VARCHAR | NOT NULL |
| `spec` | VARCHAR | nullable |
| `unit` | ENUM | `ea`, `kg`, `box`, `L` |
| `item_type` | ENUM | `RAW`, `HALF`, `FG` |
| `safety_stock` | INT | NOT NULL, default 0 |
| `created_at` | DATETIME | `BaseCreatedTimeEntity` |

### PartnerMaster (`partner_master`)

| 컬럼 | 타입 | 제약 |
|------|------|------|
| `partner_id` | INT | PK, AUTO_INCREMENT |
| `partner_code` | VARCHAR | UNIQUE, NOT NULL |
| `partner_name` | VARCHAR | NOT NULL |
| `partner_type` | ENUM | `SUPPLIER`, `CUSTOMER` |
| `business_no` | VARCHAR | 사업자번호 |
| `representative` | VARCHAR | 대표자명 |
| `contact_phone` | VARCHAR | 연락처 |
| `created_at` | DATETIME | `BaseCreatedTimeEntity` |

### BomStructure (`bom_structure`)

| 컬럼 | 타입 | 제약 |
|------|------|------|
| `bom_id` | INT | PK, AUTO_INCREMENT |
| `parent_item_id` | INT | FK → item_master, NOT NULL |
| `child_item_id` | INT | FK → item_master, NOT NULL |
| `quantity` | DECIMAL(10,4) | NOT NULL |
| `bom_version` | VARCHAR | NOT NULL, default `v1.0` |
| UNIQUE | | (`parent_item_id`, `child_item_id`, `bom_version`) |
| `created_at` | DATETIME | `BaseCreatedTimeEntity` |

### FactoryRouting (`factory_routing`)

| 컬럼 | 타입 | 제약 |
|------|------|------|
| `routing_id` | INT | PK, AUTO_INCREMENT |
| `factory_name` | VARCHAR | NOT NULL |
| `line_name` | VARCHAR | NOT NULL |
| `operation_seq` | INT | NOT NULL |
| `operation_name` | VARCHAR | NOT NULL |
| UNIQUE | | (`factory_name`, `line_name`, `operation_seq`) |
| `created_at` | DATETIME | `BaseCreatedTimeEntity` |

### WorkOrder (`work_order`)

| 컬럼 | 타입 | 제약 |
|------|------|------|
| `order_id` | INT | PK, AUTO_INCREMENT |
| `order_no` | VARCHAR | UNIQUE, NOT NULL |
| `item_id` | INT | FK → item_master, NOT NULL |
| `routing_id` | INT | FK → factory_routing, NOT NULL |
| `target_qty` | INT | NOT NULL |
| `status` | ENUM | `READY`, `RUN`, `HOLD`, `CLOSE` |
| `plan_date` | DATE | NOT NULL |
| `created_at` | DATETIME | `BaseTimeEntity` |
| `updated_at` | DATETIME | `BaseTimeEntity` |

### InboundReceipt (`inbound_receipt`)

| 컬럼 | 타입 | 제약 |
|------|------|------|
| `inbound_id` | INT | PK, AUTO_INCREMENT |
| `item_id` | INT | FK → item_master, NOT NULL |
| `partner_id` | INT | FK → partner_master, NOT NULL |
| `location_id` | INT | FK → warehouse_location, NOT NULL |
| `inbound_qty` | INT | NOT NULL |
| `inbound_date` | DATE | NOT NULL |
| `worker_id` | INT | FK → users, nullable |
| `status` | ENUM | `READY`, `COMPLETED` |
| `created_at` | DATETIME | `BaseCreatedTimeEntity` |

### OutboundShipping (`outbound_shipping`)

| 컬럼 | 타입 | 제약 |
|------|------|------|
| `shipping_id` | INT | PK, AUTO_INCREMENT |
| `shipping_no` | VARCHAR | UNIQUE, NOT NULL |
| `partner_id` | INT | FK → partner_master, NOT NULL |
| `item_id` | INT | FK → item_master, NOT NULL |
| `request_qty` | INT | NOT NULL |
| `picking_location_id` | INT | FK → warehouse_location, nullable |
| `vehicle_no` | VARCHAR | nullable |
| `status` | ENUM | `READY`, `PICKING`, `SHIPPED` |
| `worker_id` | INT | FK → users, nullable |
| `shipped_at` | DATETIME | nullable |
| `created_at` | DATETIME | `BaseCreatedTimeEntity` |

### WarehouseLocation (`warehouse_location`)

| 컬럼 | 타입 | 제약 |
|------|------|------|
| `location_id` | INT | PK, AUTO_INCREMENT |
| `location_code` | VARCHAR | UNIQUE, NOT NULL |
| `warehouse_name` | VARCHAR | NOT NULL |
| `rack_row` | VARCHAR | NOT NULL |
| `rack_column` | VARCHAR | NOT NULL |
| `created_at` | DATETIME | `BaseCreatedTimeEntity` |

### CurrentInventory (`current_inventory`)

| 컬럼 | 타입 | 제약 |
|------|------|------|
| PK | INT | AUTO_INCREMENT |
| `item_id` | INT | FK → item_master |
| `location_id` | INT | FK → warehouse_location |
| `current_qty` | INT | NOT NULL |

### InventoryTransactionHistory (`inventory_transaction_history`)

| 컬럼 | 타입 | 제약 |
|------|------|------|
| PK | INT | AUTO_INCREMENT |
| `item_id` | INT | FK → item_master |
| `location_id` | INT | FK → warehouse_location |
| `transaction_type` | ENUM | `INBOUND`, `OUTBOUND` |
| `quantity` | INT | NOT NULL |
| `reason_desc` | VARCHAR | 사유 |
| `worker_id` | INT | FK → users |

### ProductionExecution (`production_execution`)

| 컬럼 | 타입 | 제약 |
|------|------|------|
| PK | INT | AUTO_INCREMENT |
| `work_order_id` | INT | FK → work_order |
| `executed_qty` | INT | 실행 수량 |
| `defect_qty` | INT | 불량 수량 (추정) |
| `execution_date` | DATE | 실적 일자 |

## 엔티티 관계도

```
User ──┬── InboundReceipt (worker)
       ├── OutboundShipping (worker)
       └── InventoryTransactionHistory (worker)

ItemMaster ──┬── BomStructure (parent/child)
              ├── InboundReceipt
              ├── OutboundShipping
              ├── WorkOrder
              ├── CurrentInventory
              └── InventoryTransactionHistory

PartnerMaster ──┬── InboundReceipt
                └── OutboundShipping

FactoryRouting ─── WorkOrder ─── ProductionExecution

WarehouseLocation ──┬── InboundReceipt
                    ├── OutboundShipping (picking)
                    ├── CurrentInventory
                    └── InventoryTransactionHistory
```

## 주의사항

- 테이블 간 연관관계는 모두 `@ManyToOne` + `FetchType.LAZY`
- DB 마이그레이션 도구(Flyway/Liquibase)를 사용하지 않으므로, 엔티티 변경 시 테이블 스키마와 정합성을 수동 확인해야 함
- `AiQueryHistory`, `DynamicBatchSchedule` 엔티티는 정의되어 있으나 아직 구현되지 않음
