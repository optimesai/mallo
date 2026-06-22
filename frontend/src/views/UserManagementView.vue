<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { AlertTriangle, ArrowLeft, Loader2, RefreshCw, Search, ShieldCheck, Trash2, UserCog, Users } from '@lucide/vue'
import { useAuthStore } from '@/state/authStore'
import { useUserStore } from '@/state/userStore'
import type { UserResponse } from '@/api/authApi'
import type { UserRole } from '@/api/userApi'

const authStore = useAuthStore()
const userStore = useUserStore()

const roleOptions: Array<{ value: UserRole; label: string; description: string }> = [
  { value: 'WORKER', label: '현장 작업자 [WORKER]', description: '입고, 재고, 생산, 출고 업무 입력 권한' },
  { value: 'MANAGER', label: '현장 관리자 [MANAGER]', description: '작업자 권한과 기준정보 등록/수정 권한' },
  { value: 'ADMIN', label: '시스템 관리자 [ADMIN]', description: '사용자 및 권한 관리 권한' }
]

const keywordInput = ref('')
const appliedKeyword = ref('')
const roleFilter = ref<'ALL' | UserRole>('ALL')
const pendingRole = ref<UserRole | ''>('')
const pageMode = ref<'list' | 'detail'>('list')
const isSuggestOpen = ref(false)
const pageError = ref<string | null>(null)
const successToast = ref<string | null>(null)

const currentUserId = computed(() => authStore.user?.userId ?? null)
const isAdmin = computed(() => authStore.user?.role === 'ADMIN')
const selectedUser = computed(() => userStore.selectedUser)

const stats = computed(() => {
  const users = userStore.users
  return {
    total: users.length,
    admin: users.filter((user) => user.role === 'ADMIN').length,
    manager: users.filter((user) => user.role === 'MANAGER').length,
    worker: users.filter((user) => user.role === 'WORKER').length
  }
})

const filteredUsers = computed(() => {
  const keyword = appliedKeyword.value.trim().toLowerCase()
  return userStore.users.filter((user) => {
    const matchesRole = roleFilter.value === 'ALL' || user.role === roleFilter.value
    const matchesKeyword = !keyword
      || user.employeeNo.toLowerCase().includes(keyword)
      || user.userName.toLowerCase().includes(keyword)
      || user.department.toLowerCase().includes(keyword)
    return matchesRole && matchesKeyword
  })
})

const keywordSuggestions = computed(() => {
  const keyword = keywordInput.value.trim().toLowerCase()
  if (!keyword) return []

  const candidates = userStore.users.flatMap((user) => [
    { label: user.employeeNo, meta: `${user.userName} · ${user.department}`, user },
    { label: user.userName, meta: `${user.employeeNo} · ${getRoleLabel(user.role)}`, user },
    { label: user.department, meta: `${user.employeeNo} · ${user.userName}`, user }
  ])
  const unique = new Map<string, { label: string; meta: string; user: UserResponse }>()

  candidates.forEach((candidate) => {
    const key = `${candidate.label}-${candidate.user.userId}`
    if (candidate.label.toLowerCase().includes(keyword) && !unique.has(key)) unique.set(key, candidate)
  })

  return [...unique.values()].slice(0, 8)
})

const isSelectedSelf = computed(() => Boolean(selectedUser.value && selectedUser.value.userId === currentUserId.value))
const isLastAdminSelected = computed(() => selectedUser.value?.role === 'ADMIN' && stats.value.admin <= 1)
const canChangeSelectedRole = computed(() => Boolean(selectedUser.value && !isSelectedSelf.value && !userStore.isSaving))
const canDeleteSelectedUser = computed(() => Boolean(selectedUser.value && !isSelectedSelf.value && !isLastAdminSelected.value && !userStore.isSaving))

onMounted(async () => {
  if (!isAdmin.value) return
  await fetchUsers()
})

async function fetchUsers() {
  try {
    pageError.value = null
    await userStore.loadUsers()
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '사용자 목록을 불러오지 못했습니다.'
  }
}

function submitSearch() {
  appliedKeyword.value = keywordInput.value.trim()
  isSuggestOpen.value = false
}

function resetFilters() {
  keywordInput.value = ''
  appliedKeyword.value = ''
  roleFilter.value = 'ALL'
  isSuggestOpen.value = false
}

async function selectSuggestion(user: UserResponse) {
  keywordInput.value = user.employeeNo
  appliedKeyword.value = user.employeeNo
  isSuggestOpen.value = false
  await openUserDetail(user)
}

async function openUserDetail(user: UserResponse) {
  try {
    pageError.value = null
    await userStore.loadUser(user.userId)
    pendingRole.value = userStore.selectedUser?.role || ''
    pageMode.value = 'detail'
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '사용자 상세 정보를 불러오지 못했습니다.'
  }
}

async function applyRoleChange() {
  if (!selectedUser.value || !pendingRole.value || pendingRole.value === selectedUser.value.role) return

  try {
    pageError.value = null
    await userStore.updateUserRole(selectedUser.value.userId, pendingRole.value)
    showToast('사용자 권한이 수정되었습니다.')
    pageMode.value = 'list'
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '사용자 권한을 수정하지 못했습니다.'
    pendingRole.value = selectedUser.value.role
  }
}

async function requestDelete(user: UserResponse) {
  if (!confirm(`[${user.employeeNo}] ${user.userName} 사용자를 삭제하시겠습니까?`)) return

  try {
    pageError.value = null
    await userStore.deleteUser(user.userId)
    showToast('사용자가 삭제되었습니다.')
    pageMode.value = 'list'
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '사용자를 삭제하지 못했습니다.'
  }
}

function getRoleLabel(role: UserRole) {
  return roleOptions.find((option) => option.value === role)?.label || role
}

function getRoleDescription(role: UserRole) {
  return roleOptions.find((option) => option.value === role)?.description || '-'
}

function getRoleBadgeClass(role: UserRole) {
  if (role === 'ADMIN') return 'app-status-role-admin'
  if (role === 'MANAGER') return 'app-status-role-manager'
  return 'app-status-role-worker'
}

function formatDateTime(value: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function showToast(message: string) {
  successToast.value = message
  window.setTimeout(() => {
    successToast.value = null
  }, 2200)
}
</script>

<template>
  <section class="min-h-full app-bg-muted p-6 app-text-strong">
    <div v-if="successToast" class="fixed right-6 top-20 z-40 rounded-xl app-bg-strong px-4 py-3 app-type-sm app-font-label app-text-inverse shadow-2xl">
      {{ successToast }}
    </div>

    <div class="mb-6 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <div class="mb-2 inline-flex items-center gap-2 rounded-full border app-border-strong app-bg-surface px-3 py-1 app-type-xs app-font-strong app-text-muted">
          <ShieldCheck class="h-3.5 w-3.5 app-text-soft" />
          SYSTEM ADMINISTRATION
        </div>
        <h1 class="app-type-3xl app-font-emphasis tracking-tight app-text-strong">사용자 및 권한</h1>
        <p class="mt-2 app-type-sm app-text-muted">가입된 사용자를 조회하고 시스템 접근 권한을 관리합니다.</p>
      </div>

      <button v-if="isAdmin" class="inline-flex items-center justify-center gap-2 rounded-xl app-bg-strong px-4 py-2.5 app-type-sm app-font-strong app-text-inverse shadow-lg  transition app-hover-muted disabled:cursor-not-allowed disabled:opacity-60" type="button" :disabled="userStore.isLoading" @click="fetchUsers">
        <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': userStore.isLoading }" />
        새로고침
      </button>
    </div>

    <div v-if="!isAdmin" class="rounded-3xl border app-border app-bg-warning-soft p-8 app-text-warning">
      <div class="mb-3 flex items-center gap-3 app-type-lg app-font-emphasis">
        <AlertTriangle class="h-5 w-5" />
        접근 불가능한 메뉴입니다.
      </div>
      <p class="app-type-sm leading-6">사용자 및 권한 관리는 시스템 관리자 [ADMIN]만 접근할 수 있습니다. 현재 계정 권한으로는 사용자 목록과 권한 정보를 조회할 수 없습니다.</p>
    </div>

    <template v-else>
      <div class="mb-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <div class="rounded-2xl border app-border app-bg-surface p-5 shadow-sm">
          <div class="flex items-center justify-between app-type-sm app-font-strong app-text-muted">전체 사용자 <Users class="h-5 w-5 app-text-muted" /></div>
          <div class="mt-3 app-type-3xl app-font-emphasis app-text-strong">{{ stats.total }}</div>
        </div>
        <div class="rounded-2xl border app-border-muted app-bg-danger-soft p-5 shadow-sm">
          <div class="app-type-sm app-font-strong app-text-danger">시스템 관리자 [ADMIN]</div>
          <div class="mt-3 app-type-3xl app-font-emphasis app-text-danger">{{ stats.admin }}</div>
        </div>
        <div class="rounded-2xl border app-border-muted app-bg-warning-soft p-5 shadow-sm">
          <div class="app-type-sm app-font-strong app-text-warning">현장 관리자 [MANAGER]</div>
          <div class="mt-3 app-type-3xl app-font-emphasis app-text-warning">{{ stats.manager }}</div>
        </div>
        <div class="rounded-2xl border app-border-muted app-bg-primary-soft p-5 shadow-sm">
          <div class="app-type-sm app-font-strong app-accent">현장 작업자 [WORKER]</div>
          <div class="mt-3 app-type-3xl app-font-emphasis app-accent">{{ stats.worker }}</div>
        </div>
      </div>

      <p v-if="pageError" class="mb-4 rounded-2xl border app-border app-bg-danger-soft px-4 py-3 app-type-sm app-font-strong app-text-danger">{{ pageError }}</p>

      <div v-if="pageMode === 'list'" class="space-y-5">
        <form class="rounded-3xl border app-border app-bg-surface p-5 shadow-sm" @submit.prevent="submitSearch">
          <div class="grid gap-3 lg:grid-cols-[1fr_260px_auto_auto]">
            <label class="relative block">
              <Search class="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 app-text-muted" />
              <input v-model="keywordInput" class="h-12 w-full rounded-2xl border app-border app-bg-muted pl-11 pr-4 app-type-sm app-font-label outline-none transition  " placeholder="사번, 이름, 부서 검색" @focus="isSuggestOpen = true" @input="isSuggestOpen = true">
              <div v-if="isSuggestOpen && keywordSuggestions.length > 0" class="absolute left-0 right-0 top-14 z-30 overflow-hidden rounded-2xl border app-border app-bg-surface shadow-2xl">
                <button v-for="candidate in keywordSuggestions" :key="`${candidate.label}-${candidate.user.userId}`" class="flex w-full items-center justify-between gap-4 px-4 py-3 text-left transition app-hover-muted" type="button" @mousedown.prevent="selectSuggestion(candidate.user)">
                  <span>
                    <strong class="block app-type-sm app-font-emphasis app-text-strong">{{ candidate.label }}</strong>
                    <span class="app-type-xs app-font-label app-text-muted">{{ candidate.meta }}</span>
                  </span>
                  <span class="rounded-full border px-2 py-1 app-type-11 app-font-emphasis" :class="getRoleBadgeClass(candidate.user.role)">{{ getRoleLabel(candidate.user.role) }}</span>
                </button>
              </div>
            </label>
            <select v-model="roleFilter" class="h-12 rounded-2xl border app-border app-bg-muted px-4 app-type-sm app-font-strong outline-none transition  ">
              <option value="ALL">전체 권한</option>
              <option value="ADMIN">시스템 관리자 [ADMIN]</option>
              <option value="MANAGER">현장 관리자 [MANAGER]</option>
              <option value="WORKER">현장 작업자 [WORKER]</option>
            </select>
            <button class="h-12 rounded-2xl app-bg-strong px-6 app-type-sm app-font-emphasis app-text-inverse transition app-hover-muted" type="submit">검색</button>
            <button class="h-12 rounded-2xl border app-border px-5 app-type-sm app-font-strong app-text-soft transition app-hover-muted" type="button" @click="resetFilters">초기화</button>
          </div>
          <p class="mt-3 app-type-xs app-font-label app-text-muted">검색어는 검색 버튼 또는 Enter 입력 후 목록에 반영됩니다.</p>
        </form>

        <div class="overflow-hidden rounded-3xl border app-border app-bg-surface shadow-sm">
          <div class="flex items-center justify-between border-b app-border-muted px-5 py-4">
            <div>
              <h2 class="app-type-lg app-font-emphasis app-text-strong">사용자 목록</h2>
              <p class="mt-1 app-type-xs app-font-label app-text-muted">검색 결과 {{ filteredUsers.length }}명</p>
            </div>
          </div>
          <div v-if="userStore.isLoading" class="flex h-72 items-center justify-center gap-3 app-type-sm app-font-strong app-text-muted"><Loader2 class="h-5 w-5 animate-spin" />사용자 정보를 불러오는 중입니다.</div>
          <div v-else class="overflow-x-auto">
            <table class="w-full min-w-[860px] text-left app-type-sm">
              <thead class="app-bg-muted app-type-xs uppercase tracking-wide app-text-muted">
                <tr><th class="px-5 py-3 app-font-emphasis">사번</th><th class="px-5 py-3 app-font-emphasis">이름</th><th class="px-5 py-3 app-font-emphasis">부서</th><th class="px-5 py-3 app-font-emphasis">권한</th><th class="px-5 py-3 app-font-emphasis">가입일</th></tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                <tr v-for="targetUser in filteredUsers" :key="targetUser.userId" class="cursor-pointer transition app-hover-muted" @click="openUserDetail(targetUser)">
                  <td class="px-5 py-4 app-font-emphasis app-text-strong">{{ targetUser.employeeNo }}</td>
                  <td class="px-5 py-4 app-font-label app-text-soft">{{ targetUser.userName }}</td>
                  <td class="px-5 py-4 app-text-muted">{{ targetUser.department }}</td>
                  <td class="px-5 py-4"><span class="inline-flex rounded-full border px-2.5 py-1 app-type-xs app-font-emphasis" :class="getRoleBadgeClass(targetUser.role)">{{ getRoleLabel(targetUser.role) }}</span></td>
                  <td class="px-5 py-4 app-type-xs app-font-label app-text-muted">{{ formatDateTime(targetUser.createdAt) }}</td>
                </tr>
              </tbody>
            </table>
            <div v-if="filteredUsers.length === 0" class="px-5 py-16 text-center app-type-sm app-font-strong app-text-muted">조건에 맞는 사용자가 없습니다.</div>
          </div>
        </div>
      </div>

      <div v-else class="rounded-3xl border app-border app-bg-surface p-5 shadow-sm">
        <button class="mb-5 inline-flex items-center gap-2 rounded-xl border app-border px-4 py-2 app-type-sm app-font-emphasis app-text-soft transition app-hover-muted" type="button" @click="pageMode = 'list'"><ArrowLeft class="h-4 w-4" />목록으로</button>
        <div v-if="selectedUser" class="grid gap-5 xl:grid-cols-[360px_minmax(0,1fr)]">
          <div class="rounded-3xl app-bg-strong p-6 app-text-inverse">
            <div class="mb-5 flex h-14 w-14 items-center justify-center rounded-2xl app-bg-muted"><UserCog class="h-7 w-7" /></div>
            <div class="app-type-xs app-font-emphasis app-text-muted">{{ selectedUser.employeeNo }}</div>
            <div class="mt-2 app-type-3xl app-font-emphasis">{{ selectedUser.userName }}</div>
            <div class="mt-2 app-type-sm app-font-label app-text-subtle">{{ selectedUser.department }}</div>
            <span class="mt-5 inline-flex rounded-full app-bg-muted px-3 py-1 app-type-xs app-font-emphasis">{{ getRoleLabel(selectedUser.role) }}</span>
          </div>
          <div class="space-y-5">
            <div class="grid gap-3 md:grid-cols-3">
              <div class="rounded-2xl border app-border-muted p-4"><div class="app-type-xs app-font-emphasis app-text-muted">권한 설명</div><div class="mt-2 app-type-sm app-font-label app-text-soft">{{ getRoleDescription(selectedUser.role) }}</div></div>
              <div class="rounded-2xl border app-border-muted p-4"><div class="app-type-xs app-font-emphasis app-text-muted">가입일</div><div class="mt-2 app-type-sm app-font-label app-text-soft">{{ formatDateTime(selectedUser.createdAt) }}</div></div>
              <div class="rounded-2xl border app-border-muted p-4"><div class="app-type-xs app-font-emphasis app-text-muted">수정일</div><div class="mt-2 app-type-sm app-font-label app-text-soft">{{ formatDateTime(selectedUser.updatedAt) }}</div></div>
            </div>
            <div class="rounded-2xl border app-border p-5">
              <label class="app-type-xs app-font-emphasis app-text-muted" for="role-select">권한 변경</label>
              <select id="role-select" v-model="pendingRole" class="mt-2 h-12 w-full rounded-xl border app-border app-bg-muted px-3 app-type-sm app-font-strong outline-none transition   disabled:cursor-not-allowed disabled:opacity-60" :disabled="!canChangeSelectedRole">
                <option v-for="role in roleOptions" :key="role.value" :value="role.value">{{ role.label }}</option>
              </select>
              <p v-if="isSelectedSelf" class="mt-2 app-type-xs app-font-strong app-text-warning">본인 권한은 직접 변경할 수 없습니다.</p>
              <p v-else-if="isLastAdminSelected" class="mt-2 app-type-xs app-font-strong app-text-warning">마지막 관리자는 다른 권한으로 변경할 수 없습니다.</p>
              <button class="mt-3 flex h-12 w-full items-center justify-center rounded-xl app-bg-strong app-type-sm app-font-emphasis app-text-inverse transition app-hover-muted disabled:cursor-not-allowed disabled:opacity-50" type="button" :disabled="!canChangeSelectedRole || pendingRole === selectedUser.role" @click="applyRoleChange">권한 저장</button>
            </div>
            <div class="rounded-2xl border app-border-muted app-bg-danger-soft p-5">
              <div class="app-type-sm app-font-emphasis app-text-danger">사용자 삭제</div>
              <p class="mt-1 app-type-xs app-font-label app-text-danger">삭제된 사용자는 시스템에 로그인할 수 없습니다.</p>
              <button class="mt-4 flex h-12 w-full items-center justify-center gap-2 rounded-xl app-bg-danger app-type-sm app-font-emphasis app-text-inverse transition app-hover-muted disabled:cursor-not-allowed disabled:opacity-50" type="button" :disabled="!canDeleteSelectedUser" @click="requestDelete(selectedUser)"><Trash2 class="h-4 w-4" />사용자 삭제</button>
              <p v-if="isSelectedSelf" class="mt-2 app-type-xs app-font-strong app-text-warning">본인 계정은 직접 삭제할 수 없습니다.</p>
              <p v-else-if="isLastAdminSelected" class="mt-2 app-type-xs app-font-strong app-text-warning">마지막 관리자 계정은 삭제할 수 없습니다.</p>
            </div>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>
