import { defineStore } from 'pinia'

// 演示角色（与后端 Role 枚举 / X-User-* 语义一致）
export const ROLES = [
  { value: 'MODEL_ADMIN', label: '数据管理员', userId: 'u_admin01', userName: 'model_admin' },
  { value: 'DATA_STAFF', label: '数据录入员', userId: 'u_staff01', userName: 'data_staff' },
  { value: 'DATA_AUDITOR', label: '数据审核员', userId: 'u_audit01', userName: 'data_auditor' },
  { value: 'SYS_ADMIN', label: '系统管理员', userId: 'u_sys01', userName: 'sys_admin' }
]

const ROLE_KEY = 'mdm_role'

/**
 * 用户/角色 Store：顶栏切换角色 → 持久化 localStorage → 注入 X-User-* 请求头。
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    role: localStorage.getItem(ROLE_KEY) || 'MODEL_ADMIN'
  }),
  getters: {
    current(state) {
      return ROLES.find((r) => r.value === state.role) || ROLES[0]
    },
    userId() {
      return this.current.userId
    },
    userName() {
      return this.current.userName
    },
    roleLabel() {
      return this.current.label
    },
    /** 当前角色是否在指定角色集合内：can('MODEL_ADMIN','SYS_ADMIN') */
    can(state) {
      return (...roles) => roles.includes(state.role)
    }
  },
  actions: {
    setRole(role) {
      this.role = role
      localStorage.setItem(ROLE_KEY, role)
    }
  }
})
