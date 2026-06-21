<script setup lang="ts">
import { computed } from 'vue'
import { AlertTriangle, Bot, CheckCircle2, Database, Sparkles } from '@lucide/vue'
import type { AiQueryResponse } from '@/api/aiApi'

const props = defineProps<{
  response?: AiQueryResponse | null
}>()

const statusClass = computed(() => {
  if (!props.response) return 'app-status-neutral'
  if (props.response.clarificationRequired) return 'app-status-warning'
  if (props.response.executionStatus === 'SUCCESS') return 'app-status-success'
  if (props.response.executionStatus !== 'NOT_DATA_QUESTION') return 'app-status-danger'
  return 'app-status-neutral'
})

const answerText = computed(() => {
  if (!props.response) {
    return '질문을 입력하면 AI 답변 요약과 조회 결과가 이 영역에 표시됩니다.'
  }
  if (props.response.clarificationRequired && props.response.clarificationQuestion) {
    return props.response.clarificationQuestion
  }
  return props.response.answer || '조회 결과를 확인해 주세요.'
})
</script>

<template>
  <div class="app-panel">
    <div class="app-panel-head">
      <h3 class="app-panel-title">
        <Bot class="app-panel-icon" />
        답변 요약
      </h3>
      <span class="app-status" :class="statusClass">
        {{ response?.executionStatus || 'READY' }}
      </span>
    </div>

    <div class="space-y-5 p-5">
      <div
        class="rounded-xl border p-4"
        :style="response?.clarificationRequired
          ? 'background-color: var(--color-warning-soft); border-color: var(--color-warning-border); color: var(--color-warning);'
          : 'background-color: var(--color-surface-muted); border-color: var(--color-border-muted); color: var(--color-text);'"
      >
        <div class="flex items-start gap-3">
          <AlertTriangle
            v-if="response?.clarificationRequired"
            class="mt-0.5 h-5 w-5 shrink-0"
          />
          <CheckCircle2
            v-else
            class="mt-0.5 h-5 w-5 shrink-0"
            style="color: var(--color-success);"
          />
          <p class="whitespace-pre-line text-sm leading-6">{{ answerText }}</p>
        </div>
      </div>

      <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <div class="rounded-lg border p-3" style="border-color: var(--color-border);">
          <div class="flex items-center gap-2">
            <Database class="h-4 w-4 app-table-muted" />
            <p class="app-stat-label-compact">조회 행 수</p>
          </div>
          <p class="app-stat-value-compact">{{ (response?.rowCount ?? 0).toLocaleString() }}</p>
        </div>

        <div class="rounded-lg border p-3" style="border-color: var(--color-border);">
          <div class="flex items-center gap-2">
            <Sparkles class="h-4 w-4 app-table-muted" />
            <p class="app-stat-label-compact">추천 차트</p>
          </div>
          <p class="app-stat-value-compact">{{ response?.chart?.type || '-' }}</p>
        </div>

        <div class="rounded-lg border p-3" style="border-color: var(--color-border);">
          <div class="flex items-center gap-2">
            <Bot class="h-4 w-4 app-table-muted" />
            <p class="app-stat-label-compact">질의 번호</p>
          </div>
          <p class="app-stat-value-compact">{{ response?.queryId ?? '-' }}</p>
        </div>
      </div>
    </div>
  </div>
</template>
