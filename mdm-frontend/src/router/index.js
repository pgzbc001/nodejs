import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'

// 7 个业务路由（design.md 2.1）
const routes = [
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '首页统计' }
      },
      {
        path: 'models',
        name: 'models',
        component: () => import('../views/ModelList.vue'),
        meta: { title: '模型管理' }
      },
      {
        path: 'models/:id/design',
        name: 'modelDesigner',
        component: () => import('../views/ModelDesigner.vue'),
        meta: { title: '模型设计器', activeMenu: '/models' }
      },
      {
        path: 'data',
        name: 'data',
        component: () => import('../views/DataMaintenance.vue'),
        meta: { title: '数据维护' }
      },
      {
        path: 'push',
        name: 'push',
        component: () => import('../views/PushCenter.vue'),
        meta: { title: '推送中心' }
      },
      {
        path: 'quality',
        name: 'quality',
        component: () => import('../views/QualityRules.vue'),
        meta: { title: '质量规则' }
      },
      {
        path: 'logs',
        name: 'logs',
        component: () => import('../views/OperationLogs.vue'),
        meta: { title: '操作日志' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
