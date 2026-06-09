<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  AlertTriangle,
  ArrowLeft,
  Loader2,
  RefreshCw,
  Search,
  ShieldCheck,
  Trash2,
  UserCog,
  Users
} from '@lucide/vue'
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

const filteredUsers = computed(() => {
  const normalizedKeyword = appliedKeyword.value.trim().toLowerCase()

  return userStore.users.filter((user) => {
    const matchesRole = roleFilter.value === 'ALL' || user.role === roleFilter.value
    const matchesKeyword = !normalizedKeyword
      || user.employeeNo.toLowerCase().includes(normalizedKeyword)
      || user.userName.toLowerCase().includes(normalizedKeyword)
      || user.department.toLowerCase().includes(normalizedKeyword)

    return matchesRole && matchesKeyword
  })
})

const keywordSuggestions = computed(() => {
  const query = keywordInput.value.trim().toLowerCase()
  if (!query) return []

  const candidates = userStore.users.flatMap((user) => [
    { label: user.employeeNo, meta: `${user.userName} · ${user.department}`, user },
    { label: user.userName, meta: `${user.employeeNo} · ${getRoleLabel(user.role)}`, user },
    { label: user.department, meta: `${user.employeeNo} · ${user.userName}`, user }
  ])

  const unique = new Map<string, { label: string; meta: string; user: UserResponse }>()
  candidates.forEach((candidate) => {
    const key = `${candidate.label}-${candidate.user.userId}`
    if (candidate.label.toLowerCase().includes(query) && !unique.has(key)) {
      unique.set(key, candidate)
    }
  })

  return [...unique.values()].slice(0, 8)
})

const stats = computed(() => {
  const users = userStore.users
  return {
    total: users.length,
    admin: users.filter((user) => user.role === 'ADMIN').length,
    manager: users.filter((user) => user.role === 'MANAGER').length,
    worker: users.filter((user) => user.role === 'WORKER').length
  }
})

const isSelectedSelf = computed(() => {
  return Boolean(selectedUser.value && selectedUser.value.userId === currentUserId.value)
})

const isLastAdminSelected = computed(() => {
  return selectedUser.value?.role === 'ADMIN' && stats.value.admin <= 1
})

const canChangeSelectedRole = computed(() => {
  return Boolean(selectedUser.value && !isSelectedSelf.value && !userStore.isSaving)
})

const canDeleteSelectedUser = computed(() => {
  return Boolean(selectedUser.value && !isSelectedSelf.value && !isLastAdminSelected.value && !userStore.isSaving)
})

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

function backToList() {
  pageMode.value = 'list'
}

async function applyRoleChange() {
  if (!selectedUser.value || !pendingRole.value) return
  if (pendingRole.value === selectedUser.value.role) return

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
  if (role === 'ADMIN') return 'bg-rose-100 text-rose-700 border-rose-200'
  if (role === 'MANAGER') return 'bg-amber-100 text-amber-700 border-amber-200'
  return 'bg-sky-100 text-sky-700 border-sky-200'
}

function formatDateTime(dateTimeStr: string) {
  if (!dateTimeStr) return '-'
  const date = new Date(dateTimeStr)
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
  <section class="min-h-full bg-slate-100 p-6 text-slate-900">
    <div v-if="successToast" class="fixed right-6 top-20 z-40 rounded-xl bg-slate-950 px-4 py-3 text-sm font-semibold text-white shadow-2xl">
      {{ successToast }}
    </div>

    <div class="mb-6 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <div class="mb-2 inline-flex items-center gap-2 rounded-full border border-slate-300 bg-white px-3 py-1 text-xs font-bold text-slate-500">
          <ShieldCheck class="h-3.5 w-3.5 text-slate-700" />
          SYSTEM ADMINISTRATION
        </div>
        <h1 class="text-3xl font-black tracking-tight text-slate-950">사용자 및 권한</h1>
        <p class="mt-2 text-sm text-slate-500">가입된 사용자를 조회하고 시스템 접근 권한을 관리합니다.</p>
      </div>

      <button
        class="inline-flex items-center justify-center gap-2 rounded-xl bg-slate-950 px-4 py-2.5 text-sm font-bold text-white shadow-lg shadow-slate-300 transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
        type="button"
        :disabled="userStore.isLoading"
        @click="fetchUsers"
      >
        <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': userStore.isLoading }" />
        새로고침
      </button>
    </div>

    <div v-if="!isAdmin" class="rounded-3xl border border-amber-200 bg-amber-50 p-8 text-amber-900">
      <div class="mb-3 flex items-center gap-3 text-lg font-black">
        <AlertTriangle class="h-5 w-5" />
        관리자 권한이 필요합니다.
      </div>
      <p class="text-sm leading-6">이 화면은 시스템 관리자만 접근할 수 있습니다. 현재 계정으로는 사용자 목록과 권한 정보를 조회할 수 없습니다.</p>
    </div>

    <template v-else>
      <div class="mb-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <div class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div class="flex items-center justify-between text-sm font-bold text-slate-500">
            전체 사용자
            <Users class="h-5 w-5 text-slate-400" />
          </div>
          <div class="mt-3 text-3xl font-black text-slate-950">{{ stats.total }}</div>
        </div>
        <div class="rounded-2xl border border-rose-100 bg-rose-50 p-5 shadow-sm">
          <div class="text-sm font-bold text-rose-500">시스템 관리자 [ADMIN]</div>
          <div class="mt-3 text-3xl font-black text-rose-700">{{ stats.admin }}</div>
        </div>
        <div class="rounded-2xl border border-amber-100 bg-amber-50 p-5 shadow-sm">
          <div class="text-sm font-bold text-amber-600">현장 관리자 [MANAGER]</div>
          <div class="mt-3 text-3xl font-black text-amber-700">{{ stats.manager }}</div>
        </div>
        <div class="rounded-2xl border border-sky-100 bg-sky-50 p-5 shadow-sm">
          <div class="text-sm font-bold text-sky-600">현장 작업자 [WORKER]</div>
          <div class="mt-3 text-3xl font-black text-sky-700">{{ stats.worker }}</div>
        </div>
      </div>

      <p v-if="pageError" class="mb-4 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-bold text-rose-700">
        {{ pageError }}
      </p>

      <div v-if="pageMode === 'list'" class="space-y-5">
        <form class="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm" @submit.prevent="submitSearch">
          <div class="grid gap-3 lg:grid-cols-[1fr_220px_auto_auto]">
            <label class="relative block">
              <Search class="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                v-model="keywordInput"
                class="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 pl-11 pr-4 text-sm font-semibold outline-none transition focus:border-slate-500 focus:bg-white"
                placeholder="사번, 이름, 부서 검색"
                @focus="isSuggestOpen = true"
                @input="isSuggestOpen = true"
              >
              <div v-if="isSuggestOpen && keywordSuggestions.length > 0" class="absolute left-0 right-0 top-14 z-30 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl">
                <button
                  v-for="candidate in keywordSuggestions"
                  :key="`${candidate.label}-${candidate.user.userId}`"
                  class="flex w-full items-center justify-between gap-4 px-4 py-3 text-left transition hover:bg-slate-50"
                  type="button"
                  @mousedown.prevent="selectSuggestion(candidate.user)"
                >
                  <span>
                    <strong class="block text-sm font-black text-slate-900">{{ candidate.label }}</strong>
                    <span class="text-xs font-semibold text-slate-400">{{ candidate.meta }}</span>
                  </span>
                  <span class="rounded-full border px-2 py-1 text-[11px] font-black" :class="getRoleBadgeClass(candidate.user.role)">
                    {{ getRoleLabel(candidate.user.role) }}
                  </span>
                </button>
              </div>
            </label>
            <select
              v-model="roleFilter"
              class="h-12 rounded-2xl border border-slate-200 bg-slate-50 px-4 text-sm font-bold outline-none transition focus:border-slate-500 focus:bg-white"
            >
              <option value="ALL">전체 권한</option>
              <option value="ADMIN">시스템 관리자 [ADMIN]</option>
              <option value="MANAGER">현장 관리자 [MANAGER]</option>
              <option value="WORKER">현장 작업자 [WORKER]</option>
            </select>
            <button
              class="h-12 rounded-2xl bg-slate-950 px-6 text-sm font-black text-white transition hover:bg-slate-800"
              type="submit"
            >
              검색
            </button>
            <button
              class="h-12 rounded-2xl border border-slate-200 px-5 text-sm font-bold text-slate-600 transition hover:bg-slate-100"
              type="button"
              @click="resetFilters"
            >
              초기화
            </button>
          </div>
          <p class="mt-3 text-xs font-semibold text-slate-400">검색어는 검색 버튼 또는 Enter 입력 후 목록에 반영됩니다.</p>
        </form>

        <div class="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
          <div class="flex items-center justify-between border-b border-slate-100 px-5 py-4">
            <div>
              <h2 class="text-lg font-black text-slate-950">사용자 목록</h2>
              <p class="mt-1 text-xs font-semibold text-slate-400">검색 결과 {{ filteredUsers.length }}명</p>
            </div>
          </div>

          <div v-if="userStore.isLoading" class="flex h-72 items-center justify-center gap-3 text-sm font-bold text-slate-500">
            <Loader2 class="h-5 w-5 animate-spin" />
            사용자 정보를 불러오는 중입니다.
          </div>

          <div v-else class="overflow-x-auto">
            <table class="w-full min-w-[860px] text-left text-sm">
              <thead class="bg-slate-50 text-xs uppercase tracking-wide text-slate-400">
                <tr>
                  <th class="px-5 py-3 font-black">사번</th>
                  <th class="px-5 py-3 font-black">이름</th>
                  <th class="px-5 py-3 font-black">부서</th>
                  <th class="px-5 py-3 font-black">권한</th>
                  <th class="px-5 py-3 font-black">가입일</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                <tr
                  v-for="user in filteredUsers"
                  :key="user.userId"
                  class="cursor-pointer transition hover:bg-slate-50"
                  @click="openUserDetail(user)"
                >
                  <td class="px-5 py-4 font-black text-slate-900">{{ user.employeeNo }}</td>
                  <td class="px-5 py-4 font-semibold text-slate-700">{{ user.userName }}</td>
                  <td class="px-5 py-4 text-slate-500">{{ user.department }}</td>
                  <td class="px-5 py-4">
                    <span class="inline-flex rounded-full border px-2.5 py-1 text-xs font-black" :class="getRoleBadgeClass(user.role)">
                      {{ getRoleLabel(user.role) }}
                    </span>
                  </td>
                  <td class="px-5 py-4 text-xs font-semibold text-slate-400">{{ formatDateTime(user.createdAt) }}</td>
                </tr>
              </tbody>
            </table>

            <div v-if="filteredUsers.length === 0" class="px-5 py-16 text-center text-sm font-bold text-slate-400">
              조건에 맞는 사용자가 없습니다.
            </div>
          </div>
        </div>
      </div>

      <div v-else class="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
        <button
          class="mb-5 inline-flex items-center gap-2 rounded-xl border border-slate-200 px-4 py-2 text-sm font-black text-slate-600 transition hover:bg-slate-50"
          type="button"
          @click="backToList"
        >
          <ArrowLeft class="h-4 w-4" />
          목록으로
        </button>

        <div v-if="!selectedUser" class="rounded-2xl border border-dashed border-slate-200 bg-slate-50 p-6 text-sm font-semibold leading-6 text-slate-400">
          선택된 사용자가 없습니다.
        </div>

        <div v-else class="grid gap-5 xl:grid-cols-[360px_minmax(0,1fr)]">
          <div class="rounded-3xl bg-slate-950 p-6 text-white">
            <div class="mb-5 flex h-14 w-14 items-center justify-center rounded-2xl bg-white/10">
              <UserCog class="h-7 w-7" />
            </div>
            <div class="text-xs font-black text-slate-400">{{ selectedUser.employeeNo }}</div>
            <div class="mt-2 text-3xl font-black">{{ selectedUser.userName }}</div>
            <div class="mt-2 text-sm font-semibold text-slate-300">{{ selectedUser.department }}</div>
            <span class="mt-5 inline-flex rounded-full bg-white/10 px-3 py-1 text-xs font-black">
              {{ getRoleLabel(selectedUser.role) }}
            </span>
          </div>

          <div class="space-y-5">
            <div class="grid gap-3 md:grid-cols-3">
              <div class="rounded-2xl border border-slate-100 p-4">
                <div class="text-xs font-black text-slate-400">권한 설명</div>
                <div class="mt-2 text-sm font-semibold text-slate-700">{{ getRoleDescription(selectedUser.role) }}</div>
              </div>
              <div class="rounded-2xl border border-slate-100 p-4">
                <div class="text-xs font-black text-slate-400">가입일</div>
                <div class="mt-2 text-sm font-semibold text-slate-700">{{ formatDateTime(selectedUser.createdAt) }}</div>
              </div>
              <div class="rounded-2xl border border-slate-100 p-4">
                <div class="text-xs font-black text-slate-400">수정일</div>
                <div class="mt-2 text-sm font-semibold text-slate-700">{{ formatDateTime(selectedUser.updatedAt) }}</div>
              </div>
            </div>

            <div class="rounded-2xl border border-slate-200 p-5">
              <label class="text-xs font-black text-slate-400" for="role-select">권한 변경</label>
              <select
                id="role-select"
                v-model="pendingRole"
                class="mt-2 h-12 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 text-sm font-bold outline-none transition focus:border-slate-500 focus:bg-white disabled:cursor-not-allowed disabled:opacity-60"
                :disabled="!canChangeSelectedRole"
              >
                <option v-for="role in roleOptions" :key="role.value" :value="role.value">
                  {{ role.label }}
                </option>
              </select>
              <p v-if="isSelectedSelf" class="mt-2 text-xs font-bold text-amber-600">본인 권한은 직접 변경할 수 없습니다.</p>
              <p v-else-if="isLastAdminSelected" class="mt-2 text-xs font-bold text-amber-600">마지막 관리자는 다른 권한으로 변경할 수 없습니다.</p>
              <button
                class="mt-3 flex h-12 w-full items-center justify-center rounded-xl bg-slate-950 text-sm font-black text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
                type="button"
                :disabled="!canChangeSelectedRole || pendingRole === selectedUser.role"
                @click="applyRoleChange"
              >
                권한 저장
              </button>
            </div>

            <div class="rounded-2xl border border-rose-100 bg-rose-50 p-5">
              <div class="text-sm font-black text-rose-700">사용자 삭제</div>
              <p class="mt-1 text-xs font-semibold text-rose-500">삭제된 사용자는 시스템에 로그인할 수 없습니다.</p>
              <button
                class="mt-4 flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-rose-600 text-sm font-black text-white transition hover:bg-rose-700 disabled:cursor-not-allowed disabled:opacity-50"
                type="button"
                :disabled="!canDeleteSelectedUser"
                @click="requestDelete(selectedUser)"
              >
                <Trash2 class="h-4 w-4" />
                사용자 삭제
              </button>
              <p v-if="isSelectedSelf" class="mt-2 text-xs font-bold text-amber-600">본인 계정은 직접 삭제할 수 없습니다.</p>
              <p v-else-if="isLastAdminSelected" class="mt-2 text-xs font-bold text-amber-600">마지막 관리자 계정은 삭제할 수 없습니다.</p>
            </div>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>
