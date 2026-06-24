-- ===================================================================
-- 0. 스키마 보정 (SCHEMA PATCH)
-- ===================================================================
ALTER TABLE ai_query_history
    MODIFY COLUMN execution_status VARCHAR(50) NOT NULL;

-- ===================================================================
-- 1. 회원 도메인 (USER / AUTH)
-- ===================================================================
INSERT IGNORE INTO users (employee_no, user_name, department, password, role, created_at, updated_at)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 50
)
SELECT
    CASE WHEN n = 1 THEN 'user001' ELSE CONCAT('user', LPAD(n, 3, '0')) END,
    CASE WHEN n = 1 THEN '관리자' ELSE CONCAT('사용자', LPAD(n, 3, '0')) END,
    ELT(1 + MOD(n - 1, 5), '생산관리팀', '자재관리팀', '품질관리팀', '물류관리팀', '설비관리팀'),
    CASE WHEN n = 1 THEN '$2y$10$yRO8UxkCZtLXfB9xpHHrdeKwsNfOFiMaC0kfHWgkZeQFNr6BeWenK' ELSE '$2y$10$yRO8UxkCZtLXfB9xpHHrdeKwsNfOFiMaC0kfHWgkZeQFNr6BeWenK' END,
    CASE WHEN n = 1 THEN 'ADMIN' WHEN MOD(n, 5) = 0 THEN 'MANAGER' ELSE 'WORKER' END,
    TIMESTAMP('2026-01-01 08:00:00') + INTERVAL n DAY,
    TIMESTAMP('2026-01-01 08:00:00') + INTERVAL n DAY
FROM seq;

INSERT IGNORE INTO refresh_tokens (user_id, token_hash, expires_at, revoked, created_at, updated_at)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 50
)
SELECT
    u.user_id,
    SHA2(CONCAT('refresh-token-', LPAD(n, 3, '0')), 256),
    TIMESTAMP('2026-06-24 23:59:59') + INTERVAL n DAY,
    CASE WHEN MOD(n, 10) = 0 THEN 1 ELSE 0 END,
    TIMESTAMP('2026-01-02 09:00:00') + INTERVAL n DAY,
    TIMESTAMP('2026-01-02 09:00:00') + INTERVAL n DAY
FROM seq
JOIN users u ON u.employee_no = CASE WHEN n = 1 THEN 'user001' ELSE CONCAT('user', LPAD(n, 3, '0')) END;

-- ===================================================================
-- 2. 기준정보 도메인 (MASTER DATA)
-- ===================================================================
INSERT IGNORE INTO item_master (item_code, item_name, spec, unit, item_type, safety_stock, item_status, created_at)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 50
)
SELECT
    CASE
        WHEN n <= 20 THEN CONCAT('RM-', LPAD(n, 3, '0'))
        WHEN n <= 35 THEN CONCAT('SM-', LPAD(n - 20, 3, '0'))
        ELSE CONCAT('FG-', LPAD(n - 35, 3, '0'))
    END,
    CASE
        WHEN n <= 20 THEN CONCAT('원자재 ', LPAD(n, 3, '0'))
        WHEN n <= 35 THEN CONCAT('반제품 ', LPAD(n - 20, 3, '0'))
        ELSE CONCAT('완제품 ', LPAD(n - 35, 3, '0'))
    END,
    CONCAT(ELT(1 + MOD(n - 1, 5), 'STD', 'PRO', 'ECO', 'HEAVY', 'LIGHT'), '-', LPAD(n, 3, '0')),
    ELT(1 + MOD(n - 1, 4), 'ea', 'kg', 'box', 'L'),
    CASE WHEN n <= 20 THEN 'RAW' WHEN n <= 35 THEN 'HALF' ELSE 'FG' END,
    50 + (n * 10),
    CASE WHEN MOD(n, 17) = 0 THEN 'INACTIVE' ELSE 'ACTIVE' END,
    TIMESTAMP('2026-01-03 08:00:00') + INTERVAL n DAY
FROM seq;

INSERT IGNORE INTO partner_master (
    partner_code, partner_name, partner_type, business_no, representative,
    contact_phone, contact_email, note, partner_status, created_at
)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 50
)
SELECT
    CASE WHEN n <= 25 THEN CONCAT('SUP-', LPAD(n, 3, '0')) ELSE CONCAT('CUS-', LPAD(n - 25, 3, '0')) END,
    CASE WHEN n <= 25 THEN CONCAT('공급사 ', LPAD(n, 3, '0')) ELSE CONCAT('고객사 ', LPAD(n - 25, 3, '0')) END,
    CASE WHEN n <= 25 THEN 'SUPPLIER' ELSE 'CUSTOMER' END,
    CONCAT('2026-', LPAD(n, 2, '0'), '-', LPAD(10000 + n, 5, '0')),
    CONCAT('대표자', LPAD(n, 3, '0')),
    CONCAT('02-26', LPAD(n, 2, '0'), '-', LPAD(1000 + n, 4, '0')),
    CONCAT('partner', LPAD(n, 3, '0'), '@example.com'),
    CONCAT('2026년 더미 거래처 ', LPAD(n, 3, '0')),
    CASE WHEN MOD(n, 19) = 0 THEN 'INACTIVE' ELSE 'ACTIVE' END,
    TIMESTAMP('2026-01-04 08:00:00') + INTERVAL n DAY
FROM seq;

INSERT IGNORE INTO warehouse_location (
    location_code, warehouse_name, max_capacity, rack_row, rack_column, production_receipt_default
)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 50
)
SELECT
    CONCAT('WH', LPAD(1 + MOD(n - 1, 5), 2, '0'), '-R', CHAR(65 + MOD(n - 1, 10)), '-C', LPAD(1 + FLOOR((n - 1) / 10), 2, '0')),
    ELT(1 + MOD(n - 1, 5), '원자재 창고', '반제품 창고', '완제품 창고', '검사 대기 창고', '출하 대기 창고'),
    1000 + (n * 100),
    CHAR(65 + MOD(n - 1, 10)),
    LPAD(1 + FLOOR((n - 1) / 10), 2, '0'),
    CASE WHEN MOD(n, 10) = 0 THEN 1 ELSE 0 END
FROM seq;

INSERT IGNORE INTO factory_routing (factory_name, line_name, operation_seq, operation_name, routing_status, created_at)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 50
)
SELECT
    CONCAT('창원제', 1 + FLOOR((n - 1) / 25), '공장'),
    CONCAT(CHAR(65 + MOD(FLOOR((n - 1) / 5), 5)), '라인'),
    1 + MOD(n - 1, 5),
    ELT(1 + MOD(n - 1, 5), '자재 투입', '가공', '조립', '검사', '포장'),
    CASE WHEN MOD(n, 23) = 0 THEN 'INACTIVE' ELSE 'ACTIVE' END,
    TIMESTAMP('2026-01-05 08:00:00') + INTERVAL n DAY
FROM seq;

-- ===================================================================
-- 3. BOM 도메인
-- ===================================================================
INSERT IGNORE INTO bom_structure (parent_item_id, child_item_id, quantity, bom_version, bom_status, created_at)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 50
)
SELECT
    parent_item.item_id,
    child_item.item_id,
    1 + MOD(n, 8),
    'v1.0',
    CASE WHEN MOD(n, 29) = 0 THEN 'INACTIVE' ELSE 'ACTIVE' END,
    TIMESTAMP('2026-01-06 08:00:00') + INTERVAL n DAY
FROM seq
JOIN item_master parent_item
    ON parent_item.item_code = CASE
        WHEN n <= 30 THEN CONCAT('SM-', LPAD(1 + MOD(n - 1, 15), 3, '0'))
        ELSE CONCAT('FG-', LPAD(1 + MOD(n - 31, 15), 3, '0'))
    END
JOIN item_master child_item
    ON child_item.item_code = CASE
        WHEN n <= 30 THEN CONCAT('RM-', LPAD(1 + MOD((n * 7) - 1, 20), 3, '0'))
        ELSE CONCAT('SM-', LPAD(1 + MOD((n - 31) + (5 * FLOOR((n - 31) / 15)), 15), 3, '0'))
    END;

-- ===================================================================
-- 4. 입고 및 재고 도메인 (WMS / INVENTORY)
-- ===================================================================
INSERT IGNORE INTO current_inventory (item_id, location_id, lot_number, current_qty, first_inbound_date, updated_at)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 50
)
SELECT
    item.item_id,
    location.location_id,
    CONCAT('LOT-', LPAD(n, 3, '0')),
    100 + (n * 13),
    DATE('2026-01-01') + INTERVAL n DAY,
    TIMESTAMP('2026-02-01 10:00:00') + INTERVAL n DAY
FROM seq
JOIN item_master item
    ON item.item_code = CASE
        WHEN n <= 20 THEN CONCAT('RM-', LPAD(n, 3, '0'))
        WHEN n <= 35 THEN CONCAT('SM-', LPAD(n - 20, 3, '0'))
        ELSE CONCAT('FG-', LPAD(n - 35, 3, '0'))
    END
JOIN warehouse_location location
    ON location.location_code = CONCAT('WH', LPAD(1 + MOD(n - 1, 5), 2, '0'), '-R', CHAR(65 + MOD(n - 1, 10)), '-C', LPAD(1 + FLOOR((n - 1) / 10), 2, '0'));

INSERT IGNORE INTO inbound_receipt (item_id, partner_id, location_id, inbound_qty, inbound_date, worker_id, status, created_at)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 50
)
SELECT
    item.item_id,
    partner.partner_id,
    location.location_id,
    50 + (n * 5),
    DATE('2026-01-10') + INTERVAL n DAY,
    worker.user_id,
    ELT(1 + MOD(n - 1, 3), 'READY', 'COMPLETED', 'STACKED'),
    TIMESTAMP('2026-01-10 09:30:00') + INTERVAL n DAY
FROM seq
JOIN item_master item
    ON item.item_code = CASE
        WHEN n <= 20 THEN CONCAT('RM-', LPAD(n, 3, '0'))
        WHEN n <= 35 THEN CONCAT('SM-', LPAD(n - 20, 3, '0'))
        ELSE CONCAT('FG-', LPAD(n - 35, 3, '0'))
    END
JOIN partner_master partner
    ON partner.partner_code = CONCAT('SUP-', LPAD(1 + MOD(n - 1, 25), 3, '0'))
JOIN warehouse_location location
    ON location.location_code = CONCAT('WH', LPAD(1 + MOD(n - 1, 5), 2, '0'), '-R', CHAR(65 + MOD(n - 1, 10)), '-C', LPAD(1 + FLOOR((n - 1) / 10), 2, '0'))
JOIN users worker
    ON worker.employee_no = CONCAT('user', LPAD(1 + MOD(n, 50), 3, '0'));

-- ===================================================================
-- 5. 생산 도메인
-- ===================================================================
INSERT IGNORE INTO work_order (order_no, item_id, routing_id, target_qty, bom_version, status, plan_date, created_at, updated_at)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 50
)
SELECT
    CONCAT('WO-2026-', LPAD(n, 4, '0')),
    item.item_id,
    routing.routing_id,
    100 + (n * 4),
    'v1.0',
    ELT(1 + MOD(n - 1, 4), 'READY', 'RUN', 'HOLD', 'CLOSE'),
    DATE('2026-02-01') + INTERVAL n DAY,
    TIMESTAMP('2026-01-20 08:00:00') + INTERVAL n DAY,
    TIMESTAMP('2026-01-20 08:00:00') + INTERVAL n DAY
FROM seq
JOIN item_master item
    ON item.item_code = CASE WHEN n <= 25 THEN CONCAT('SM-', LPAD(1 + MOD(n - 1, 15), 3, '0')) ELSE CONCAT('FG-', LPAD(1 + MOD(n - 26, 15), 3, '0')) END
JOIN factory_routing routing
    ON routing.factory_name = CONCAT('창원제', 1 + FLOOR((n - 1) / 25), '공장')
    AND routing.line_name = CONCAT(CHAR(65 + MOD(FLOOR((n - 1) / 5), 5)), '라인')
    AND routing.operation_seq = 1 + MOD(n - 1, 5);

INSERT IGNORE INTO work_order_sequence (plan_date, last_sequence)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 50
)
SELECT
    DATE('2026-02-01') + INTERVAL n DAY,
    1 + MOD(n, 9)
FROM seq;

INSERT IGNORE INTO production_execution (
    order_id, routing_id, good_qty, defect_qty, defect_type, defect_reason, reworkable,
    worker_id, man_hours_minutes, created_at
)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 50
)
SELECT
    work_order.order_id,
    routing.routing_id,
    80 + (n * 3),
    MOD(n, 6),
    CASE WHEN MOD(n, 6) = 0 THEN NULL ELSE ELT(1 + MOD(n - 1, 4), '외관 불량', '치수 불량', '전장 불량', '포장 불량') END,
    CASE WHEN MOD(n, 6) = 0 THEN NULL ELSE CONCAT('검사 기준 미달 ', LPAD(n, 3, '0')) END,
    CASE WHEN MOD(n, 2) = 0 THEN 1 ELSE 0 END,
    worker.user_id,
    60 + (n * 7),
    TIMESTAMP('2026-02-01 13:00:00') + INTERVAL n DAY
FROM seq
JOIN work_order ON work_order.order_no = CONCAT('WO-2026-', LPAD(n, 4, '0'))
JOIN factory_routing routing
    ON routing.factory_name = CONCAT('창원제', 1 + FLOOR((n - 1) / 25), '공장')
    AND routing.line_name = CONCAT(CHAR(65 + MOD(FLOOR((n - 1) / 5), 5)), '라인')
    AND routing.operation_seq = 1 + MOD(n - 1, 5)
JOIN users worker
    ON worker.employee_no = CONCAT('user', LPAD(1 + MOD(n + 1, 50), 3, '0'));

INSERT IGNORE INTO inventory_transaction_history (
    item_id, location_id, transaction_type, quantity, reason_desc,
    work_order_id, production_execution_id, original_transaction_id, worker_id, created_at
)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 50
)
SELECT
    item.item_id,
    location.location_id,
    ELT(1 + MOD(n - 1, 12), 'RESERVATION', 'INBOUND', 'OUTBOUND', 'PRODUCTION_ISSUE', 'PRODUCTION_ISSUE_CANCEL', 'PRODUCTION_RECEIPT', 'PRODUCTION_RECEIPT_CANCEL', 'ADJUSTMENT', 'TRANSFER_OUT', 'TRANSFER_IN', 'SCRAP', 'RETURN'),
    10 + (n * 2),
    CONCAT('더미 수불 이력 ', LPAD(n, 3, '0')),
    CASE WHEN MOD(n, 3) = 0 THEN work_order.order_id ELSE NULL END,
    CASE WHEN MOD(n, 4) = 0 THEN execution.execution_id ELSE NULL END,
    NULL,
    worker.user_id,
    TIMESTAMP('2026-02-10 11:00:00') + INTERVAL n DAY
FROM seq
JOIN item_master item
    ON item.item_code = CASE
        WHEN n <= 20 THEN CONCAT('RM-', LPAD(n, 3, '0'))
        WHEN n <= 35 THEN CONCAT('SM-', LPAD(n - 20, 3, '0'))
        ELSE CONCAT('FG-', LPAD(n - 35, 3, '0'))
    END
JOIN warehouse_location location
    ON location.location_code = CONCAT('WH', LPAD(1 + MOD(n - 1, 5), 2, '0'), '-R', CHAR(65 + MOD(n - 1, 10)), '-C', LPAD(1 + FLOOR((n - 1) / 10), 2, '0'))
LEFT JOIN work_order ON work_order.order_no = CONCAT('WO-2026-', LPAD(n, 4, '0'))
LEFT JOIN production_execution execution ON execution.order_id = work_order.order_id
JOIN users worker
    ON worker.employee_no = CONCAT('user', LPAD(1 + MOD(n + 2, 50), 3, '0'));

-- ===================================================================
-- 6. 출하 도메인
-- ===================================================================
INSERT IGNORE INTO outbound_shipping (
    shipping_no, partner_id, item_id, request_qty, shipped_qty, shipping_type,
    picking_location_id, vehicle_no, carrier, tracking_no, estimated_delivery,
    cancel_reason, status, worker_id, shipped_at, created_at
)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 50
)
SELECT
    CONCAT('SHP-2026-', LPAD(n, 4, '0')),
    partner.partner_id,
    item.item_id,
    20 + (n * 3),
    CASE WHEN MOD(n, 7) = 0 THEN NULL ELSE 15 + (n * 3) END,
    ELT(1 + MOD(n - 1, 3), 'SALE', 'TRANSFER', 'RETURN'),
    location.location_id,
    CONCAT('서울', LPAD(n, 2, '0'), '가', LPAD(1000 + n, 4, '0')),
    ELT(1 + MOD(n - 1, 4), 'CJ대한통운', '한진택배', '롯데글로벌로지스', '직배송'),
    CONCAT('TRK2026', LPAD(n, 6, '0')),
    DATE('2026-03-01') + INTERVAL n DAY,
    CASE WHEN MOD(n, 13) = 0 THEN '고객 요청 취소' ELSE NULL END,
    ELT(1 + MOD(n - 1, 7), 'READY', 'PICKING', 'PACKING', 'INSPECTING', 'SHIPPED', 'PARTIALLY_SHIPPED', 'CANCELED'),
    worker.user_id,
    CASE WHEN MOD(n, 7) IN (5, 6) THEN TIMESTAMP('2026-03-01 15:00:00') + INTERVAL n DAY ELSE NULL END,
    TIMESTAMP('2026-02-20 09:00:00') + INTERVAL n DAY
FROM seq
JOIN partner_master partner
    ON partner.partner_code = CONCAT('CUS-', LPAD(1 + MOD(n - 1, 25), 3, '0'))
JOIN item_master item
    ON item.item_code = CONCAT('FG-', LPAD(1 + MOD(n - 1, 15), 3, '0'))
JOIN warehouse_location location
    ON location.location_code = CONCAT('WH', LPAD(1 + MOD(n - 1, 5), 2, '0'), '-R', CHAR(65 + MOD(n - 1, 10)), '-C', LPAD(1 + FLOOR((n - 1) / 10), 2, '0'))
JOIN users worker
    ON worker.employee_no = CONCAT('user', LPAD(1 + MOD(n + 3, 50), 3, '0'));

-- ===================================================================
-- 7. AI 도메인
-- ===================================================================
INSERT IGNORE INTO ai_query_history (
    worker_id, natural_question, conversation_id, parent_query_id, effective_question,
    generated_sql, natural_answer, result_json, chart_spec_json, row_count,
    execution_time_ms, model_name, execution_status, error_log, created_at
)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 50
)
SELECT
    worker.user_id,
    CONCAT('2026년 재고와 생산 현황을 조회해줘 ', LPAD(n, 3, '0')),
    CONCAT('conv-2026-', LPAD(1 + FLOOR((n - 1) / 5), 3, '0')),
    NULL,
    CONCAT('2026년 재고와 생산 현황 조회 ', LPAD(n, 3, '0')),
    'SELECT COUNT(*) FROM current_inventory',
    CONCAT('조회 결과 ', 10 + n, '건입니다.'),
    CONCAT('{"rows":', 10 + n, '}'),
    CONCAT('{"type":"bar","index":', n, '}'),
    10 + n,
    1000 + (n * 37),
    'gpt-4.1-mini',
    ELT(1 + MOD(n - 1, 6), 'SUCCESS', 'CLARIFICATION_REQUIRED', 'CLARIFICATION_ANSWERED', 'SQL_EXECUTION_FAILED', 'ANSWER_GENERATION_FAILED', 'TIMEOUT'),
    CASE WHEN MOD(n, 6) IN (3, 4, 5) THEN CONCAT('더미 오류 로그 ', LPAD(n, 3, '0')) ELSE NULL END,
    TIMESTAMP('2026-03-10 10:00:00') + INTERVAL n DAY
FROM seq
JOIN users worker
    ON worker.employee_no = CONCAT('user', LPAD(1 + MOD(n + 4, 50), 3, '0'));

INSERT IGNORE INTO dynamic_batch_schedule (
    schedule_name, cron_expression, query_id, is_active, last_run_at,
    last_run_status, worker_id, created_at
)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 50
)
SELECT
    CONCAT('AI 정기 리포트 ', LPAD(n, 3, '0')),
    ELT(1 + MOD(n - 1, 5), '0 0 8 * * *', '0 0 12 * * *', '0 0 18 * * *', '0 30 9 * * MON', '0 0 6 1 * *'),
    query_history.query_id,
    CASE WHEN MOD(n, 11) = 0 THEN 0 ELSE 1 END,
    TIMESTAMP('2026-04-01 08:00:00') + INTERVAL n DAY,
    ELT(1 + MOD(n - 1, 3), 'SUCCESS', 'FAILED', 'NONE'),
    worker.user_id,
    TIMESTAMP('2026-03-20 08:00:00') + INTERVAL n DAY
FROM seq
JOIN ai_query_history query_history
    ON query_history.conversation_id = CONCAT('conv-2026-', LPAD(1 + FLOOR((n - 1) / 5), 3, '0'))
    AND query_history.natural_question = CONCAT('2026년 재고와 생산 현황을 조회해줘 ', LPAD(n, 3, '0'))
JOIN users worker
    ON worker.employee_no = CONCAT('user', LPAD(1 + MOD(n + 5, 50), 3, '0'));
