export interface MockData {
  title: string
  subtitle: string
  stats: { label: string; value: string }[]
  tableTitle: string
  tableMoreText: string
  columns: { key: string; label: string }[]
  tableData: Record<string, any>[]
}

export const mockApi = {
  async getMockData(): Promise<MockData> {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          title: '대시보드 (Dashboard)',
          subtitle: '실시간 재고 현황 및 설비 모니터링 정보를 확인합니다.',
          stats: [
            { label: '전체 적재율', value: '78.4%' },
            { label: '금일 입고 예정', value: '12 건' },
            { label: '금일 출고 완료', value: '34 건' }
          ],
          tableTitle: '실시간 재고 목록 (최근 5건)',
          tableMoreText: '전체 보기 →',
          columns: [
            { key: 'itemCode', label: '품목 코드' },
            { key: 'itemName', label: '품목명' },
            { key: 'category', label: '구분' },
            { key: 'stock', label: '현재고' },
            { key: 'safetyStock', label: '안전재고' }
          ],
          tableData: [
            { itemCode: 'ITEM-A01', itemName: '반도체 세척액 B', category: '원자재', stock: '4,500 L', safetyStock: '1,000 L' },
            { itemCode: 'ITEM-B12', itemName: '실리콘 웨이퍼 8인치', category: '원자재', stock: '2,300 pcs', safetyStock: '500 pcs' },
            { itemCode: 'ITEM-C05', itemName: '포토레지스트 용액', category: '원자재', stock: '800 L', safetyStock: '200 L' }
          ]
        })
      }, 200)
    })
  }
}
