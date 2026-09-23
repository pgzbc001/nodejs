<template>
  <div class="page-container dm-page">
    <!-- 左侧：分类 → 在线模型树 -->
    <div class="side page-card">
      <div class="tree-header">
        <span class="tree-title">在线模型</span>
        <el-button link size="small" type="primary" @click="loadTree">刷新</el-button>
      </div>
      <el-input
        v-model="treeKeyword"
        placeholder="搜索分类/模型"
        clearable
        size="small"
        class="tree-filter"
      />
      <el-tree
        ref="treeRef"
        :data="treeData"
        node-key="id"
        :props="{ label: 'label', children: 'children' }"
        highlight-current
        default-expand-all
        :expand-on-click-node="false"
        :filter-node-method="filterTreeNode"
        @node-click="onNodeClick"
      >
        <template #default="{ data }">
          <span class="tree-node">
            <el-icon v-if="data.isModel" class="model-icon"><Tickets /></el-icon>
            <el-icon v-else class="cat-icon"><FolderOpened /></el-icon>
            <span>{{ data.label }}</span>
          </span>
        </template>
      </el-tree>
      <el-empty v-if="!treeData.length" description="暂无在线模型" :image-size="60" />
    </div>

    <!-- 右侧：数据维护 -->
    <div class="content page-card">
      <template v-if="currentModel">
        <div class="model-bar">
          <span class="model-title">{{ currentModel.label }}</span>
          <el-tag size="small" type="info">{{ currentModel.modelCode }}</el-tag>
        </div>

        <div class="toolbar">
          <el-button type="primary" size="small" :disabled="!canWrite" @click="openForm(null)">新增</el-button>
          <el-button size="small" :disabled="!canWrite || !currentRow" @click="openForm(currentRow.id)">修改</el-button>
          <el-button
            size="small"
            :disabled="!canWrite || !currentRow || currentRow.status !== 'VALID'"
            @click="handleDisable"
          >
            禁用
          </el-button>
          <el-button
            size="small"
            :disabled="!canWrite || !currentRow || currentRow.status !== 'DISABLED'"
            @click="handleEnable"
          >
            启用
          </el-button>
          <el-button size="small" type="danger" plain :disabled="!canWrite || !currentRow" @click="handleDelete">
            删除
          </el-button>
          <span class="divider" />
          <el-button size="small" :disabled="!currentRow" @click="goPush">推送</el-button>
          <el-button size="small" :disabled="!currentRow" @click="openVersions">版本</el-button>
          <el-button size="small" @click="ioVisible = true">导入 / 导出</el-button>
          <el-button size="small" @click="loadData">刷新</el-button>
          <span v-if="currentRow" class="text-muted selected-tip">
            已选：{{ currentRow.code }}
          </span>
        </div>

        <div class="filter-bar">
          <div class="filter-item">
            <span class="filter-label">关键字</span>
            <el-input
              v-model="keyword"
              placeholder="编码 / 名称"
              clearable
              size="small"
              style="width: 180px"
              @keyup.enter="search"
            />
          </div>
          <div class="filter-item">
            <span class="filter-label">状态</span>
            <el-select v-model="status" clearable size="small" style="width: 130px">
              <el-option value="VALID" label="有效" />
              <el-option value="DISABLED" label="已禁用" />
            </el-select>
          </div>
          <div
            v-for="f in viewSchema?.searchFields || []"
            :key="f.name"
            class="filter-item"
          >
            <span class="filter-label">{{ f.label }}</span>
            <el-select
              v-if="f.selectable && f.domainValues?.length"
              v-model="filters[f.name]"
              clearable
              size="small"
              style="width: 160px"
            >
              <el-option v-for="v in f.domainValues" :key="v" :value="v" :label="v" />
            </el-select>
            <el-input
              v-else
              v-model="filters[f.name]"
              clearable
              size="small"
              style="width: 160px"
              @keyup.enter="search"
            />
          </div>
          <el-button type="primary" size="small" @click="search">查询</el-button>
          <el-button size="small" @click="resetSearch">重置</el-button>
        </div>

        <el-table
          v-loading="loading"
          :data="rows"
          border
          size="small"
          highlight-current-row
          max-height="520"
          @current-change="onCurrentChange"
        >
          <el-table-column prop="code" label="编码" width="160" fixed="left" />
          <el-table-column prop="name" label="名称" min-width="150" show-overflow-tooltip />
          <el-table-column
            v-for="field in listFields"
            :key="field.name"
            :label="field.label"
            :min-width="field.type === 'LONG_TEXT' ? 180 : 130"
            show-overflow-tooltip
          >
            <template #default="{ row }">{{ attrText(row, field) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'VALID' ? 'success' : 'info'">
                {{ row.status === 'VALID' ? '有效' : '已禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="协同" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.collabStatus" size="small" :type="collabTag(row.collabStatus)">
                {{ collabLabel(row.collabStatus) }}
              </el-tag>
              <span v-else class="text-muted">—</span>
            </template>
          </el-table-column>
          <el-table-column prop="versionNo" label="版本" width="70" />
          <el-table-column prop="updatedBy" label="更新人" width="120" />
          <el-table-column prop="updatedTime" label="更新时间" width="160" />
          <template #empty>
            <el-empty description="暂无数据，可新增或导入" :image-size="60" />
          </template>
        </el-table>

        <div class="pager">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="size"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            @size-change="loadData"
            @current-change="loadData"
          />
        </div>
      </template>
      <el-empty v-else description="请在左侧选择模型开始维护数据" :image-size="80" />
    </div>

    <!-- 动态表单抽屉 -->
    <DynamicFormDrawer
      v-model:visible="formVisible"
      :model-id="currentModel?.modelId"
      :data-id="formDataId"
      :view-schema="viewSchema"
      @success="onFormSuccess"
      @use-existing="onUseExisting"
    />

    <!-- 版本抽屉 -->
    <VersionDrawer
      v-model:visible="versionVisible"
      :model-id="currentModel?.modelId"
      :data-id="versionDataId"
      @success="loadData"
    />

    <!-- 导入导出 -->
    <ImportExportDialog
      v-model:visible="ioVisible"
      :model-id="currentModel?.modelId"
      @success="loadData"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FolderOpened, Tickets } from '@element-plus/icons-vue'
import { fetchCategoryTree } from '../api/category'
import {
  deleteData as apiDeleteData,
  disableData as apiDisableData,
  enableData as apiEnableData,
  fetchDataPage,
  fetchDataView
} from '../api/data'
import { fetchAllModels } from '../api/model'
import { useUserStore } from '../stores/user'
import DynamicFormDrawer from '../components/DynamicFormDrawer.vue'
import ImportExportDialog from '../components/ImportExportDialog.vue'
import VersionDrawer from '../components/VersionDrawer.vue'

// 数据维护（REQ-FED04/05/06/07 / design 3.4）：分类-模型树 + view 元数据驱动表格
const router = useRouter()
const userStore = useUserStore()

const canWrite = computed(() => userStore.can('DATA_STAFF', 'MODEL_ADMIN', 'SYS_ADMIN'))

// ---------- 左侧树 ----------
const treeRef = ref(null)
const treeData = ref([])
const treeKeyword = ref('')

watch(treeKeyword, (value) => {
  treeRef.value?.filter(value)
})

function filterTreeNode(value, data) {
  if (!value) return true
  return data.label.includes(value.trim())
}

async function loadTree() {
  const [cats, models] = await Promise.all([
    fetchCategoryTree(),
    fetchAllModels({ status: 'ONLINE' })
  ])
  const modelList = models?.list || []
  const used = new Set()
  const build = (nodes) =>
    (nodes || []).map((cat) => {
      const children = build(cat.children)
      for (const m of modelList) {
        if (m.categoryId === cat.id) {
          used.add(m.id)
          children.push(toModelNode(m))
        }
      }
      return { id: `cat-${cat.id}`, label: cat.name, isModel: false, children }
    })
  const tree = build(cats)
  const orphans = modelList.filter((m) => !used.has(m.id))
  if (orphans.length) {
    tree.push({
      id: 'cat-unmatched',
      label: '未分类模型',
      isModel: false,
      children: orphans.map(toModelNode)
    })
  }
  treeData.value = tree
}

function toModelNode(m) {
  return { id: `model-${m.id}`, label: m.name, isModel: true, modelId: m.id, modelCode: m.code }
}

// ---------- 右侧数据 ----------
const currentModel = ref(null)
const viewSchema = ref(null)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const status = ref('')
const filters = reactive({})
const loading = ref(false)
const currentRow = ref(null)

const listFields = computed(() =>
  (viewSchema.value?.listFields || []).filter((f) => f.name !== 'code' && f.name !== 'name')
)

function onNodeClick(data) {
  if (data.isModel) selectModel(data)
}

async function selectModel(node) {
  currentModel.value = node
  currentRow.value = null
  keyword.value = ''
  status.value = ''
  resetFilters()
  page.value = 1
  viewSchema.value = await fetchDataView(node.modelId)
  await loadData()
}

function resetFilters() {
  Object.keys(filters).forEach((key) => delete filters[key])
}

function buildFilters() {
  const active = {}
  for (const f of viewSchema.value?.searchFields || []) {
    const value = filters[f.name]
    if (value !== '' && value !== null && value !== undefined) {
      active[f.name] = String(value)
    }
  }
  return Object.keys(active).length ? JSON.stringify(active) : undefined
}

async function loadData() {
  if (!currentModel.value) return
  loading.value = true
  try {
    const result = await fetchDataPage(currentModel.value.modelId, {
      keyword: keyword.value || undefined,
      status: status.value || undefined,
      filters: buildFilters(),
      page: page.value,
      size: size.value
    })
    rows.value = result?.list || []
    total.value = result?.total || 0
    currentRow.value = null
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  loadData()
}

function resetSearch() {
  keyword.value = ''
  status.value = ''
  resetFilters()
  page.value = 1
  loadData()
}

function onCurrentChange(row) {
  currentRow.value = row
}

function attrText(row, field) {
  const value = row.attributes?.[field.name]
  if (value === null || value === undefined || value === '') return '—'
  return Array.isArray(value) ? value.join(', ') : String(value)
}

function collabLabel(status_) {
  return { PENDING: '待协同', CONFIRMED: '已协同', REJECTED: '已驳回' }[status_] || status_
}

function collabTag(status_) {
  return { PENDING: 'warning', CONFIRMED: 'success', REJECTED: 'danger' }[status_] || 'info'
}

// ---------- 表单 / 版本 / 导入导出 ----------
const formVisible = ref(false)
const formDataId = ref(null)
const versionVisible = ref(false)
const versionDataId = ref(null)
const ioVisible = ref(false)

function openForm(dataId) {
  if (dataId && !currentRow.value) {
    ElMessage.warning('请先在表格中选择一行数据')
    return
  }
  formDataId.value = dataId
  formVisible.value = true
}

function openVersions() {
  if (!currentRow.value) return
  versionDataId.value = currentRow.value.id
  versionVisible.value = true
}

function goPush() {
  if (!currentRow.value) return
  router.push({
    path: '/push',
    query: { modelId: currentModel.value.modelId, dataId: currentRow.value.id }
  })
}

function onFormSuccess() {
  loadData()
}

function onUseExisting(item) {
  keyword.value = item.code || ''
  page.value = 1
  loadData()
}

// ---------- 禁用 / 启用 / 删除（40909 下游预检失败需强制） ----------
async function handleDisable() {
  const row = currentRow.value
  try {
    await ElMessageBox.confirm(
      `确认禁用数据「${row.code}」？禁用后下游将不再接收该数据。`,
      '禁用确认',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    await apiDisableData(currentModel.value.modelId, row.id, false)
    ElMessage.success('已禁用')
  } catch (e) {
    if (e?.code === 40909) {
      const confirmed = await confirmForce('下游预检未通过（存在 FAIL 状态）')
      if (!confirmed) return
      await apiDisableData(currentModel.value.modelId, row.id, true)
      ElMessage.success('已强制禁用')
    }
  }
  await loadData()
}

async function handleEnable() {
  const row = currentRow.value
  try {
    await ElMessageBox.confirm(`确认启用数据「${row.code}」？`, '启用确认', { type: 'warning' })
  } catch (e) {
    return
  }
  await apiEnableData(currentModel.value.modelId, row.id)
  ElMessage.success('已启用')
  await loadData()
}

async function handleDelete() {
  const row = currentRow.value
  try {
    await ElMessageBox.confirm(`确认删除数据「${row.code}」？删除为逻辑删除。`, '删除确认', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await apiDeleteData(currentModel.value.modelId, row.id, false)
    ElMessage.success('已删除')
  } catch (e) {
    if (e?.code === 40909) {
      const confirmed = await confirmForce('下游预检未通过（存在 FAIL 状态）')
      if (!confirmed) return
      await apiDeleteData(currentModel.value.modelId, row.id, true)
      ElMessage.success('已强制删除')
    }
  }
  await loadData()
}

async function confirmForce(reason) {
  try {
    await ElMessageBox.confirm(`${reason}。是否强制操作？`, '预检未通过', { type: 'warning' })
    return true
  } catch (e) {
    return false
  }
}

onMounted(loadTree)
</script>

<style scoped>
.dm-page {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.side {
  width: 260px;
  flex-shrink: 0;
}

.content {
  flex: 1;
  min-width: 0;
}

.tree-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.tree-title {
  font-weight: 600;
}

.tree-filter {
  margin-bottom: 8px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 4px;
}

.model-icon {
  color: #409eff;
}

.cat-icon {
  color: #e6a23c;
}

.model-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.model-title {
  font-size: 15px;
  font-weight: 600;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.divider {
  width: 1px;
  height: 20px;
  background: #ebeef5;
  margin: 0 2px;
}

.selected-tip {
  font-size: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
