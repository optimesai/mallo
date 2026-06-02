import axios from 'axios'
import type { ApiResponse } from '@/api/authApi'

export interface MockWorkOrder {
  orderId: number
  orderNo: string
  itemCode: string
  itemName: string
  targetQty: number
  status: 'READY' | 'RUN' | 'HOLD' | 'CLOSE'
  planDate: string
}

export interface MockBomStructure {
  parentItemCode: string
  childItemCode: string
  childItemName: string
  unit: string
  quantityPerUnit: number
}

// Mock work orders representing production commands
const MOCK_WORK_ORDERS: MockWorkOrder[] = [
  {
    orderId: 1,
    orderNo: 'WO-20260602-001',
    itemCode: 'FP-SMART-BOX',
    itemName: '스마트 물류 제어 단말기',
    targetQty: 50,
    status: 'READY',
    planDate: '2026-06-02'
  },
  {
    orderId: 2,
    orderNo: 'WO-20260602-002',
    itemCode: 'FP-CONTROLLER',
    itemName: '산업용 모터 인버터 정밀 제어기',
    targetQty: 20,
    status: 'READY',
    planDate: '2026-06-02'
  },
  {
    orderId: 3,
    orderNo: 'WO-20260602-003',
    itemCode: 'SM-PCB-ASSY',
    itemName: '제어보드 PCB 조립체',
    targetQty: 100,
    status: 'READY',
    planDate: '2026-06-02'
  },
  {
    orderId: 4,
    orderNo: 'WO-20260602-004',
    itemCode: 'FP-SMART-BOX',
    itemName: '스마트 물류 제어 단말기 (재고부족 유도용)',
    targetQty: 9999,
    status: 'READY',
    planDate: '2026-06-02'
  }
]

// Mock BOM structures matching database data.sql
const MOCK_BOM_STRUCTURES: MockBomStructure[] = [
  // 1. 반제품 '제어보드 PCB 조립체' (SM-PCB-ASSY): 메인 칩셋 1개, 케이블 0.05박스
  { parentItemCode: 'SM-PCB-ASSY', childItemCode: 'RM-CHIP-5G', childItemName: '통신 제어용 메인 칩셋', unit: 'ea', quantityPerUnit: 1.0 },
  { parentItemCode: 'SM-PCB-ASSY', childItemCode: 'RM-CABLE-02', childItemName: '고온 절연 와이어 케이블', unit: 'box', quantityPerUnit: 0.05 },

  // 2. 반제품 '강판 프레임 용접 모듈' (SM-FRAME-A): 탄소강판 12.5kg
  { parentItemCode: 'SM-FRAME-A', childItemCode: 'RM-STEEL-01', childItemName: '고탄소 탄소강판', unit: 'kg', quantityPerUnit: 12.5 },

  // 3. 완제품 '스마트 물류 제어 단말기' (FP-SMART-BOX): PCB조립체 1개, 프레임모듈 1개, 사출케이스 1개
  { parentItemCode: 'FP-SMART-BOX', childItemCode: 'SM-PCB-ASSY', childItemName: '제어보드 PCB 조립체', unit: 'ea', quantityPerUnit: 1.0 },
  { parentItemCode: 'FP-SMART-BOX', childItemCode: 'SM-FRAME-A', childItemName: '강판 프레임 용접 모듈', unit: 'ea', quantityPerUnit: 1.0 },
  { parentItemCode: 'FP-SMART-BOX', childItemCode: 'RM-PLASTIC-P', childItemName: '강화 플라스틱 사출 케이스', unit: 'ea', quantityPerUnit: 1.0 },

  // 4. 완제품 '산업용 모터 인버터 정밀 제어기' (FP-CONTROLLER): 메인 칩셋 2개, 케이블 0.2박스
  { parentItemCode: 'FP-CONTROLLER', childItemCode: 'RM-CHIP-5G', childItemName: '통신 제어용 메인 칩셋', unit: 'ea', quantityPerUnit: 2.0 },
  { parentItemCode: 'FP-CONTROLLER', childItemCode: 'RM-CABLE-02', childItemName: '고온 절연 와이어 케이블', unit: 'box', quantityPerUnit: 0.2 }
]

const AUTH_TOKEN_KEY = 'ssafy-pjt-access-token'

function getAuthHeaders() {
  const token = localStorage.getItem(AUTH_TOKEN_KEY)
  return {
    Authorization: `Bearer ${token}`
  }
}

export const workOrderApi = {
  // Returns mock list of work orders
  async getWorkOrders(): Promise<MockWorkOrder[]> {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve([...MOCK_WORK_ORDERS])
      }, 150)
    })
  },

  // Returns mock BOM structures for a parent item
  async getMaterialsRequirements(itemCode: string): Promise<MockBomStructure[]> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const directChildren = MOCK_BOM_STRUCTURES.filter(bom => bom.parentItemCode === itemCode)
        resolve(directChildren)
      }, 100)
    })
  },

  // Real POST request to execute material issue
  async issueMaterials(orderId: number) {
    const response = await axios.post<ApiResponse<void>>(`/api/work-orders/${orderId}/issue-materials`, null, {
      headers: getAuthHeaders()
    })
    return response.data
  }
}
