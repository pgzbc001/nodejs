<template>
  <div v-loading="loading" class="page-container">
    <!-- 头部 -->
    <div class="page-card designer-header">
      <div class="left">
        <el-button link @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <span class="model-name">{{ model?.name || '-' }}</span>
        <el-tag size="small" type="info">{{ model?.code }}</el-tag>
        <el-tag size="small" :type="isOnline ? 'success' : 'info'">
          {{ isOnline ? '已上线' : '未上线' }}
        </el-tag>
        <span class="text-muted">v{{ model?.versionNo }}</span>
      </div>
      <div class="right">
        <el-button v-if="isOnline" size="small" @click="toggleOnline">下线</el-button>
        <el-button v-else size="small" type="success" @click="toggleOnline">上线</el-button>
        <el-button size="small" type="primary" :loading="saving" @click="saveAll">保存</el-button>
      </div>
    </div>

    <div class="page-card">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <!-- ========== 基础信息 ========== -->
        <el-tab-pane label="基础信息" name="basic">
          <el-form :model="baseForm" label-width="110px" style="max-width: 560px; padding-top: 8px">
            <el-form-item label="模型编码">
              <el-input v-model="baseForm.code" disabled />
            </el-form-item>
            <el-form-item label="模型名称" required>
              <el-input v-model="baseForm.name" />
            </el-form-item>
            <el-form-item label="所属分类" required>
              <el-tree-select
                v-model="baseForm.categoryId"
                :data="categoryTree"
                :props="{ label: 'name', children: 'children' }"
                node-key="id"
                check-strictly
                default-expand-all
                :render-after-expand="false"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="归口部门">
              <el-input v-model="baseForm.dept" />
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="baseForm.description" type="textarea" :rows="3" />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- ========== 字段配置 ========== -->
        <el-tab-pane label="字段配置" name="fields">
          <div class="tab-toolbar">
            <el-button type="primary" size="small" @click="addField">新增字段</el-button>
            <span class="text-muted">
              共 {{ fieldDefs.length }} 个字段；字段名（英文标识）将用于表单/列表取值
            </span>
          </div>
          <el-alert
            v-if="isOnline"
            type="warning"
            :closable="false"
            title="模型已上线：仅可新增字段与调整展示类属性（标签/分组/列表展示/检索/弹窗/默认值），其余结构属性锁定"
            class="tab-alert"
          />

          <el-table :data="fieldDefs" border size="small" max-height="480">
            <el-table-column label="字段名" width="150">
              <template #default="{ row }">
                <el-input v-model="row.name" size="small" :disabled="lockStructural(row)" />
              </template>
            </el-table-column>
            <el-table-column label="显示名" width="140">
              <template #default="{ row }">
                <el-input v-model="row.label" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="类型" width="120">
              <template #default="{ row }">
                <el-select v-model="row.type" size="small" :disabled="lockStructural(row)">
                  <el-option value="TEXT" label="TEXT 文本" />
                  <el-option value="LONG_TEXT" label="LONG_TEXT 长文本" />
                  <el-option value="NUMBER" label="NUMBER 数值" />
                  <el-option value="DATE" label="DATE 日期" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="必填" width="70" align="center">
              <template #default="{ row }">
                <el-switch v-model="row.required" size="small" :disabled="lockStructural(row)" />
              </template>
            </el-table-column>
            <el-table-column label="唯一" width="70" align="center">
              <template #default="{ row }">
                <el-switch v-model="row.unique" size="small" :disabled="lockStructural(row)" />
              </template>
            </el-table-column>
            <el-table-column label="列表" width="70" align="center">
              <template #default="{ row }">
                <el-switch v-model="row.listShow" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="检索" width="70" align="center">
              <template #default="{ row }">
                <el-switch v-model="row.searchable" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="弹窗" width="70" align="center">
              <template #default="{ row }">
                <el-switch v-model="row.popup" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="分组" width="110">
              <template #default="{ row }">
                <el-input v-model="row.group" size="small" placeholder="如 基本信息" />
              </template>
            </el-table-column>
            <el-table-column label="值域" width="100">
              <template #default="{ row }">
                <span>{{ domainLabel(row) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row, $index }">
                <el-button link type="primary" size="small" @click="openFieldDetail(row)">配置</el-button>
                <el-button
                  link
                  type="danger"
                  size="small"
                  :disabled="lockStructural(row)"
                  @click="removeField($index)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无字段，请新增" :image-size="60" />
            </template>
          </el-table>
        </el-tab-pane>

        <!-- ========== 扩展配置 ========== -->
        <el-tab-pane label="扩展配置" name="ext">
          <el-form :model="extForm" label-width="130px" style="max-width: 560px; padding-top: 8px">
            <el-form-item label="流程标题字段">
              <el-select
                v-model="extForm.titleField"
                filterable
                allow-create
                clearable
                placeholder="选择用于展示标题的字段名"
                style="width: 100%"
              >
                <el-option v-for="f in fieldDefs" :key="f.name" :value="f.name" :label="`${f.label}（${f.name}）`" />
              </el-select>
            </el-form-item>
            <el-form-item label="树形结构">
              <el-switch v-model="extForm.treeEnabled" />
              <span class="text-muted hint">开启后数据以树形展示（自关联上级/子级字段）</span>
            </el-form-item>
            <template v-if="extForm.treeEnabled">
              <el-form-item label="上级字段">
                <el-select v-model="extForm.parentField" filterable allow-create clearable style="width: 100%">
                  <el-option v-for="f in fieldDefs" :key="f.name" :value="f.name" :label="`${f.label}（${f.name}）`" />
                </el-select>
              </el-form-item>
              <el-form-item label="子级字段">
                <el-select v-model="extForm.childField" filterable allow-create clearable style="width: 100%">
                  <el-option v-for="f in fieldDefs" :key="f.name" :value="f.name" :label="`${f.label}（${f.name}）`" />
                </el-select>
              </el-form-item>
              <el-form-item label="展示字段">
                <el-select v-model="extForm.displayField" filterable allow-create clearable style="width: 100%">
                  <el-option v-for="f in fieldDefs" :key="f.name" :value="f.name" :label="`${f.label}（${f.name}）`" />
                </el-select>
              </el-form-item>
            </template>
          </el-form>
        </el-tab-pane>

        <!-- ========== 编码规则 ========== -->
        <el-tab-pane label="编码规则" name="codeRule">
          <div class="tab-toolbar">
            <el-button type="primary" size="small" @click="addSegment">新增段</el-button>
            <span class="text-muted">编码段按顺序拼接生成编码；MODEL_REF（编码引用）仅允许作为第一段</span>
          </div>

          <div v-for="(seg, index) in codeRules" :key="index" class="segment-card">
            <div class="segment-head">
              <span class="segment-index">段 {{ index + 1 }}</span>
              <el-select v-model="seg.type" size="small" style="width: 170px">
                <el-option value="FIXED" label="FIXED 固定值" />
                <el-option value="FIELD_REF" label="FIELD_REF 字段引用" />
                <el-option value="SEQ" label="SEQ 顺序值" />
                <el-option value="MODEL_REF" label="MODEL_REF 编码引用" />
              </el-select>
              <span class="spacer" />
              <el-button link size="small" :disabled="index === 0" @click="moveSegment(index, -1)">上移</el-button>
              <el-button link size="small" :disabled="index === codeRules.length - 1" @click="moveSegment(index, 1)">下移</el-button>
              <el-button link type="danger" size="small" @click="removeSegment(index)">删除</el-button>
            </div>

            <div class="segment-body">
              <template v-if="seg.type === 'FIXED'">
                <el-input v-model="seg.value" size="small" placeholder="固定文本，如 MDM-" style="width: 240px" />
              </template>
              <template v-else-if="seg.type === 'FIELD_REF'">
                <el-select v-model="seg.value" size="small" filterable allow-create placeholder="选择字段" style="width: 240px">
                  <el-option v-for="f in fieldDefs" :key="f.name" :value="f.name" :label="`${f.label}（${f.name}）`" />
                </el-select>
                <span class="text-muted">取该字段值作为编码段</span>
              </template>
              <template v-else-if="seg.type === 'SEQ'">
                <div class="seq-row">
                  <el-input-number v-model="seg.seqStart" size="small" :min="0" :controls="false" placeholder="起始值" style="width: 110px" />
                  <el-input-number v-model="seg.seqStep" size="small" :min="1" :controls="false" placeholder="步长" style="width: 90px" />
                  <el-input-number v-model="seg.length" size="small" :min="1" :max="12" :controls="false" placeholder="位数" style="width: 90px" />
                  <el-input v-model="seg.padChar" size="small" placeholder="补位符" style="width: 90px" />
                  <el-select v-model="seg.padSide" size="small" style="width: 110px">
                    <el-option value="LEFT" label="左补位" />
                    <el-option value="RIGHT" label="右补位" />
                  </el-select>
                </div>
              </template>
              <template v-else>
                <el-select v-model="seg.refModelCode" size="small" filterable placeholder="被引用模型" style="width: 220px">
                  <el-option v-for="m in allModels" :key="m.code" :value="m.code" :label="`${m.name}（${m.code}）`" />
                </el-select>
                <el-select v-model="seg.refField" size="small" filterable allow-create placeholder="引用字段名（取其值）" style="width: 220px">
                  <el-option v-for="f in refFields(seg.refModelCode)" :key="f.name" :value="f.name" :label="`${f.label}（${f.name}）`" />
                </el-select>
              </template>
            </div>
          </div>
          <el-empty v-if="!codeRules.length" description="暂无编码规则（保存数据时将自动生成编码）" :image-size="60" />

          <el-alert v-if="codeRules.length" type="success" :closable="false" class="preview-alert">
            <template #title>
              预览示例编码：<b>{{ codePreview }}</b>
            </template>
          </el-alert>
        </el-tab-pane>

        <!-- ========== 版本历史 ========== -->
        <el-tab-pane label="版本历史" name="versions">
          <div class="tab-toolbar">
            <el-button
              size="small"
              type="primary"
              :disabled="selectedVersions.length !== 2"
              @click="openDiff"
            >
              对比选中两版本
            </el-button>
            <span class="text-muted">勾选任意两个版本查看差异；回滚将以该版本快照为准并生成新版本</span>
          </div>
          <el-table
            ref="versionTableRef"
            v-loading="versionLoading"
            :data="versions"
            border
            size="small"
            @selection-change="onVersionSelectionChange"
          >
            <el-table-column type="selection" width="42" />
            <el-table-column prop="versionNo" label="版本" width="70" />
            <el-table-column prop="operation" label="操作" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="operationType(row.operation)">{{ row.operation }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="模型状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'ONLINE' ? 'success' : 'info'">
                  {{ row.status === 'ONLINE' ? '已上线' : '未上线' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="operator" label="操作人" width="130" />
            <el-table-column prop="operatedTime" label="时间" min-width="170" />
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="rollback(row)">回滚</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无版本记录" :image-size="60" />
            </template>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 字段完整配置 -->
    <el-dialog v-model="fieldDialogVisible" :title="`字段配置：${fieldDialogRow?.label || ''}`" width="620px" append-to-body>
      <el-form v-if="fieldDialogRow" :model="fieldDialogRow" label-width="120px">
        <el-form-item label="字段名">
          <el-input v-model="fieldDialogRow.name" :disabled="lockStructural(fieldDialogRow)" />
        </el-form-item>
        <el-form-item label="显示名">
          <el-input v-model="fieldDialogRow.label" />
        </el-form-item>
        <el-form-item label="分组">
          <el-input v-model="fieldDialogRow.group" placeholder="表单分区名称，如 基本信息" />
        </el-form-item>
        <el-form-item label="下拉选项">
          <el-switch v-model="fieldDialogRow.selectable" :disabled="lockStructural(fieldDialogRow)" />
        </el-form-item>
        <el-form-item label="值域来源">
          <el-select v-model="fieldDialogRow.domainSource" clearable :disabled="lockStructural(fieldDialogRow)" style="width: 100%">
            <el-option value="MANUAL" label="MANUAL 手动维护固定值" />
            <el-option value="REF" label="REF 引用其他模型" />
          </el-select>
        </el-form-item>
        <template v-if="fieldDialogRow.domainSource === 'MANUAL'">
          <el-form-item label="固定值列表">
            <el-select
              v-model="fieldDialogRow.domainValues"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="输入后回车添加候选项"
              style="width: 100%"
            />
          </el-form-item>
        </template>
        <template v-if="fieldDialogRow.domainSource === 'REF'">
          <el-form-item label="引用模型">
            <el-select v-model="fieldDialogRow.refModelCode" filterable style="width: 100%">
              <el-option v-for="m in allModels" :key="m.code" :value="m.code" :label="`${m.name}（${m.code}）`" />
            </el-select>
          </el-form-item>
          <el-form-item label="引用过滤">
            <el-input v-model="fieldDialogRow.refFilter" placeholder="如 status=VALID" />
          </el-form-item>
          <el-form-item label="多选">
            <el-switch v-model="fieldDialogRow.multiSelect" />
          </el-form-item>
        </template>
        <el-form-item label="默认值">
          <el-input v-model="fieldDialogRow.defaultValue" />
        </el-form-item>
        <el-form-item label="数据分级">
          <el-select v-model="fieldDialogRow.securityLevel" clearable style="width: 100%">
            <el-option v-for="level in SECURITY_LEVELS" :key="level.value" :value="level.value" :label="level.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="加密存储">
          <el-switch v-model="fieldDialogRow.encrypted" :disabled="lockStructural(fieldDialogRow)" />
        </el-form-item>
        <el-form-item label="安全识别规则">
          <el-input v-model="fieldDialogRow.securityRule" placeholder="关联安全识别规则（可选）" />
        </el-form-item>
        <el-form-item label="自定义校验">
          <el-select v-model="fieldDialogRow.customRule" clearable style="width: 100%">
            <el-option value="PHONE" label="PHONE 手机号" />
            <el-option value="ID_CARD" label="ID_CARD 身份证" />
            <el-option value="NUMBER_RANGE" label="NUMBER_RANGE 数值范围" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="fieldDialogVisible = false">完成</el-button>
      </template>
    </el-dialog>

    <!-- 版本差异 -->
    <el-dialog v-model="diffVisible" :title="`模型版本差异 v${diffFrom} → v${diffTo}`" width="820px" append-to-body>
      <DiffTable :items="diffItems" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { fetchCategoryTree } from '../api/category'
import {
  diffModelVersions,
  fetchAllModels,
  fetchModelDetail,
  fetchModelVersions,
  offlineModel,
  onlineModel,
  rollbackModel,
  updateModel
} from '../api/model'
import DiffTable from '../components/DiffTable.vue'

// 模型设计器（REQ-FED03 / design 3.3）：基础信息 / 字段 / 扩展 / 编码规则 / 版本历史
const route = useRoute()
const router = useRouter()
const modelId = route.params.id

const SECURITY_LEVELS = [
  { value: 'PUBLIC', label: 'PUBLIC 公开' },
  { value: 'SENSITIVE', label: 'SENSITIVE 敏感' },
  { value: 'SECRET', label: 'SECRET 秘密' },
  { value: 'CONFIDENTIAL', label: 'CONFIDENTIAL 机密' },
  { value: 'TOP_SECRET', label: 'TOP_SECRET 绝密' }
]

const loading = ref(false)
const saving = ref(false)
const activeTab = ref('basic')

const model = ref(null)
const categoryTree = ref([])
const allModels = ref([])
const baseForm = reactive({ code: '', name: '', categoryId: null, dept: '', description: '' })
const fieldDefs = ref([])
const extForm = reactive({
  titleField: '',
  treeEnabled: false,
  parentField: '',
  childField: '',
  displayField: ''
})
const codeRules = ref([])

const isOnline = computed(() => model.value?.status === 'ONLINE')

function lockStructural(row) {
  return isOnline.value && !!row._origName
}

function domainLabel(row) {
  if (row.domainSource === 'MANUAL') return `固定值×${(row.domainValues || []).length}`
  if (row.domainSource === 'REF') return `引用 ${row.refModelCode || '-'}`
  if (row.selectable) return '下拉'
  return '-'
}

async function load() {
  loading.value = true
  try {
    const detail = await fetchModelDetail(modelId)
    model.value = detail
    Object.assign(baseForm, {
      code: detail.code,
      name: detail.name,
      categoryId: detail.categoryId,
      dept: detail.dept || '',
      description: detail.description || ''
    })
    fieldDefs.value = (detail.fieldDefs || []).map((f) => ({ ...f, _origName: f.name }))
    Object.assign(extForm, {
      titleField: detail.extConfig?.titleField || '',
      treeEnabled: !!detail.extConfig?.treeEnabled,
      parentField: detail.extConfig?.parentField || '',
      childField: detail.extConfig?.childField || '',
      displayField: detail.extConfig?.displayField || ''
    })
    codeRules.value = (detail.codeRules || []).map((s) => ({ ...s }))
  } finally {
    loading.value = false
  }
}

async function loadMeta() {
  const [tree, models] = await Promise.all([fetchCategoryTree(), fetchAllModels()])
  categoryTree.value = tree || []
  allModels.value = models?.list || []
}

// ---------- 保存 ----------
function cleanedFields() {
  return fieldDefs.value.map((f) => {
    const { _origName, ...rest } = f
    return rest
  })
}

async function saveAll() {
  if (!baseForm.name.trim()) {
    ElMessage.warning('模型名称不能为空')
    return
  }
  for (const f of fieldDefs.value) {
    if (!f.name?.trim() || !f.label?.trim()) {
      ElMessage.warning('每个字段的字段名与显示名均为必填')
      return
    }
  }
  const names = fieldDefs.value.map((f) => f.name.trim())
  if (new Set(names).size !== names.length) {
    ElMessage.warning('字段名不允许重复')
    return
  }
  saving.value = true
  try {
    await updateModel(modelId, {
      name: baseForm.name.trim(),
      categoryId: baseForm.categoryId,
      dept: baseForm.dept,
      description: baseForm.description,
      fieldDefs: cleanedFields(),
      codeRules: codeRules.value,
      extConfig: { ...extForm }
    })
    ElMessage.success('模型已保存')
    await load()
  } finally {
    saving.value = false
  }
}

async function toggleOnline() {
  try {
    await ElMessageBox.confirm(
      isOnline.value
        ? '确认下线模型？下线后数据维护页将不再可见。'
        : '确认上线模型？上线后字段结构将被锁定（仅可新增字段与调整展示属性）。',
      isOnline.value ? '下线确认' : '上线确认',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  if (isOnline.value) {
    await offlineModel(modelId)
    ElMessage.success('模型已下线')
  } else {
    await onlineModel(modelId)
    ElMessage.success('模型已上线')
  }
  await load()
}

function goBack() {
  router.push('/models')
}

// ---------- 字段配置 ----------
function addField() {
  fieldDefs.value.push({
    name: '',
    label: '',
    type: 'TEXT',
    required: false,
    unique: false,
    listShow: true,
    selectable: false,
    multiSelect: false,
    searchable: false,
    popup: false,
    encrypted: false,
    _origName: null
  })
}

function removeField(index) {
  fieldDefs.value.splice(index, 1)
}

const fieldDialogVisible = ref(false)
const fieldDialogRow = ref(null)

function openFieldDetail(row) {
  fieldDialogRow.value = row
  fieldDialogVisible.value = true
}

// ---------- 编码规则 ----------
function addSegment() {
  codeRules.value.push({ type: 'FIXED', value: '' })
}

function removeSegment(index) {
  codeRules.value.splice(index, 1)
}

function moveSegment(index, delta) {
  const target = index + delta
  if (target < 0 || target >= codeRules.value.length) return
  const list = codeRules.value
  ;[list[index], list[target]] = [list[target], list[index]]
}

function refFields(refModelCode) {
  if (!refModelCode) return []
  return allModels.value.find((m) => m.code === refModelCode)?.fieldDefs || []
}

const codePreview = computed(() =>
  codeRules.value
    .map((seg) => {
      if (seg.type === 'FIXED') return seg.value || ''
      if (seg.type === 'FIELD_REF') return `{${seg.value || '字段'}}`
      if (seg.type === 'SEQ') {
        const length = seg.length || 4
        const start = String(seg.seqStart ?? 1)
        const pad = seg.padChar || '0'
        return seg.padSide === 'RIGHT' ? start.padEnd(length, pad) : start.padStart(length, pad)
      }
      if (seg.type === 'MODEL_REF') {
        return `{${seg.refField || '引用值'}@${seg.refModelCode || '模型'}}`
      }
      return ''
    })
    .join('')
)

// ---------- 版本历史 ----------
const versionLoading = ref(false)
const versions = ref([])
const versionTableRef = ref(null)
const selectedVersions = ref([])
const diffVisible = ref(false)
const diffFrom = ref(null)
const diffTo = ref(null)
const diffItems = ref([])

async function loadVersions() {
  versionLoading.value = true
  try {
    versions.value = (await fetchModelVersions(modelId)) || []
  } finally {
    versionLoading.value = false
  }
}

function onVersionSelectionChange(rows) {
  if (rows.length > 2) {
    versionTableRef.value.toggleRowSelection(rows[rows.length - 1], false)
    return
  }
  selectedVersions.value = rows
}

async function openDiff() {
  const sorted = [...selectedVersions.value].sort((a, b) => a.versionNo - b.versionNo)
  diffFrom.value = sorted[0].versionNo
  diffTo.value = sorted[1].versionNo
  const result = await diffModelVersions(modelId, diffFrom.value, diffTo.value)
  diffItems.value = (result?.changes || []).map((change) => ({
    field: change.field,
    label: change.type,
    oldValue: change.oldValue,
    newValue: change.newValue
  }))
  diffVisible.value = true
}

async function rollback(row) {
  try {
    await ElMessageBox.confirm(
      `确认回滚模型到版本 v${row.versionNo}？回滚后模型定义将以该版本快照为准。`,
      '回滚确认',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  await rollbackModel(modelId, row.versionNo)
  ElMessage.success(`模型已回滚到版本 v${row.versionNo}`)
  await load()
  await loadVersions()
}

function operationType(operation) {
  return {
    CREATE: 'success',
    UPDATE: 'primary',
    ONLINE: 'success',
    OFFLINE: 'info',
    ROLLBACK: 'warning'
  }[operation] || 'info'
}

function onTabChange(name) {
  if (name === 'versions' && !versions.value.length) loadVersions()
}

onMounted(async () => {
  await Promise.all([load(), loadMeta()])
})
</script>

<style scoped>
.designer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding: 12px 16px;
}

.designer-header .left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.model-name {
  font-size: 16px;
  font-weight: 600;
}

.tab-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.tab-alert {
  margin-bottom: 10px;
}

.spacer {
  flex: 1;
}

.segment-card {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 10px 12px;
  margin-bottom: 10px;
}

.segment-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.segment-index {
  font-weight: 600;
  color: #409eff;
}

.segment-body {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.seq-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.preview-alert {
  margin-top: 10px;
}

.hint {
  margin-left: 10px;
  font-size: 12px;
}
</style>
