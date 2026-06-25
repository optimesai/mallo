<script setup lang="ts">
import { computed } from 'vue'
import { AlertTriangle, Bot, CheckCircle2, Database, Sparkles } from '@lucide/vue'
import type { AiQueryResponse } from '@/api/aiApi'
import AiMarkdownRenderer from '@/ui/AiMarkdownRenderer.vue'

const props = defineProps<{
  response?: AiQueryResponse | null
}>()

const emit = defineEmits<{
  selectSuggested: [question: string]
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

const chartTypeLabel = computed(() => {
  const type = props.response?.chart?.type
  if (!type) return '-'

  return {
    NONE: '없음',
    TABLE: '표',
    STAT: '지표',
    BAR: '막대',
    LINE: '선',
    DONUT: '도넛',
    HORIZONTAL_BAR: '가로 막대',
    STACKED_BAR: '누적 막대',
    AREA: '영역',
    COMBO: '복합',
    PARETO: '파레토'
  }[type] ?? type
})

const interpretationLabel = computed(() => {
  if (!props.response?.interpretedDomain || props.response.interpretedDomain === 'unknown') {
    return '-'
  }
  return `${props.response.interpretedDomain} / ${props.response.interpretedIntent || 'unknown'}`
})

const suggestedQuestions = computed(() => props.response?.suggestedQuestions ?? [])
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
          <AiMarkdownRenderer :content="answerText" />
        </div>
      </div>

      <div
        v-if="response?.interpretationSummary"
        class="rounded-lg border p-3"
        style="border-color: var(--color-border);"
      >
        <p class="app-stat-label-compact">질의 해석</p>
        <AiMarkdownRenderer :content="response.interpretationSummary" />
      </div>

      <div class="grid grid-cols-1 gap-3 sm:grid-cols-4">
        <div class="min-w-0 rounded-lg border p-3" style="border-color: var(--color-border);">
          <div class="flex items-center gap-2">
            <Database class="h-4 w-4 app-table-muted" />
            <p class="app-stat-label-compact">조회 행 수</p>
          </div>
          <p class="app-stat-value-compact min-w-0 break-words">{{ (response?.rowCount ?? 0).toLocaleString() }}</p>
        </div>

        <div class="min-w-0 rounded-lg border p-3" style="border-color: var(--color-border);">
          <div class="flex items-center gap-2">
            <Sparkles class="h-4 w-4 app-table-muted" />
            <p class="app-stat-label-compact">추천 차트</p>
          </div>
          <p class="app-stat-value-compact min-w-0 break-words app-type-lg leading-6">{{ chartTypeLabel }}</p>
        </div>

        <div class="min-w-0 rounded-lg border p-3" style="border-color: var(--color-border);">
          <div class="flex items-center gap-2">
            <Bot class="h-4 w-4 app-table-muted" />
            <p class="app-stat-label-compact">질의 번호</p>
          </div>
          <p class="app-stat-value-compact min-w-0 break-words">{{ response?.queryId ?? '-' }}</p>
        </div>

        <div class="min-w-0 rounded-lg border p-3" style="border-color: var(--color-border);">
          <div class="flex items-center gap-2">
            <Bot class="h-4 w-4 app-table-muted" />
            <p class="app-stat-label-compact">해석 도메인</p>
          </div>
          <p class="app-stat-value-compact min-w-0 break-words app-type-lg leading-6">{{ interpretationLabel }}</p>
        </div>
      </div>

      <div v-if="suggestedQuestions.length > 0" class="space-y-2">
        <p class="app-stat-label-compact">추천 후속 질문</p>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="suggestedQuestion in suggestedQuestions"
            :key="suggestedQuestion"
            type="button"
            class="app-status app-status-neutral"
            @click="emit('selectSuggested', suggestedQuestion)"
          >
            {{ suggestedQuestion }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
