<template>
  <el-dialog
    :model-value="visible"
    :title="`选择引用数据（${refModelCode || ''}）`"
    width="780px"
    append-to-body
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
  >
    <div class="dialog-toolbar">
      <el-input
        v-model="keyword"
        placeholder="按编码/名称搜索"
        clearable
        size="small"
        style="width: 260px"
        @keyup.enter="loadPage(1)"
        @clear="loadPage(1)"
      />
      <el-button size="small" @click="loadPage(1)">搜索</el-button>
    </div>

    <el-table
      :data="rows"
      v-loading="loading"
      border
      size="small"
      height="340"
      highlight-current-row
      @current-change="onSelect"
    >
      <el-table-column prop="code" label="编码" width="190" />
      <el-table-column prop="name" label="名称" min-width="140" show-overflow-tooltip />
      <el-table-column
        v-for="col in extraColumns"
        :key="col.name"
        :label="col.label"
        min-width="120"
        show-overflow-tooltip
      >
        <template #default="{ row }">{{ display(row.attributes?.[col.name]) }}</template>
      </el-table-column>
      <template #empty>
        <el-empty description="无引用数据" :image-size="60" />
      </template>
    </el-table>

    <div class="dialog-pager">
      <el-pagination
        layout="total, prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page="page"
        @current-change="loadPage"
      />
    </div>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :disabled="!selected" @click="confirm">
        确定{{ selected ? `：${selected.code}` : '' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchDataPage, fetchDataView } from '../api/data'
import { fetchAllModels } from '../api/model'

// 引用数据选择弹窗（REQ-FED05）：按 refModelCode 解析模型 → 分页浏览 → 单选回填
const props = defineProps({
  visible: { type: Boolean, default: false },
  refModelCode: { type: String, default: '' }
})

const emit = defineEmits(['update:visible', 'picked'])

const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const rows = ref([])
const selected = ref(null)
const modelId = ref(null)
const extraColumns = ref([])

watch(
  () => props.visible,
  async (visible) => {
    if (!visible) return
    reset()
    await resolveModel()
  }
)

function reset() {
  keyword.value = ''
  page.value = 1
  total.value = 0
  rows.value = []
  selected.value = null
  modelId.value = null
  extraColumns.value = []
}

async function resolveModel() {
  if (!props.refModelCode) {
    ElMessage.warning('该字段未配置引用模型编码')
    return
  }
  loading.value = true
  try {
    const result = await fetchAllModels()
    const found = (result.list || []).find((m) => m.code === props.refModelCode)
    if (!found) {
      ElMessage.warning(`未找到引用模型：${props.refModelCode}`)
      return
    }
    modelId.value = found.id
    const schema = await fetchDataView(found.id)
    extraColumns.value = (schema.listFields || [])
      .filter((f) => f.name !== 'code' && f.name !== 'name')
      .slice(0, 4)
    await loadPage(1)
  } finally {
    loading.value = false
  }
}

async function loadPage(target) {
  if (!modelId.value) return
  loading.value = true
  try {
    page.value = target
    const result = await fetchDataPage(modelId.value, {
      keyword: keyword.value,
      page: target,
      size: size.value,
      status: 'VALID'
    })
    rows.value = result.list || []
    total.value = result.total || 0
    selected.value = null
  } finally {
    loading.value = false
  }
}

function onSelect(row) {
  selected.value = row
}

function confirm() {
  if (!selected.value) return
  emit('picked', {
    code: selected.value.code,
    name: selected.value.name,
    attributes: selected.value.attributes || {}
  })
  emit('update:visible', false)
}

function display(value) {
  if (value === null || value === undefined || value === '') return '—'
  return Array.isArray(value) ? value.join('、') : String(value)
}
</script>

<style scoped>
.dialog-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.dialog-pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}
</style>
