<template>
  <el-drawer
    :model-value="visible"
    title="数据版本管理"
    size="720px"
    @update:model-value="(v) => $emit('update:visible', v)"
  >
    <div class="toolbar">
      <el-button size="small" type="primary" :disabled="selectedRows.length !== 2" @click="openDiff">
        对比选中两版本
      </el-button>
      <span class="text-muted">勾选任意两个版本进行逐字段差异对比</span>
    </div>

    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="versions"
      border
      size="small"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="42" />
      <el-table-column prop="versionNo" label="版本" width="70" />
      <el-table-column prop="operation" label="操作" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="operationType(row.operation)">{{ row.operation }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="operator" label="操作人" width="120" />
      <el-table-column prop="operatedTime" label="时间" min-width="160" />
      <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="rollback(row)">回滚</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无版本记录" :image-size="60" />
      </template>
    </el-table>

    <el-dialog
      v-model="showDiff"
      :title="`版本差异 v${diffFrom} → v${diffTo}`"
      width="760px"
      append-to-body
    >
      <DiffTable :items="diffItems" />
    </el-dialog>
  </el-drawer>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  diffDataVersions,
  fetchDataVersions,
  rollbackData
} from '../api/data'
import DiffTable from './DiffTable.vue'

// 数据版本抽屉（REQ-BKD06 / design 3.4）：列表 + 两版本 diff + 回滚
const props = defineProps({
  visible: { type: Boolean, default: false },
  modelId: { type: [Number, String], default: null },
  dataId: { type: [Number, String], default: null }
})

const emit = defineEmits(['update:visible', 'success'])

const loading = ref(false)
const versions = ref([])
const tableRef = ref(null)
const selectedRows = ref([])

const showDiff = ref(false)
const diffFrom = ref(null)
const diffTo = ref(null)
const diffItems = ref([])

watch(
  () => props.visible,
  async (visible) => {
    if (!visible) return
    selectedRows.value = []
    await load()
  }
)

async function load() {
  loading.value = true
  try {
    versions.value = (await fetchDataVersions(props.modelId, props.dataId)) || []
  } finally {
    loading.value = false
  }
}

function onSelectionChange(rows) {
  if (rows.length > 2) {
    tableRef.value.toggleRowSelection(rows[rows.length - 1], false)
    return
  }
  selectedRows.value = rows
}

async function openDiff() {
  const sorted = [...selectedRows.value].sort((a, b) => a.versionNo - b.versionNo)
  diffFrom.value = sorted[0].versionNo
  diffTo.value = sorted[1].versionNo
  const result = await diffDataVersions(
    props.modelId,
    props.dataId,
    diffFrom.value,
    diffTo.value
  )
  diffItems.value = (result?.changes || []).map((change) => ({
    field: change.field,
    label: change.label,
    oldValue: change.oldValue,
    newValue: change.newValue
  }))
  showDiff.value = true
}

async function rollback(row) {
  try {
    await ElMessageBox.confirm(
      `确认回滚到版本 v${row.versionNo}？回滚将生成新版本并通过质量校验。`,
      '回滚确认',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  await rollbackData(props.modelId, props.dataId, row.versionNo)
  ElMessage.success(`已回滚到版本 v${row.versionNo}`)
  emit('success')
  await load()
}

function operationType(operation) {
  const map = {
    CREATE: 'success',
    UPDATE: 'primary',
    ROLLBACK: 'warning',
    DISABLE: 'info',
    ENABLE: 'success',
    DELETE: 'danger'
  }
  return map[operation] || 'info'
}
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
</style>
