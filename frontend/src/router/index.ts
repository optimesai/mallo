import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import { useAuthStore } from '@/state/authStore'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: {
        guestOnly: true
      }
    },
    {
      path: '/signup',
      name: 'signup',
      component: () => import('../views/SignupView.vue'),
      meta: {
        guestOnly: true
      }
    },
    {
      path: '/',
      component: () => import('../layouts/DefaultLayout.vue'),
      meta: {
        requiresAuth: true
      },
      children: [
        {
          path: '',
          name: 'home',
          component: HomeView
        },
        {
          path: 'me',
          name: 'my-info',
          component: () => import('../views/MyInfoView.vue')
        },
        {
          path: 'system/users',
          name: 'system-users',
          component: () => import('../views/UserManagementView.vue')
        },
        {
          path: 'master/items',
          name: 'item-master',
          component: () => import('../views/ItemMasterView.vue')
        },
        {
          path: 'master/items/:id',
          name: 'item-master-detail',
          component: () => import('../views/ItemMasterDetailView.vue')
        },
        {
          path: 'master/partners',
          name: 'partner-master',
          component: () => import('../views/PartnerMasterView.vue')
        },
        {
          path: 'master/partners/:id',
          name: 'partner-master-detail',
          component: () => import('../views/PartnerMasterDetailView.vue')
        },
        {
          path: 'master/factory-lines',
          name: 'factory-line-master',
          component: () => import('../views/FactoryLineMasterView.vue')
        },
        {
          path: 'master/factory-lines/:id',
          name: 'factory-line-master-detail',
          component: () => import('../views/FactoryLineMasterDetailView.vue')
        },
        {
          path: 'master/boms',
          name: 'bom-master',
          component: () => import('../views/BomMasterView.vue')
        },
        {
          path: 'master/boms/:parentItemId',
          name: 'bom-master-detail',
          component: () => import('../views/BomMasterDetailView.vue')
        },
        {
          path: 'inbound/receipt',
          name: 'inbound-receipt',
          component: () => import('../views/InboundReceiptView.vue')
        },
        {
          path: 'inbound/stack',
          name: 'inbound-stack',
          component: () => import('../views/InboundStackView.vue')
        },
        {
          path: 'inventory/status',
          name: 'inventory-status',
          component: () => import('../views/InventoryStatusView.vue')
        },
        {
          path: 'inventory/history',
          name: 'inventory-history',
          component: () => import('../views/InventoryHistoryView.vue')
        },
        {
          path: 'production/work-orders',
          name: 'production-work-orders',
          component: () => import('../views/WorkOrderView.vue')
        },
        {
          path: 'production/work-orders/:id',
          name: 'production-work-order-detail',
          component: () => import('../views/WorkOrderDetailView.vue')
        },
        {
          path: 'production/issue',
          name: 'production-issue',
          component: () => import('../views/MaterialIssueView.vue')
        },
        {
          path: 'production/executions',
          name: 'production-executions',
          component: () => import('../views/ProductionExecutionView.vue')
        },
        {
          path: 'shipping/order',
          name: 'shipping-order',
          component: () => import('../views/ShippingOrderView.vue')
        },
        {
          path: 'shipping/picking',
          name: 'shipping-picking',
          component: () => import('../views/PickingView.vue')
        },
        {
          path: 'ai/queries',
          name: 'ai-queries',
          component: () => import('../views/AiDataChatbotView.vue')
        }
      ]
    }
  ]
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (!authStore.isInitialized) {
    await authStore.initializeAuth()
  }

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { name: 'login' }
  }

  if (to.meta.guestOnly && authStore.isLoggedIn) {
    return { name: 'home' }
  }

  return true
})

export default router
