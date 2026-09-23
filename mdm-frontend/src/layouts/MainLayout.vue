<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">主数据管理平台</div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon>
          <span>首页统计</span>
        </el-menu-item>
        <el-menu-item index="/models">
          <el-icon><Grid /></el-icon>
          <span>模型管理</span>
        </el-menu-item>
        <el-menu-item index="/data">
          <el-icon><Files /></el-icon>
          <span>数据维护</span>
        </el-menu-item>
        <el-menu-item index="/push">
          <el-icon><Promotion /></el-icon>
          <span>推送中心</span>
        </el-menu-item>
        <el-menu-item index="/quality">
          <el-icon><Checked /></el-icon>
          <span>质量规则</span>
        </el-menu-item>
        <el-menu-item index="/logs">
          <el-icon><Document /></el-icon>
          <span>操作日志</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="page-title">{{ pageTitle }}</div>
        <div class="user-area">
          <span class="text-muted">演示身份：</span>
          <el-select
            v-model="role"
            size="small"
            style="width: 150px"
            @change="onRoleChange"
          >
            <el-option
              v-for="item in ROLES"
              :key="item.value"
              :value="item.value"
              :label="item.label"
            />
          </el-select>
          <el-tag size="small" type="info">{{ userStore.userName }}</el-tag>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  DataBoard,
  Grid,
  Files,
  Promotion,
  Checked,
  Document
} from '@element-plus/icons-vue'
import { useUserStore, ROLES } from '../stores/user'

const route = useRoute()
const userStore = useUserStore()
const role = ref(userStore.role)

// 模型设计器等子页面通过 meta.activeMenu 指定菜单高亮项
const activeMenu = computed(() => route.meta.activeMenu || route.path)
const pageTitle = computed(() => route.meta.title || '主数据管理平台')

function onRoleChange(value) {
  userStore.setRole(value)
  ElMessage.success(`已切换为「${userStore.roleLabel}」（${userStore.userName}）`)
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  background: #001529;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
  background: #002140;
}

.menu {
  flex: 1;
  border-right: none;
  background: #001529;
}

.menu :deep(.el-menu-item) {
  color: #a6adb4;
}

.menu :deep(.el-menu-item:hover) {
  background: #112a45;
  color: #fff;
}

.menu :deep(.el-menu-item.is-active) {
  background: #1890ff;
  color: #fff;
}

.header {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 8px;
}

.main {
  padding: 0;
  overflow: auto;
  background: #f0f2f5;
}
</style>
