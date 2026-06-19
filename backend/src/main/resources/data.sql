-- ===================================================================
-- 0. 스키마 보정 (SCHEMA PATCH)
-- ===================================================================
ALTER TABLE ai_query_history
    MODIFY COLUMN execution_status VARCHAR(50) NOT NULL;

-- ===================================================================
-- ===================================================================
-- 1. 회원 도메인 (MEMBER)
-- ===================================================================
INSERT IGNORE INTO users (employee_no, user_name, department, password, role) VALUES
('EMP202601', '김관리', 'DevOps/AI 아키텍트 팀', '$2a$10$YnK23Eld...', 'ADMIN'),
('EMP202602', '김작업', 'MES/백엔드 개발 팀', '$2a$10$Zk91Mdls...', 'MANAGER'),
('EMP202603', '이창고', '물류 관리 팀 (WMS)', '$2a$10$Ab12Cd34...', 'WORKER'),
('EMP202604', '박공정', '생산 공정 A라인', '$2a$10$Ef56Gh78...', 'WORKER');


-- ===================================================================
-- 2. 기준정보 관리 도메인 (MASTER DATA)
-- ===================================================================

-- [item_master] 인서트
INSERT IGNORE INTO item_master (item_code, item_name, spec, unit, item_type, safety_stock) VALUES
('RM-STEEL-01', '고탄소 탄소강판', '2.0T * 1219 * 2438', 'kg', 'RAW', 5000),
('RM-CHIP-5G', '통신 제어용 메인 칩셋', 'Snapdragon IoT-v3', 'ea', 'RAW', 200),
('RM-CABLE-02', '고온 절연 와이어 케이블', 'Ø5.0 / 100m roll', 'box', 'RAW', 50),
('RM-PLASTIC-P', '강화 플라스틱 사출 케이스', 'Standard-450', 'ea', 'RAW', 1000),
('SM-PCB-ASSY', '제어보드 PCB 조립체', 'SMD-DoubleSided', 'ea', 'HALF', 300),
('SM-FRAME-A', '강판 프레임 용접 모듈', 'Standard-A급 용접형', 'ea', 'HALF', 150),
('FP-SMART-BOX', '스마트 물류 제어 단말기', 'SB-2026-Pro', 'ea', 'FG', 100),
('FP-CONTROLLER', '산업용 모터 인버터 정밀 제어기', 'IC-100HP', 'ea', 'FG', 50);

-- 생성된 AUTO_INCREMENT PK 값을 안전하게 추적하기 위해 변수에 담아둡니다.
SET @rm_steel = (SELECT item_id FROM item_master WHERE item_code = 'RM-STEEL-01');
SET @rm_chip  = (SELECT item_id FROM item_master WHERE item_code = 'RM-CHIP-5G');
SET @rm_cable = (SELECT item_id FROM item_master WHERE item_code = 'RM-CABLE-02');
SET @rm_case  = (SELECT item_id FROM item_master WHERE item_code = 'RM-PLASTIC-P');
SET @sm_pcb   = (SELECT item_id FROM item_master WHERE item_code = 'SM-PCB-ASSY');
SET @sm_frame = (SELECT item_id FROM item_master WHERE item_code = 'SM-FRAME-A');
SET @fp_box   = (SELECT item_id FROM item_master WHERE item_code = 'FP-SMART-BOX');
SET @fp_ctrl  = (SELECT item_id FROM item_master WHERE item_code = 'FP-CONTROLLER');


-- [partner_master] 인서트
INSERT IGNORE INTO partner_master (partner_code, partner_name, partner_type, business_no, representative, contact_phone) VALUES
('SUP-POSCO-01', '(주)포스코 인터내셔널', 'SUPPLIER', '123-45-67890', '이구택', '02-3457-1114'),
('SUP-SAMSUNG-E', '삼성전자 디바이스솔루션', 'SUPPLIER', '220-81-62517', '경계현', '031-200-1114'),
('SUP-SAMWHA-0', '삼화전기(주)', 'SUPPLIER', '301-81-04281', '오영주', '043-261-0111'),
('CUS-HYUNDAI-M', '현대모비스 울산공장', 'CUSTOMER', '113-81-22441', '이규석', '052-202-0114'),
('CUS-LG-ENSOL', 'LG에너지솔루션 오창공장', 'CUSTOMER', '582-88-01230', '권영수', '043-219-0114');


-- [factory_routing] 인서트
INSERT IGNORE INTO factory_routing (factory_name, line_name, operation_seq, operation_name) VALUES
('창원제1공장', 'A라인', 1, 'SMD 표면실장 공정'),
('창원제1공장', 'A라인', 2, '프레임 기계조립 공정'),
('창원제1공장', 'A라인', 3, '펌웨어 인젝션 및 최종 검사 공정'),
('창원제1공장', 'B라인', 1, '메인보드 배선 공정'),
('창원제1공장', 'B라인', 2, '인버터 하우징 조립 공정'),
('창원제1공장', 'B라인', 3, '에이징 테스트 및 포장 공정');


-- [bom_structure] 인서트 (추출한 대리 키 변수를 바인딩)
INSERT IGNORE INTO bom_structure (parent_item_id, child_item_id, quantity, bom_version) VALUES
-- 1. 반제품 '제어보드 PCB 조립체'를 만들기 위해: 메인 칩셋 1개, 케이블 0.05박스 투입
(@sm_pcb, @rm_chip, 1.0000, 'v1.0'),
(@sm_pcb, @rm_cable, 0.0500, 'v1.0'),

-- 2. 반제품 '강판 프레임 용접 모듈'을 만들기 위해: 탄소강판 12.5kg 투입
(@sm_frame, @rm_steel, 12.5000, 'v1.0'),

-- 3. 완제품 '스마트 물류 제어 단말기'를 만들기 위해: PCB조립체 1개, 프레임모듈 1개, 사출케이스 1개 투입
(@fp_box, @sm_pcb, 1.0000, 'v1.0'),
(@fp_box, @sm_frame, 1.0000, 'v1.0'),
(@fp_box, @rm_case, 1.0000, 'v1.0'),

-- 4. 완제품 '산업용 모터 인버터 정밀 제어기'를 만들기 위해: 메인 칩셋 2개, 케이블 0.2박스 투입
(@fp_ctrl, @rm_chip, 2.0000, 'v1.0'),
(@fp_ctrl, @rm_cable, 0.2000, 'v1.0');


-- ===================================================================
-- 3. 입고 및 재고 관리 도메인 (WMS / INVENTORY)
-- ===================================================================

-- [warehouse_location] 인서트
INSERT IGNORE INTO warehouse_location (location_code, warehouse_name, rack_row, rack_column, production_receipt_default) VALUES
('WH01-RACK-A1', '원자재 창고', 'A열', '1단', false),
('WH01-RACK-A2', '원자재 창고', 'A열', '2단', false),
('WH02-RACK-B1', '반제품 창고', 'B열', '1단', false),
('WH03-RACK-C1', '완제품 창고', 'C열', '1단', true),
('WH03-RACK-C2', '완제품 창고', 'C열', '2단', false);
