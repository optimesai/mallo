import { mockApi, type MockData } from '@/api/mockApi'

export const mockService = {
  async fetchMock(): Promise<MockData> {
    try {
      return await mockApi.getMockData()
    } catch (error) {
      console.error('Failed to load dashboard data in service:', error)
      throw error
    }
  }
}
