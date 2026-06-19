import { AxiosError } from 'axios'
import { aiApi } from '@/api/aiApi'
import type { AiQueryRequest, AiQueryResponse } from '@/api/aiApi'

export const aiService = {
  async ask(request: AiQueryRequest): Promise<AiQueryResponse> {
    try {
      const response = await aiApi.ask(request)
      return response.data
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || 'AI 데이터 질의에 실패했습니다.')
      }
      throw new Error('AI 데이터 질의에 실패했습니다.')
    }
  }
}
