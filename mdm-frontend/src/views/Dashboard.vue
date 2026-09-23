<template>
  <div class="page-container">
    <el-row :gutter="12">
      <el-col v-for="card in cards" :key="card.label" :span="6" class="card-col">
        <div class="page-card stat-card">
          <el-icon class="stat-icon" :style="{ color: card.color }">
            <component :is="card.icon" />
          </el-icon>
          <div class="stat-body">
            <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-sub text-muted">{{ card.sub }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="page-card recent">
      <div class="recent-header">
        <span class="recent-title">最近操作</span>
        <el-button size="small" @click="load">刷新</el-button>
      </div>
      <el-table v-loading="loading" :data="logs" size="small" border>
        <el-table-column prop="operatedTime" label="时间" width="170" />
        <el-table-column label="业务类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="BIZ_TAG[row.bizType] || 'info'">
              {{ BIZ_LABEL[row.bizType] || row.bizType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operation" label="操作" width="110" />
        <el-table-column prop="targetDesc" label="对象" min-width="160" show-overflow-tooltip />
        <el-table-column prop="operator" label="操作人" width="120" />
        <template #empty>
          <el-empty description="暂无操作记录" :image-size="60" />
        </template>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import {
  Bell, CircleCheck, CircleClose, Document, Files, FolderOpened, Grid, TrendCharts
} from '@element-plus/icons-vue'
import { fetchLogs } from '../api/log'
import { fetchOverview } from '../api/stats'

// 首页统计（REQ-FED01 / design 3.6）：8 张指标卡 + 最近操作列表
const BIZ_LABEL = {
  CATEGORY: '分类', MODEL: '模型', DATA: '主数据', PUSH: '推送',
  COLLABORATION: '协同', QUALITY: '质量', IMPORT_EXPORT: '导入导出'
}
const BIZ_TAG = {
  CATEGORY: 'primary', MODEL: 'success', DATA: 'warning',
  PUSH: 'danger', COLLABORATION: 'info', QUALITY: 'primary', IMPORT_EXPORT: 'info'
}

const overview = ref({})
const logs = ref([])
const loading = ref(false)

const cards = computed(() => {
  const o = overview.value || {}
  return [
    { label: '分类总数', value: o.categoryCount ?? 0, sub: '主数据分类树', icon: FolderOpened, color: '#409eff' },
    { label: '模型总数', value: o.modelTotal ?? 0, sub: `已上线 ${o.modelOnline ?? 0}`, icon: Grid, color: '#67c23a' },
    { label: '主数据总数', value: o.dataTotal ?? 0, sub: `有效 ${o.dataValid ?? 0} / 禁用 ${o.dataDisabled ?? 0}`, icon: Files, color: '#e6a23c' },
    { label: '今日新增数据', value: o.todayNewData ?? 0, sub: '当日新增主数据', icon: TrendCharts, color: '#f56c6c' },
    { label: '待协同工单', value: o.pendingCollaboration ?? 0, sub: '待数据审核员处理', icon: Bell, color: '#e6a23c' },
    { label: '推送成功', value: o.pushSuccess ?? 0, sub: '累计成功次数', icon: CircleCheck, color: '#67c23a' },
    { label: '推送失败', value: o.pushFail ?? 0, sub: '累计失败次数', icon: CircleClose, color: '#f56c6c' },
    { label: '今日操作次数', value: o.todayOperations ?? 0, sub: '全平台操作留痕', icon: Document, color: '#409eff' }
  ]
})

async function load() {
  loading.value = true
  try {
    const [stat, logPage] = await Promise.all([
      fetchOverview(),
      fetchLogs({ page: 1, size: 10 })
    ])
    overview.value = stat || {}
    logs.value = logPage?.list || []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.card-col {
  margin-bottom: 12px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-icon {
  font-size: 40px;
  flex-shrink: 0;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #303133;
  margin-top: 2px;
}

.stat-sub {
  font-size: 12px;
}

.recent {
  margin-top: 4px;
}

.recent-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.recent-title {
  font-size: 15px;
  font-weight: 600;
}
</style>
