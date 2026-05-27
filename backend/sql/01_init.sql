-- 데이터베이스 초기화 (필요시 사용)
DROP DATABASE IF EXISTS ssafy_pjt;
CREATE DATABASE ssafy_pjt;
USE ssafy_pjt;

-- ===================================================================
-- 1. 회원 도메인 (MEMBER)
-- ===================================================================

CREATE TABLE users (
    user_id INT AUTO_INCREMENT COMMENT '사용자 고유 ID (PK)',
    employee_no VARCHAR(50) NOT NULL COMMENT '사내 고유 사번',
    user_name VARCHAR(50) NOT NULL COMMENT '성명',
    department VARCHAR(100) NOT NULL COMMENT '소속 부서',
    password VARCHAR(255) NOT NULL COMMENT '단방향 암호화된 비밀번호',
    role ENUM('WORKER', 'MANAGER', 'ADMIN') NOT NULL DEFAULT 'WORKER' COMMENT '사용자 권한 (WORKER: 현장 작업자, MANAGER: 현장 관리자, ADMIN: 시스템 관리자)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '가입 일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    PRIMARY KEY (user_id),
    UNIQUE KEY idx_employee_no_unique (employee_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='사용자 마스터 테이블';

-- ===================================================================
-- 2. 기준정보 관리 도메인 (MASTER DATA)
-- ===================================================================

CREATE TABLE item_master (
    item_id INT AUTO_INCREMENT COMMENT '품목 고유 ID (PK)',
    item_code VARCHAR(50) NOT NULL COMMENT '품목 코드',
    item_name VARCHAR(100) NOT NULL COMMENT '품목명',
    spec VARCHAR(100) COMMENT '규격 및 사이즈',
    unit ENUM('ea', 'kg', 'box', 'L') NOT NULL COMMENT '기본 단위',
    item_type ENUM('RAW', 'HALF', 'FG') NOT NULL COMMENT '품목 분류 (RAW: 자재, HALF: 반제품, FG: 완제품)',
    safety_stock INT NOT NULL DEFAULT 0 COMMENT '안전 재고량 (AI 분석 및 경고 기준)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    PRIMARY KEY (item_id),
    UNIQUE KEY idx_item_code_unique (item_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='품목 마스터 테이블';

CREATE TABLE partner_master (
    partner_id INT AUTO_INCREMENT COMMENT '거래처 고유 ID (PK)',
    partner_code VARCHAR(50) NOT NULL COMMENT '거래처 고유 코드',
    partner_name VARCHAR(100) NOT NULL COMMENT '거래처명',
    partner_type ENUM('SUPPLIER', 'CUSTOMER') NOT NULL COMMENT '거래처 구분 (SUPPLIER: 공급사, CUSTOMER: 고객사)',
    business_no VARCHAR(50) COMMENT '사업자등록번호',
    representative VARCHAR(50) COMMENT '대표자명',
    contact_phone VARCHAR(50) COMMENT '담당자 연락처',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    PRIMARY KEY (partner_id),
    UNIQUE KEY idx_partner_code_unique (partner_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='거래처 마스터 테이블';

CREATE TABLE factory_routing (
    routing_id INT AUTO_INCREMENT COMMENT '라우팅 고유 ID (PK)',
    factory_name VARCHAR(50) NOT NULL COMMENT '공장명',
    line_name VARCHAR(50) NOT NULL COMMENT '생산 라인명 (e.g., A라인, B라인)',
    operation_seq INT NOT NULL COMMENT '공정 순서 (e.g., 1, 2, 3)',
    operation_name VARCHAR(50) NOT NULL COMMENT '세부 공정명 (e.g., 배합, 성형, 포장)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    PRIMARY KEY (routing_id),
    UNIQUE KEY idx_routing_unique (factory_name, line_name, operation_seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='공장 및 생산 라인 라우팅 구성 테이블';

CREATE TABLE bom_structure (
    bom_id INT AUTO_INCREMENT COMMENT 'BOM 고유 ID (PK)',
    parent_item_id INT NOT NULL COMMENT '부모 품목 고유 ID (완제품 또는 반제품, FK)',
    child_item_id INT NOT NULL COMMENT '자식 품목 고유 ID (반제품 또는 원자재, FK)',
    quantity DECIMAL(10, 4) NOT NULL COMMENT '투입 소요량 (부모 1단위 생산에 필요한 자식 수량)',
    bom_version VARCHAR(20) NOT NULL DEFAULT 'v1.0' COMMENT 'BOM 레시피 버전',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    PRIMARY KEY (bom_id),
    FOREIGN KEY (parent_item_id) REFERENCES item_master(item_id),
    FOREIGN KEY (child_item_id) REFERENCES item_master(item_id),
    UNIQUE KEY idx_bom_tree (parent_item_id, child_item_id, bom_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='제품별 BOM 부품명세서 (계층형 구조)';

-- ===================================================================
-- 3. 입고 및 재고 관리 도메인 (WMS / INVENTORY)
-- ===================================================================

CREATE TABLE warehouse_location (
    location_id INT AUTO_INCREMENT COMMENT '로케이션 고유 ID (PK)',
    location_code VARCHAR(50) NOT NULL COMMENT '로케이션 창고 및 렉 코드 (e.g., WH01-RACK02)',
    warehouse_name VARCHAR(50) NOT NULL COMMENT '가용 창고명 (e.g., 원자재창고, 완제품창고)',
    rack_row VARCHAR(10) COMMENT '렉 가로 위치',
    rack_column VARCHAR(10) COMMENT '렉 세로 위치',
    PRIMARY KEY (location_id),
    UNIQUE KEY idx_location_code_unique (location_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='창고 내부 세부 로케이션(렉) 정보 테이블';

CREATE TABLE current_inventory (
    inventory_id INT AUTO_INCREMENT COMMENT '재고 고유 ID (PK)',
    item_id INT NOT NULL COMMENT '품목 고유 ID (FK)',
    location_id INT NOT NULL COMMENT '위치 고유 ID (FK)',
    current_qty INT NOT NULL DEFAULT 0 COMMENT '실시간 현재고 수량',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '마지막 재고 변동 일시',
    PRIMARY KEY (inventory_id),
    FOREIGN KEY (item_id) REFERENCES item_master(item_id),
    FOREIGN KEY (location_id) REFERENCES warehouse_location(location_id),
    UNIQUE KEY idx_item_location (item_id, location_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='실시간 창고/로케이션별 현재고 테이블';

CREATE TABLE inbound_receipt (
    inbound_id INT AUTO_INCREMENT COMMENT '입고 고유 ID (PK)',
    item_id INT NOT NULL COMMENT '입고 품목 고유 ID (FK)',
    partner_id INT NOT NULL COMMENT '외주 공급사 고유 ID (FK)',
    location_id INT NOT NULL COMMENT '적재될 창고 로케이션 고유 ID (FK)',
    inbound_qty INT NOT NULL COMMENT '입고 검수 수량',
    inbound_date DATE NOT NULL COMMENT '전산 입고 처리 일자',
    worker_id INT COMMENT '물류 담당자 사용자 고유 ID (FK)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '로그 기록 일시',
    PRIMARY KEY (inbound_id),
    FOREIGN KEY (item_id) REFERENCES item_master(item_id),
    FOREIGN KEY (partner_id) REFERENCES partner_master(partner_id),
    FOREIGN KEY (location_id) REFERENCES warehouse_location(location_id),
    FOREIGN KEY (worker_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='원부자재 전산 입고 등록 이력 테이블';

CREATE TABLE inventory_transaction_history (
    transaction_id INT AUTO_INCREMENT COMMENT '수불 이력 고유 ID (PK)',
    item_id INT NOT NULL COMMENT '품목 고유 ID (FK)',
    location_id INT NOT NULL COMMENT '위치 고유 ID (FK)',
    transaction_type ENUM('INBOUND', 'OUTBOUND', 'PRODUCTION_ISSUE') NOT NULL 
        COMMENT '수불 유형 (INBOUND:입고, OUTBOUND:최종출하, PRODUCTION_ISSUE:생산불출)',
    quantity INT NOT NULL COMMENT '변동 수량 (항상 양수)',
    reason_desc VARCHAR(255) COMMENT '이동 및 조정 사유 상세내역',
    worker_id INT COMMENT '처리 작업자 사용자 고유 ID (FK)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '수불 발생 일시',
    PRIMARY KEY (transaction_id),
    FOREIGN KEY (item_id) REFERENCES item_master(item_id),
    FOREIGN KEY (location_id) REFERENCES warehouse_location(location_id),
    FOREIGN KEY (worker_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='창고 및 로케이션간 재고 수불 이동 이력(History) 테이블';

-- ===================================================================
-- 4. 생산 및 제조 관리 도메인 (MES / PRODUCTION)
-- ===================================================================

CREATE TABLE work_order (
    order_id INT AUTO_INCREMENT COMMENT '작업 지시 고유 ID (PK)',
    order_no VARCHAR(50) NOT NULL COMMENT '생산 작업 지시 번호 (e.g., WO-20260520-001)',
    item_id INT NOT NULL COMMENT '생산할 완제품/반제품 품목 고유 ID (FK)',
    routing_id INT NOT NULL COMMENT '지정 생산 라인 및 공정 경로 ID (FK)',
    target_qty INT NOT NULL COMMENT '생산 목표 수량',
    status ENUM('READY', 'RUN', 'HOLD', 'CLOSE') NOT NULL DEFAULT 'READY' COMMENT '작업 지시 상태 (READY:대기, RUN:진행, HOLD:보류, CLOSE:최종마감)',
    plan_date DATE NOT NULL COMMENT '생산 계획 수립 일자',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '지시 발행 일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '상태 제어 변경 일시',
    PRIMARY KEY (order_id),
    UNIQUE KEY idx_order_no_unique (order_no),
    FOREIGN KEY (item_id) REFERENCES item_master(item_id),
    FOREIGN KEY (routing_id) REFERENCES factory_routing(routing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='생산 계획 및 작업 지시서 발행 테이블';

CREATE TABLE production_execution (
    execution_id INT AUTO_INCREMENT COMMENT '공정 실적 고유 ID (PK)',
    order_id INT NOT NULL COMMENT '연동된 작업 지시 고유 ID (FK)',
    good_qty INT NOT NULL DEFAULT 0 COMMENT '실제 생산 완료한 양품(합격) 수량',
    defect_qty INT NOT NULL DEFAULT 0 COMMENT '생산 중 발생한 불량품 수량',
    worker_id INT COMMENT '실적 입력 현장 작업자 사용자 고유 ID (FK)',
    man_hours_minutes INT NOT NULL COMMENT '공정별 총 소요 시간 (분 단위)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '실적 실시간 등록 일시',
    PRIMARY KEY (execution_id),
    FOREIGN KEY (order_id) REFERENCES work_order(order_id),
    FOREIGN KEY (worker_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='공정별 생산 작업 실적 및 자재 투입량 실시간 등록 테이블';

-- ===================================================================
-- 5. 완제품 출고 도메인 (WMS / OUTBOUND)
-- ===================================================================

CREATE TABLE outbound_shipping (
    shipping_id INT AUTO_INCREMENT COMMENT '출하 지시 고유 ID (PK)',
    shipping_no VARCHAR(50) NOT NULL COMMENT '출하 지시 번호 (e.g., SH-20260520-001)',
    partner_id INT NOT NULL COMMENT '납품 고객사 고유 ID (FK)',
    item_id INT NOT NULL COMMENT '출하할 완제품 품목 고유 ID (FK)',
    request_qty INT NOT NULL COMMENT '고객사 주문 확정 수량',
    picking_location_id INT COMMENT '완제품 창고 피킹 적재 위치 고유 ID (FK)',
    vehicle_no VARCHAR(50) COMMENT '배정 상차 차량 번호',
    status ENUM('READY', 'PICKING', 'SHIPPED') NOT NULL DEFAULT 'READY' COMMENT '출하 진행 상태 (READY:출하지시, PICKING:피킹중, SHIPPED:최종출고완료)',
    worker_id INT COMMENT '물류 담당자 사용자 고유 ID (FK)',
    shipped_at TIMESTAMP COMMENT '최종 상차 완료 및 출하 명세서 발행 일시',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '출하지시서 최초 발행 일시',
    PRIMARY KEY (shipping_id),
    UNIQUE KEY idx_shipping_no_unique (shipping_no),
    FOREIGN KEY (partner_id) REFERENCES partner_master(partner_id),
    FOREIGN KEY (item_id) REFERENCES item_master(item_id),
    FOREIGN KEY (picking_location_id) REFERENCES warehouse_location(location_id),
    FOREIGN KEY (worker_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='완제품 출하 지시 및 피킹/상차 최종 출고 관리 테이블';

-- ===================================================================
-- 6. AI 플랫폼 및 업무 자동화 관리 도메인 (AI SYSTEM)
-- ===================================================================

CREATE TABLE ai_query_history (
    query_id INT AUTO_INCREMENT COMMENT '질의 로그 고유 ID (PK)',
    worker_id INT NOT NULL COMMENT '자연어 질문을 던진 사용자 고유 ID (FK)',
    natural_question TEXT NOT NULL COMMENT '사용자가 입력한 실제 자연어 질의 내용',
    generated_sql TEXT COMMENT 'AI가 맥락 인식(NL2SQL)을 통해 가공 변환한 ANSI-SQL 문',
    execution_status ENUM('SUCCESS', 'BLOCKED_DML', 'SYNTAX_ERROR', 'TIMEOUT') NOT NULL DEFAULT 'SUCCESS' 
        COMMENT '실행 및 보안 샌드박스 결과 (SUCCESS:성공, BLOCKED_DML:악성쿼리 원천차단, SYNTAX_ERROR:문법오류)',
    error_log TEXT COMMENT '예외 발생 시 반환된 시스템 에러 로그',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '질의 요청 일시',
    PRIMARY KEY (query_id),
    FOREIGN KEY (worker_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 분석 질의 및 NL2SQL 변환 실행 이력 아카이브 테이블';

CREATE TABLE dynamic_batch_schedule (
    schedule_id INT AUTO_INCREMENT COMMENT '배치 작업 스케줄 고유 ID (PK)',
    schedule_name VARCHAR(100) NOT NULL COMMENT '배치 스케줄 업무명 (e.g., 주간 가동률 리포터 자동 생성)',
    cron_expression VARCHAR(50) NOT NULL COMMENT '런타임 구동용 Cron 표현식 (e.g., 0 0 2 * * ?)',
    query_id INT NOT NULL COMMENT '정기 배치 실행 시 호출할 AI 변환 쿼리 매핑 ID (FK)',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '스케줄 활성화 여부 토글 상태',
    last_run_at TIMESTAMP COMMENT '최근 배치 작업 수행 시작 시각',
    last_run_status ENUM('SUCCESS', 'FAILED', 'NONE') NOT NULL DEFAULT 'NONE' COMMENT '최근 배치 스케줄러 수행 결과 상태',
    worker_id INT COMMENT '배치 동적 등록 및 관리자 사용자 고유 ID (FK)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '배치 스케줄러 최초 등록 일시',
    PRIMARY KEY (schedule_id),
    FOREIGN KEY (query_id) REFERENCES ai_query_history(query_id),
    FOREIGN KEY (worker_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ThreadPoolTaskScheduler 연동 동적 런타임 배치 작업 스케줄 제어 테이블';
