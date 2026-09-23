<template>
  <div class="page-container">
    <div class="page-card">
      <div class="filter-bar">
        <el-select v-model="query.bizType" placeholder="业务类型" clearable style="width: 150px">
          <el-option v-for="(label, value) in BIZ_TYPES" :key="value" :value="value" :label="label" />
        </el-select>
        <el-select v-model="query.operation" placeholder="操作" clearable style="width: 150px">
          <el-option v-for="op in OPERATIONS" :key="op" :value="op" :label="op" />
        </el-select>
        <el-input
          v-model="query.operator"
          placeholder="操作人"
          clearable
          style="width: 150px"
          @keyup.enter="search"
        />
        <el-date-picker
          v-model="timeRange"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          style="width: 380px"
        />
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="logs" border size="small">
        <el-table-column prop="operatedTime" label="时间" width="170" />
        <el-table-column label="业务类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="BIZ_TAG[row.bizType] || 'info'">
              {{ BIZ_TYPES[row.bizType] || row.bizType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operation" label="操作" width="110" />
        <el-table-column prop="targetId" label="对象ID" width="90" />
        <el-table-column prop="targetDesc" label="对象" min-width="170" show-overflow-tooltip />
        <el-table-column prop="operator" label="操作人" width="130" />
        <el-table-column label="详情" width="90" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              :disabled="!row.detail"
              @click="showDetail(row)"
            >
              查看
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无操作日志" :image-size="60" />
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="操作详情" width="640px" append-to-body>
      <pre class="detail-json">{{ prettyDetail }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { fetchLogs } from '../api/log'

// 操作日志（REQ-BKD10 / design 3.6）：时间范围 + 类型筛选 + 详情 JSON 弹窗
const BIZ_TYPES = {
  CATEGORY: '分类',
  MODEL: '模型',
  DATA: '主数据',
  PUSH: '推送',
  COLLABORATION: '协同',
  QUALITY: '质量',
  IMPORT_EXPORT: '导入导出'
}
const BIZ_TAG = {
  CATEGORY: 'primary',
  MODEL: 'success',
  DATA: 'warning',
  PUSH: 'danger',
  COLLABORATION: 'info',
  QUALITY: 'primary',
  IMPORT_EXPORT: 'info'
}
const OPERATIONS = [
  'CREATE', 'UPDATE', 'DELETE', 'ONLINE', 'OFFLINE', 'DISABLE', 'ENABLE',
  'ROLLBACK', 'PUSH', 'IMPORT', 'EXPORT', 'CONFIRM', 'REJECT', 'IGNORE'
]

const loading = ref(false)
const logs = ref([])
const total = ref(0)
const timeRange = ref([])
const query = reactive({
  bizType: '',
  operation: '',
  operator: '',
  page: 1,
  size: 20
})

const detailVisible = ref(false)
const detailText = ref('')
const prettyDetail = computed(() => {
  try {
    return JSON.stringify(JSON.parse(detailText.value), null, 2)
  } catch (e) {
    return detailText.value
  }
})

async function load() {
  loading.value = true
  try {
    const page = await fetchLogs({
      bizType: query.bizType || undefined,
      operation: query.operation || undefined,
      operator: query.operator || undefined,
      startTime: timeRange.value?.[0] || undefined,
      endTime: timeRange.value?.[1] || undefined,
      page: query.page,
      size: query.size
    })
    logs.value = page?.list || []
    total.value = page?.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function reset() {
  query.bizType = ''
  query.operation = ''
  query.operator = ''
  query.page = 1
  timeRange.value = []
  load()
}

function showDetail(row) {
  detailText.value = row.detail || ''
  detailVisible.value = true
}

onMounted(load)
</script>

<style scoped>
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.detail-json {
  margin: 0;
  padding: 12px;
  background: #f6f8fa;
  border-radius: 4px;
  max-height: 420px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
