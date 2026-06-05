export interface MenuItem {
  name: string
  path: string
}

export interface MenuGroup {
  category: string
  iconPath: string
  items: MenuItem[]
}

export const menuGroups: MenuGroup[] = [
  {
    category: '시스템 관리',
    iconPath: 'M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z',
    items: [{ name: '사용자 및 권한', path: '/' }]
  },
  {
    category: '기준정보 관리',
    iconPath: 'M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10',
    items: [
      { name: '품목 마스터', path: '/master/items' },
      { name: '거래처 마스터', path: '/master/partners' },
      { name: '공장 및 생산 라인', path: '/master/factory-lines' },
      { name: 'BOM (부품명세서)', path: '/master/boms' }
    ]
  },
  {
    category: '입고 관리',
    iconPath: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01',
    items: [
      { name: '입고 등록', path: '/inbound/receipt' },
      { name: '창고 적재', path: '/inbound/stack' }
    ]
  },
  {
    category: '재고 관리',
    iconPath: 'M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4',
    items: [
      { name: '현재고 현황', path: '/inventory/status' },
      { name: '수불 이력 조회', path: '/inventory/history' }
    ]
  },
  {
    category: '생산 관리',
    iconPath: 'M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z M15 12a3 3 0 11-6 0 3 3 0 016 0z',
    items: [
      { name: '작업 지시 등록', path: '/' },
      { name: '자재 출고(불출) 처리', path: '/production/issue' },
      { name: '공정 실적 및 원부자재', path: '/' }
    ]
  },
  {
    category: '출고 관리',
    iconPath: 'M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4',
    items: [
      { name: '출하 지시', path: '/shipping/order' },
      { name: '피킹/상차', path: '/shipping/picking' }
    ]
  },
  {
    category: '지능형 분석 서비스',
    iconPath: 'M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z',
    items: [{ name: 'AI 데이터 챗봇', path: '/' }]
  }
]
