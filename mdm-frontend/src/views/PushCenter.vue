<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- ==================== 推送执行 ==================== -->
      <el-tab-pane label="推送执行" name="push">
        <div class="page-card">
          <div class="filter-bar">
            <el-select
              v-model="pushModelId"
              placeholder="选择模型（仅已上线）"
              clearable
              filterable
              style="width: 260px"
              @change="onModelChange"
            >
              <el-option
                v-for="m in onlineModels"
                :key="m.id"
                :value="m.id"
                :label="`${m.name}（${m.code}）`"
              />
            </el-select>
            <el-input
              v-model="dataKeyword"
              placeholder="数据编码/名称"
              clearable
              style="width: 200px"
              @keyup.enter="searchData"
            />
            <el-button type="primary" @click="searchData">查询</el-button>
            <el-button @click="resetPrecheck">清除预检</el-button>
          </div>

          <el-table
            ref="dataTableRef"
            v-loading="dataLoading"
            :data="dataList"
            border
            size="small"
            max-height="300"
            @selection-change="onDataSelectionChange"
          >
            <el-table-column type="selection" width="42" />
            <el-table-column prop="code" label="编码" width="150" />
            <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'VALID' ? 'success' : 'info'">
                  {{ row.status === 'VALID' ? '有效' : '已禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="updatedTime" label="更新时间" width="160" />
            <template #empty>
              <el-empty :description="pushModelId ? '暂无数据' : '请先选择模型'" :image-size="60" />
            </template>
          </el-table>
          <div class="pager">
            <el-pagination
              v-model:current-page="dataPage"
              v-model:page-size="dataSize"
              :total="dataTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @size-change="loadData"
              @current-change="loadData"
            />
          </div>

          <el-divider />

          <div class="system-row">
            <span class="label">目标系统：</span>
            <el-checkbox-group v-model="systemIds">
              <el-checkbox v-for="sys in systems" :key="sys.id" :value="sys.id">
                {{ sys.name }}（{{ sys.code }}）
              </el-checkbox>
            </el-checkbox-group>
          </div>

          <div class="exec-row">
            <el-button
              type="primary"
              plain
              :disabled="!selectedRows.length || !systemIds.length"
              @click="onPrecheck"
            >
              预检（{{ selectedRows.length }} 条数据 × {{ systemIds.length }} 个系统）
            </el-button>
            <el-button
              type="success"
              :disabled="!selectedRows.length || !systemIds.length"
              :loading="pushing"
              @click="onExecute"
            >
              执行推送
            </el-button>
            <el-checkbox v-if="hasPrecheckFail" v-model="force" class="force-check">
              强制推送（存在预检 FAIL）
            </el-checkbox>
          </div>

          <el-table
            v-if="precheckResults.length"
            :data="precheckResults"
            border
            size="small"
            class="precheck-table"
          >
            <el-table-column prop="dataCode" label="数据编码" width="150" />
            <el-table-column prop="systemCode" label="系统" width="120" />
            <el-table-column prop="systemName" label="系统名称" min-width="140" />
            <el-table-column label="预检结果" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="precheckTag(row.result)">
                  {{ precheckLabel(row.result) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="说明" min-width="180">
              <template #default="{ row }">{{ row.detail || '-' }}</template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- ==================== 协同工单 ==================== -->
      <el-tab-pane label="协同工单" name="collab">
        <div class="page-card">
          <div class="filter-bar">
            <el-select v-model="collabStatus" placeholder="状态" clearable style="width: 150px" @change="loadCollabs">
              <el-option value="PENDING" label="待处理" />
              <el-option value="CONFIRMED" label="已确认" />
              <el-option value="REJECTED" label="已驳回" />
            </el-select>
            <el-button @click="loadCollabs">刷新</el-button>
            <span class="text-muted">敏感字段变更触发协同，待数据审核员确认后生效</span>
          </div>

          <el-table v-loading="collabLoading" :data="collabs" border size="small">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="dataId" label="数据ID" width="80" />
            <el-table-column prop="modelId" label="模型ID" width="80" />
            <el-table-column prop="reason" label="变更原因" min-width="170" show-overflow-tooltip />
            <el-table-column prop="applicant" label="申请人" width="110" />
            <el-table-column prop="applyTime" label="申请时间" width="160" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="collabTag(row.status)">
                  {{ collabLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="confirmComment" label="处理意见" min-width="140" show-overflow-tooltip />
            <el-table-column prop="confirmTime" label="处理时间" width="160" />
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === 'PENDING'">
                  <el-button
                    link
                    type="primary"
                    size="small"
                    :disabled="!canAudit"
                    @click="handleCollab(row, true)"
                  >
                    确认
                  </el-button>
                  <el-button
                    link
                    type="danger"
                    size="small"
                    :disabled="!canAudit"
                    @click="handleCollab(row, false)"
                  >
                    驳回
                  </el-button>
                </template>
                <span v-else class="text-muted">已处理</span>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无协同工单" :image-size="60" />
            </template>
          </el-table>
          <div class="pager">
            <el-pagination
              v-model:current-page="collabPage"
              v-model:page-size="collabSize"
              :total="collabTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @size-change="loadCollabs"
              @current-change="loadCollabs"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- ==================== 推送日志 ==================== -->
      <el-tab-pane label="推送日志" name="logs">
        <div class="page-card">
          <div class="filter-bar">
            <el-input
              v-model="logDataId"
              placeholder="数据ID"
              clearable
              style="width: 150px"
              @keyup.enter="searchLogs"
            />
            <el-button type="primary" @click="searchLogs">查询</el-button>
            <el-button @click="loadLogs">刷新</el-button>
          </div>

          <el-table v-loading="logLoading" :data="pushLogs" border size="small">
            <el-table-column prop="pushedTime" label="推送时间" width="170" />
            <el-table-column prop="dataCode" label="数据编码" width="150" />
            <el-table-column prop="modelId" label="模型ID" width="80" />
            <el-table-column prop="systemCode" label="系统" width="120" />
            <el-table-column label="结果" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.result === 'SUCCESS' ? 'success' : 'danger'">
                  {{ row.result === 'SUCCESS' ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="detail" label="详情" min-width="180" show-overflow-tooltip />
            <el-table-column prop="operator" label="操作人" width="130" />
            <template #empty>
              <el-empty description="暂无推送日志" :image-size="60" />
            </template>
          </el-table>
          <div class="pager">
            <el-pagination
              v-model:current-page="logPage"
              v-model:page-size="logSize"
              :total="logTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @size-change="loadLogs"
              @current-change="loadLogs"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 推送结果 -->
    <el-dialog v-model="resultVisible" title="推送结果" width="680px" append-to-body>
      <el-descriptions :column="2" border size="small" class="result-summary">
        <el-descriptions-item label="成功">{{ result?.successCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="失败">
          <span :style="{ color: result?.failCount ? '#f56c6c' : '#67c23a' }">
            {{ result?.failCount ?? 0 }}
          </span>
        </el-descriptions-item>
      </el-descriptions>
      <el-table :data="result?.items || []" border size="small" max-height="320">
        <el-table-column prop="dataCode" label="数据编码" width="140" />
        <el-table-column prop="systemCode" label="系统" width="110" />
        <el-table-column prop="systemName" label="系统名称" min-width="120" />
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.result === 'SUCCESS' ? 'success' : 'danger'">
              {{ row.result === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="detail" label="详情" min-width="150" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchDataPage } from '../api/data'
import { fetchModels } from '../api/model'
import {
  confirmCollaboration,
  executePush,
  fetchCollaborations,
  fetchPushLogs,
  fetchSystems,
  precheckPush,
  rejectCollaboration
} from '../api/push'
import { useUserStore } from '../stores/user'

// 推送中心（REQ-FED07 / design 3.5）：数据×系统预检 → 执行推送；协同工单；推送日志
const route = useRoute()
const userStore = useUserStore()

const canAudit = computed(() => userStore.can('DATA_AUDITOR', 'SYS_ADMIN'))

const activeTab = ref('push')

// ---------- 推送执行 ----------
const onlineModels = ref([])
const systems = ref([])
const pushModelId = ref(null)
const dataKeyword = ref('')
const dataList = ref([])
const dataTotal = ref(0)
const dataPage = ref(1)
const dataSize = ref(10)
const dataLoading = ref(false)
const dataTableRef = ref(null)
const selectedRows = ref([])
const systemIds = ref([])
const precheckResults = ref([])
const force = ref(false)
const pushing = ref(false)
const resultVisible = ref(false)
const result = ref(null)
let pendingSelectDataId = null

const hasPrecheckFail = computed(() =>
  precheckResults.value.some((r) => r.result === 'FAIL')
)

async function loadModels() {
  onlineModels.value = (await fetchModels({ status: 'ONLINE', page: 1, size: 200 }))?.list || []
}

async function loadSystems() {
  const all = (await fetchSystems()) || []
  systems.value = all.filter((s) => s.enabled === 1 || s.enabled === true)
}

async function onModelChange() {
  dataPage.value = 1
  resetPrecheck()
  await loadData()
}

async function loadData() {
  if (!pushModelId.value) {
    dataList.value = []
    dataTotal.value = 0
    return
  }
  dataLoading.value = true
  try {
    const page = await fetchDataPage(pushModelId.value, {
      keyword: dataKeyword.value || undefined,
      status: 'VALID',
      page: dataPage.value,
      size: dataSize.value
    })
    dataList.value = page?.list || []
    dataTotal.value = page?.total || 0
    if (pendingSelectDataId) {
      const target = dataList.value.find((row) => row.id === pendingSelectDataId)
      if (target) {
        dataTableRef.value?.toggleRowSelection(target, true)
      }
      pendingSelectDataId = null
    }
  } finally {
    dataLoading.value = false
  }
}

function searchData() {
  dataPage.value = 1
  loadData()
}

function onDataSelectionChange(rows) {
  selectedRows.value = rows
  resetPrecheck()
}

function resetPrecheck() {
  precheckResults.value = []
  force.value = false
}

async function onPrecheck() {
  precheckResults.value =
    (await precheckPush(
      selectedRows.value.map((r) => r.id),
      systemIds.value
    )) || []
}

async function onExecute() {
  try {
    await ElMessageBox.confirm(
      `将向 ${systemIds.value.length} 个下游系统推送 ${selectedRows.value.length} 条数据，确认执行？`,
      '推送确认',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  pushing.value = true
  try {
    result.value = await executePush({
      dataIds: selectedRows.value.map((r) => r.id),
      systemIds: systemIds.value,
      force: force.value
    })
    resultVisible.value = true
    ElMessage.success(`推送完成：成功 ${result.value?.successCount ?? 0}，失败 ${result.value?.failCount ?? 0}`)
    await onPrecheck()
    loadLogs()
  } finally {
    pushing.value = false
  }
}

function precheckTag(result_) {
  return { PASS: 'success', FAIL: 'danger', CHECKING: 'warning' }[result_] || 'info'
}

function precheckLabel(result_) {
  return { PASS: '通过', FAIL: '未通过', CHECKING: '确认中' }[result_] || result_
}

// ---------- 协同工单 ----------
const collabLoading = ref(false)
const collabs = ref([])
const collabTotal = ref(0)
const collabPage = ref(1)
const collabSize = ref(10)
const collabStatus = ref('')

async function loadCollabs() {
  collabLoading.value = true
  try {
    const page = await fetchCollaborations({
      status: collabStatus.value || undefined,
      page: collabPage.value,
      size: collabSize.value
    })
    collabs.value = page?.list || []
    collabTotal.value = page?.total || 0
  } finally {
    collabLoading.value = false
  }
}

function collabLabel(status) {
  return { PENDING: '待处理', CONFIRMED: '已确认', REJECTED: '已驳回' }[status] || status
}

function collabTag(status) {
  return { PENDING: 'warning', CONFIRMED: 'success', REJECTED: 'danger' }[status] || 'info'
}

async function handleCollab(row, isConfirm) {
  const action = isConfirm ? '确认' : '驳回'
  let comment = ''
  try {
    const { value } = await ElMessageBox.prompt(
      `确认${action}协同工单 #${row.id} 的敏感字段变更？`,
      `${action}协同`,
      {
        inputPlaceholder: '处理意见（可选）',
        inputValue: '',
        confirmButtonText: action
      }
    )
    comment = value || ''
  } catch (e) {
    return
  }
  if (isConfirm) {
    await confirmCollaboration(row.id, comment)
  } else {
    await rejectCollaboration(row.id, comment)
  }
  ElMessage.success(`已${action}工单 #${row.id}`)
  await loadCollabs()
}

// ---------- 推送日志 ----------
const logLoading = ref(false)
const pushLogs = ref([])
const logTotal = ref(0)
const logPage = ref(1)
const logSize = ref(10)
const logDataId = ref('')

async function loadLogs() {
  logLoading.value = true
  try {
    const page = await fetchPushLogs({
      dataId: logDataId.value || undefined,
      page: logPage.value,
      size: logSize.value
    })
    pushLogs.value = page?.list || []
    logTotal.value = page?.total || 0
  } finally {
    logLoading.value = false
  }
}

function searchLogs() {
  logPage.value = 1
  loadLogs()
}

function onTabChange(name) {
  if (name === 'collab' && !collabs.value.length) loadCollabs()
  if (name === 'logs' && !pushLogs.value.length) loadLogs()
}

onMounted(async () => {
  await Promise.all([loadModels(), loadSystems()])
  const queryModelId = route.query.modelId ? Number(route.query.modelId) : null
  const queryDataId = route.query.dataId ? Number(route.query.dataId) : null
  if (queryModelId) {
    pushModelId.value = queryModelId
    pendingSelectDataId = queryDataId
    if (activeTab.value === 'push') await loadData()
  }
})
</script>

<style scoped>
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.system-row {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-bottom: 12px;
}

.system-row .label {
  line-height: 32px;
  color: #606266;
  flex-shrink: 0;
}

.exec-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.force-check {
  margin-left: 4px;
}

.precheck-table {
  margin-top: 4px;
}

.result-summary {
  margin-bottom: 12px;
}
</style>
