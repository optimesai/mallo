<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  AlertTriangle,
  Bot,
  LoaderCircle,
  MessageSquare,
  Send,
  Sparkles,
  Trash2
} from '@lucide/vue'
import { useAiStore } from '@/state/aiStore'
import AiAnswerSummary from '@/ui/AiAnswerSummary.vue'
import AiChartPanel from '@/ui/AiChartPanel.vue'
import AiResultTable from '@/ui/AiResultTable.vue'
import AiSqlPanel from '@/ui/AiSqlPanel.vue'

const aiStore = useAiStore()
const route = useRoute()

const question = ref('')
const messageListRef = ref<HTMLElement | null>(null)
const lastAutoQuestion = ref('')

const examples = [
  '안전재고 미만 품목을 보여줘',
  '최근 7일 입고 수량 추이를 알려줘',
  '출하 대기 건수를 거래처별로 집계해줘',
  '현재 재고 수량이 가장 많은 품목 10개를 알려줘'
]

const canSubmit = computed(() => question.value.trim().length > 0 && !aiStore.isLoading)
const isTablePresentation = computed(() => aiStore.currentResponse?.chart?.type === 'TABLE')

async function submitQuestion() {
  if (!canSubmit.value) return

  const submittedQuestion = question.value
  question.value = ''

  try {
    await aiStore.ask(submittedQuestion)
  } finally {
    await scrollToBottom()
  }
}

async function useExample(example: string) {
  question.value = example
  await submitQuestion()
}

onMounted(() => {
  submitRouteQuestion()
})

watch(() => route.query.question, () => {
  submitRouteQuestion()
})

async function submitRouteQuestion() {
  const routeQuestion = getRouteQuestion()
  if (!routeQuestion || routeQuestion === lastAutoQuestion.value) return

  lastAutoQuestion.value = routeQuestion
  question.value = routeQuestion
  await submitQuestion()
}

function getRouteQuestion() {
  const value = route.query.question
  if (Array.isArray(value)) return value[0]?.trim() ?? ''
  return value?.trim() ?? ''
}

async function scrollToBottom() {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

function formatTime(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}
</script>

<template>
  <div class="app-page">
    <div class="app-page-header">
      <div>
        <h1 class="app-page-title">AI 데이터 챗봇</h1>
        <p class="app-page-subtitle">업무 데이터를 자연어로 질의하고 조회 결과, 차트, 생성 SQL을 함께 확인합니다.</p>
      </div>
      <button
        type="button"
        class="app-button app-button-muted"
        :disabled="aiStore.messages.length === 0 && !aiStore.currentResponse"
        @click="aiStore.clearMessages"
      >
        <Trash2 class="h-4 w-4" />
        대화 초기화
      </button>
    </div>

    <div
      v-if="aiStore.error"
      class="app-alert app-alert-danger"
    >
      <AlertTriangle class="h-5 w-5 shrink-0" />
      <div>
        <h4 class="app-alert-title">AI 질의 오류</h4>
        <p class="app-alert-text">{{ aiStore.error }}</p>
      </div>
    </div>

    <div class="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)]">
      <section class="space-y-6">
        <div class="app-panel flex h-[680px] max-h-[calc(100vh-12rem)] min-h-[560px] flex-col">
          <div class="app-panel-head">
            <h2 class="app-panel-title">
              <MessageSquare class="app-panel-icon" />
              질의 대화
            </h2>
            <span class="app-type-xs app-table-muted">{{ aiStore.messages.length.toLocaleString() }}개 메시지</span>
          </div>

          <div
            ref="messageListRef"
            class="min-h-0 flex-1 space-y-4 overflow-y-auto p-5"
          >
            <div
              v-if="aiStore.messages.length === 0"
              class="flex h-full min-h-[320px] flex-col items-center justify-center gap-4 text-center"
            >
              <div class="rounded-xl p-4" style="background-color: var(--color-primary-soft); color: var(--color-primary);">
                <Bot class="h-9 w-9" />
              </div>
              <div>
                <p class="app-type-base app-table-main">업무 데이터를 질문해 주세요.</p>
                <p class="mt-1 app-type-sm app-table-muted">재고, 입고, 출하, 생산 데이터를 자연어로 조회할 수 있습니다.</p>
              </div>
            </div>

            <button
              v-for="message in aiStore.messages"
              :key="message.id"
              type="button"
              class="flex w-full cursor-default gap-3 text-left"
              :class="{ 'justify-end': message.role === 'user' }"
              @click="message.response && aiStore.selectResponse(message.response)"
            >
              <div
                v-if="message.role === 'assistant'"
                class="mt-1 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg"
                style="background-color: var(--color-primary-soft); color: var(--color-primary);"
              >
                <Bot class="h-4 w-4" />
              </div>

              <div
                class="max-w-[82%] rounded-xl border px-4 py-3 shadow-sm"
                :style="message.role === 'user'
                  ? 'background-color: var(--color-primary); border-color: var(--color-primary); color: var(--color-text-inverse);'
                  : 'background-color: var(--color-surface); border-color: var(--color-border); color: var(--color-text);'"
              >
                <p class="whitespace-pre-line app-type-sm leading-10">{{ message.content }}</p>
                <div class="mt-2 flex items-center gap-2 app-type-11 opacity-70">
                  <span>{{ formatTime(message.createdAt) }}</span>
                  <span v-if="message.response" class="app-status app-status-neutral">
                    {{ message.response.rowCount.toLocaleString() }}건
                  </span>
                </div>
              </div>
            </button>

            <div v-if="aiStore.isLoading" class="flex gap-3">
              <div
                class="mt-1 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg"
                style="background-color: var(--color-primary-soft); color: var(--color-primary);"
              >
                <LoaderCircle class="h-4 w-4 animate-spin" />
              </div>
              <div class="rounded-xl border px-4 py-3 app-type-sm app-table-muted" style="border-color: var(--color-border);">
                데이터를 조회하고 있습니다.
              </div>
            </div>
          </div>

          <div class="shrink-0 space-y-4 border-t p-5" style="border-color: var(--color-border-muted);">
            <div class="flex flex-wrap gap-2">
              <button
                v-for="example in examples"
                :key="example"
                type="button"
                class="app-button app-button-subtle h-8"
                :disabled="aiStore.isLoading"
                @click="useExample(example)"
              >
                <Sparkles class="h-3.5 w-3.5" />
                {{ example }}
              </button>
            </div>

            <form class="flex gap-2" @submit.prevent="submitQuestion">
              <input
                v-model="question"
                class="app-control h-12"
                type="text"
                placeholder="예: 이번 달 출하 대기 건수를 거래처별로 보여줘"
                :disabled="aiStore.isLoading"
              />
              <button
                type="submit"
                class="app-button app-button-primary h-12 shrink-0"
                :disabled="!canSubmit"
              >
                <Send class="h-4 w-4" />
                전송
              </button>
            </form>
          </div>
        </div>

        <AiAnswerSummary :response="aiStore.currentResponse" />
      </section>

      <section class="space-y-6">
        <AiResultTable
          v-if="isTablePresentation"
          :rows="aiStore.currentResponse?.rows ?? []"
        />
        <AiChartPanel
          :chart="aiStore.currentResponse?.chart"
          :rows="aiStore.currentResponse?.rows ?? []"
        />
        <AiResultTable
          v-if="!isTablePresentation"
          :rows="aiStore.currentResponse?.rows ?? []"
        />
        <AiSqlPanel :sql="aiStore.currentResponse?.generatedSql" />
      </section>
    </div>
  </div>
</template>
