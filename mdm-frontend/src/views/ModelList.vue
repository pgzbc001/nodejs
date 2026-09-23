<template>
  <div class="page-container model-page">
    <!-- 左侧分类树 -->
    <div class="side page-card">
      <CategoryTreePanel ref="treePanelRef" @select="onCategorySelect" @refresh="onCategoryRefresh" />
    </div>

    <!-- 右侧模型列表 -->
    <div class="content page-card">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索模型编码/名称"
          clearable
          style="width: 220px"
          @keyup.enter="search"
        />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
          <el-option value="ONLINE" label="已上线" />
          <el-option value="OFFLINE" label="未上线" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
        <span class="spacer" />
        <el-button type="primary" @click="openCreate('BLANK')">空白创建</el-button>
        <el-button @click="openCreate('INHERIT')">继承创建</el-button>
      </div>

      <div v-if="currentCategory" class="category-tip">
        <span>当前分类：</span>
        <el-tag size="small">{{ currentCategory.name }}</el-tag>
        <el-button link size="small" type="primary" @click="clearCategory">清除</el-button>
      </div>

      <el-table v-loading="loading" :data="models" border size="small">
        <el-table-column prop="code" label="编码" width="140" />
        <el-table-column prop="name" label="名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'ONLINE' ? 'success' : 'info'">
              {{ row.status === 'ONLINE' ? '已上线' : '未上线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dept" label="归口部门" width="120" />
        <el-table-column prop="versionNo" label="版本" width="70" />
        <el-table-column prop="updatedTime" label="更新时间" width="160" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="design(row)">设计</el-button>
            <el-button link type="primary" size="small" @click="toggleOnline(row)">
              {{ row.status === 'ONLINE' ? '下线' : '上线' }}
            </el-button>
            <el-button link size="small" @click="openInherit(row)">继承</el-button>
            <el-button link type="primary" size="small" @click="openVersions(row)">版本</el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无模型，请创建" :image-size="60" />
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>

    <!-- 创建模型（空白 / 继承） -->
    <el-dialog
      v-model="createVisible"
      :title="createMode === 'INHERIT' ? '继承创建模型' : '空白创建模型'"
      width="560px"
      append-to-body
    >
      <el-form :model="createForm" label-width="100px">
        <el-form-item v-if="createMode === 'INHERIT'" label="来源模型" required>
          <el-select v-model="createForm.inheritFromId" filterable style="width: 100%">
            <el-option
              v-for="m in allModels"
              :key="m.id"
              :value="m.id"
              :label="`${m.name}（${m.code}）`"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="编码" required>
          <el-input v-model="createForm.code" placeholder="如 MDM_MATERIAL" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="createForm.name" placeholder="如 物料主数据" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-tree-select
            v-model="createForm.categoryId"
            :data="categoryTree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            default-expand-all
            :render-after-expand="false"
            placeholder="选择所属分类"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="归口部门">
          <el-input v-model="createForm.dept" placeholder="如 生产部" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-alert
          v-if="createMode === 'INHERIT'"
          type="info"
          :closable="false"
          title="继承创建将复制来源模型的字段定义、编码规则与扩展配置"
        />
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="saveCreate">创建</el-button>
      </template>
    </el-dialog>

    <ModelVersionDrawer
      v-model:visible="versionsVisible"
      :model-id="versionsModelId"
      @success="load"
    />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchCategoryTree } from '../api/category'
import {
  createModel,
  deleteModel,
  fetchAllModels,
  fetchModels,
  offlineModel,
  onlineModel
} from '../api/model'
import CategoryTreePanel from '../components/CategoryTreePanel.vue'
import ModelVersionDrawer from '../components/ModelVersionDrawer.vue'

// 模型管理（REQ-FED02 / design 3.2）：分类树 + 模型表格 + 创建/上线/版本
const router = useRouter()

const treePanelRef = ref(null)
const loading = ref(false)
const models = ref([])
const total = ref(0)
const currentCategory = ref(null)
const allModels = ref([])
const categoryTree = ref([])

const query = reactive({
  keyword: '',
  status: '',
  categoryId: null,
  page: 1,
  size: 10
})

const createVisible = ref(false)
const creating = ref(false)
const createMode = ref('BLANK')
const createForm = reactive({
  inheritFromId: null,
  code: '',
  name: '',
  categoryId: null,
  dept: '',
  description: ''
})

const versionsVisible = ref(false)
const versionsModelId = ref(null)

async function load() {
  loading.value = true
  try {
    const page = await fetchModels({
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      categoryId: query.categoryId || undefined,
      page: query.page,
      size: query.size
    })
    models.value = page?.list || []
    total.value = page?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadAllModels() {
  allModels.value = (await fetchAllModels())?.list || []
}

async function loadCategoryTree() {
  categoryTree.value = (await fetchCategoryTree()) || []
}

function search() {
  query.page = 1
  load()
}

function resetQuery() {
  query.keyword = ''
  query.status = ''
  query.categoryId = null
  currentCategory.value = null
  query.page = 1
  treePanelRef.value?.refresh()
  load()
}

function onCategorySelect(data) {
  currentCategory.value = data
  query.categoryId = data.id
  query.page = 1
  load()
}

function clearCategory() {
  currentCategory.value = null
  query.categoryId = null
  query.page = 1
  load()
}

async function onCategoryRefresh() {
  await loadCategoryTree()
}

function design(row) {
  router.push(`/models/${row.id}/design`)
}

async function toggleOnline(row) {
  const isOnline = row.status === 'ONLINE'
  try {
    await ElMessageBox.confirm(
      isOnline
        ? `确认下线模型「${row.name}」？下线后数据维护页将不再可见。`
        : `确认上线模型「${row.name}」？上线后字段结构将被锁定（仅可新增字段与调整展示属性）。`,
      isOnline ? '下线确认' : '上线确认',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  if (isOnline) {
    await offlineModel(row.id)
    ElMessage.success('模型已下线')
  } else {
    await onlineModel(row.id)
    ElMessage.success('模型已上线')
  }
  await load()
}

function openCreate(mode) {
  createMode.value = mode
  Object.assign(createForm, {
    inheritFromId: null,
    code: '',
    name: '',
    categoryId: currentCategory.value?.id ?? null,
    dept: '',
    description: ''
  })
  loadCategoryTree()
  loadAllModels()
  createVisible.value = true
}

function openInherit(row) {
  openCreate('INHERIT')
  createForm.inheritFromId = row.id
  createForm.name = `${row.name}副本`
  createForm.categoryId = row.categoryId ?? createForm.categoryId
  createForm.dept = row.dept || ''
}

async function saveCreate() {
  if (createMode.value === 'INHERIT' && !createForm.inheritFromId) {
    ElMessage.warning('请选择来源模型')
    return
  }
  if (!createForm.code.trim() || !createForm.name.trim()) {
    ElMessage.warning('编码与名称为必填项')
    return
  }
  if (!createForm.categoryId) {
    ElMessage.warning('请选择所属分类')
    return
  }
  creating.value = true
  try {
    const created = await createModel({
      mode: createMode.value,
      inheritFromId: createForm.inheritFromId,
      code: createForm.code.trim(),
      name: createForm.name.trim(),
      categoryId: createForm.categoryId,
      dept: createForm.dept,
      description: createForm.description
    })
    ElMessage.success('模型已创建')
    createVisible.value = false
    await load()
    if (created?.id) {
      router.push(`/models/${created.id}/design`)
    }
  } finally {
    creating.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除模型「${row.name}」？存在关联数据或引用时不允许删除。`,
      '删除确认',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  await deleteModel(row.id)
  ElMessage.success('模型已删除')
  await load()
}

function openVersions(row) {
  versionsModelId.value = row.id
  versionsVisible.value = true
}

onMounted(async () => {
  await Promise.all([load(), loadAllModels(), loadCategoryTree()])
})
</script>

<style scoped>
.model-page {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.side {
  width: 280px;
  flex-shrink: 0;
}

.content {
  flex: 1;
  min-width: 0;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.spacer {
  flex: 1;
}

.category-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  color: #606266;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
