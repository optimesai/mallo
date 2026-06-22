import { defineStore } from 'pinia'
import { ref } from 'vue'
import { aiService } from '@/services/aiService'
import type { AiQueryResponse } from '@/api/aiApi'

export interface AiMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  response?: AiQueryResponse
  createdAt: string
}

let messageSequence = 0

export const useAiStore = defineStore('ai', () => {
  const messages = ref<AiMessage[]>([])
  const currentResponse = ref<AiQueryResponse | null>(null)
  const isLoading = ref<boolean>(false)
  const error = ref<string | null>(null)

  async function ask(question: string) {
    const normalizedQuestion = question.trim()
    if (!normalizedQuestion) return

    const userMessage = createMessage('user', normalizedQuestion)
    messages.value.push(userMessage)

    isLoading.value = true
    error.value = null

    try {
      const response = await aiService.ask({ question: normalizedQuestion })
      currentResponse.value = response

      const assistantMessage = createMessage(
        'assistant',
        getAssistantContent(response),
        response
      )
      messages.value.push(assistantMessage)
      return response
    } catch (err) {
      const message = err instanceof Error ? err.message : 'AI 데이터 질의에 실패했습니다.'
      error.value = message
      messages.value.push(createMessage('assistant', message))
      throw err
    } finally {
      isLoading.value = false
    }
  }

  function selectResponse(response: AiQueryResponse) {
    currentResponse.value = response
  }

  function clearMessages() {
    messages.value = []
    currentResponse.value = null
    error.value = null
  }

  function createMessage(
    role: AiMessage['role'],
    content: string,
    response?: AiQueryResponse
  ): AiMessage {
    messageSequence += 1
    return {
      id: messageSequence,
      role,
      content,
      response,
      createdAt: new Date().toISOString()
    }
  }

  function getAssistantContent(response: AiQueryResponse) {
    if (response.clarificationRequired && response.clarificationQuestion) {
      return response.clarificationQuestion
    }
    return response.answer || '조회 결과를 확인해 주세요.'
  }

  return {
    messages,
    currentResponse,
    isLoading,
    error,
    ask,
    selectResponse,
    clearMessages
  }
})
