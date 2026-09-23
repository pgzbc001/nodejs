<template>
  <div class="page-container">
    <div class="page-card">
      <div class="toolbar">
        <el-select
          v-model="filterModelId"
          placeholder="适用模型"
          clearable
          style="width: 240px"
          @change="load"
        >
          <el-option :value="null" label="全局规则" />
          <el-option
            v-for="m in models"
            :key="m.id"
            :value="m.id"
            :label="`${m.name}（${m.code}）`"
          />
        </el-select>
        <el-button type="primary" @click="openCreate">新增规则</el-button>
        <el-button @click="load">刷新</el-button>
        <span class="text-muted">三级告警：CRITICAL 阻止提交，WARNING/INFO 可填原因忽略</span>
      </div>

      <el-table v-loading="loading" :data="rules" border size="small">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="适用模型" min-width="150">
          <template #default="{ row }">{{ modelName(row.modelId) }}</template>
        </el-table-column>
        <el-table-column label="规则类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="ruleTypeTag(row.ruleType)">
              {{ ruleTypeLabel(row.ruleType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fieldName" label="目标字段" width="130" />
        <el-table-column label="表达式" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ formatExpression(row) }}</template>
        </el-table-column>
        <el-table-column label="等级" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="severityTag(row.severity)">{{ row.severity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="提示信息" min-width="170" show-overflow-tooltip />
        <el-table-column label="启用" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.enabled === 1 ? 'success' : 'info'">
              {{ row.enabled === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无质量规则" :image-size="60" />
        </template>
      </el-table>
    </div>

    <!-- 新增/编辑规则 -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑质量规则' : '新增质量规则'"
      width="640px"
      append-to-body
    >
      <el-form :model="form" label-width="110px">
        <el-form-item label="适用模型">
          <el-select
            v-model="form.modelId"
            clearable
            style="width: 100%"
            @change="onFormModelChange"
          >
            <el-option :value="null" label="全局规则（所有模型）" />
            <el-option
              v-for="m in models"
              :key="m.id"
              :value="m.id"
              :label="`${m.name}（${m.code}）`"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="规则类型" required>
          <el-radio-group v-model="form.ruleType">
            <el-radio-button value="COMPLIANCE">合规</el-radio-button>
            <el-radio-button value="CONSISTENCY">一致</el-radio-button>
            <el-radio-button value="COMPLETENESS">完整</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="目标字段" required>
          <el-select
            v-model="form.fieldName"
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入字段名"
            style="width: 100%"
          >
            <el-option
              v-for="f in fieldOptions"
              :key="f.name"
              :value="f.name"
              :label="`${f.label}（${f.name}）`"
            />
          </el-select>
        </el-form-item>

        <!-- 合规：长度 / 正则 / 数值范围 -->
        <template v-if="form.ruleType === 'COMPLIANCE'">
          <el-form-item label="合规操作符" required>
            <el-select v-model="compliance.op" style="width: 100%">
              <el-option value="LENGTH_MAX" label="最大长度（LENGTH_MAX）" />
              <el-option value="REGEX" label="正则匹配（REGEX）" />
              <el-option value="NUM_RANGE" label="数值范围（NUM_RANGE）" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="compliance.op === 'LENGTH_MAX'" label="最大长度" required>
            <el-input-number v-model="compliance.maxLength" :min="1" :controls="false" style="width: 100%" />
          </el-form-item>
          <el-form-item v-else-if="compliance.op === 'REGEX'" label="正则表达式" required>
            <el-input v-model="compliance.regex" placeholder="如 ^[A-Z0-9-]+$" />
          </el-form-item>
          <el-form-item v-else label="数值范围" required>
            <div class="range-row">
              <el-input-number v-model="compliance.min" :controls="false" style="width: 140px" />
              <span class="text-muted">至</span>
              <el-input-number v-model="compliance.max" :controls="false" style="width: 140px" />
            </div>
          </el-form-item>
        </template>

        <!-- 一致：值域归属 / 字段间比较 -->
        <template v-else-if="form.ruleType === 'CONSISTENCY'">
          <el-form-item label="一致类型" required>
            <el-radio-group v-model="consistency.mode">
              <el-radio value="inDomain">值域归属</el-radio>
              <el-radio value="compare">字段间比较</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="consistency.mode === 'inDomain'" label="值域来源字段" required>
            <el-select
              v-model="consistency.refField"
              filterable
              allow-create
              placeholder="提供可选值域的字段名"
              style="width: 100%"
            >
              <el-option
                v-for="f in fieldOptions"
                :key="f.name"
                :value="f.name"
                :label="`${f.label}（${f.name}）`"
              />
            </el-select>
          </el-form-item>
          <template v-else>
            <el-form-item label="字段 A" required>
              <el-select v-model="consistency.fieldA" filterable allow-create style="width: 100%">
                <el-option
                  v-for="f in fieldOptions"
                  :key="f.name"
                  :value="f.name"
                  :label="`${f.label}（${f.name}）`"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="比较符" required>
              <el-select v-model="consistency.op" style="width: 100%">
                <el-option v-for="op in COMPARE_OPS" :key="op.value" :value="op.value" :label="op.label" />
              </el-select>
            </el-form-item>
            <el-form-item label="字段 B" required>
              <el-select v-model="consistency.fieldB" filterable allow-create style="width: 100%">
                <el-option
                  v-for="f in fieldOptions"
                  :key="f.name"
                  :value="f.name"
                  :label="`${f.label}（${f.name}）`"
                />
              </el-select>
            </el-form-item>
          </template>
        </template>

        <!-- 完整：非空校验 -->
        <el-form-item v-else label="校验方式">
          <el-alert
            type="info"
            :closable="false"
            title="完整性校验：目标字段值不允许为空"
          />
        </el-form-item>

        <el-form-item label="告警等级" required>
          <el-select v-model="form.severity" style="width: 100%">
            <el-option value="CRITICAL" label="CRITICAL（阻止提交，必须修正）" />
            <el-option value="WARNING" label="WARNING（可填原因忽略）" />
            <el-option value="INFO" label="INFO（提示信息）" />
          </el-select>
        </el-form-item>
        <el-form-item label="提示信息" required>
          <el-input v-model="form.message" placeholder="违规时展示给用户的说明文案" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchAllModels, fetchModelDetail } from '../api/model'
import {
  createQualityRule,
  deleteQualityRule,
  fetchQualityRules,
  updateQualityRule
} from '../api/quality'

// 质量规则管理（REQ-BKD10 / REQ-FED06）：模型筛选 + CRUD + 表达式可视化编辑
const COMPARE_OPS = [
  { value: 'GT', label: '大于（GT）' },
  { value: 'GTE', label: '大于等于（GTE）' },
  { value: 'LT', label: '小于（LT）' },
  { value: 'LTE', label: '小于等于（LTE）' },
  { value: 'EQ', label: '等于（EQ）' },
  { value: 'NE', label: '不等于（NE）' }
]

const loading = ref(false)
const saving = ref(false)
const rules = ref([])
const models = ref([])
const filterModelId = ref(null)

const dialogVisible = ref(false)
const fieldOptions = ref([])
const form = reactive({
  id: null,
  modelId: null,
  ruleType: 'COMPLIANCE',
  fieldName: '',
  severity: 'WARNING',
  message: '',
  enabled: true
})
const compliance = reactive({ op: 'LENGTH_MAX', maxLength: 20, regex: '', min: 0, max: 100 })
const consistency = reactive({ mode: 'inDomain', refField: '', fieldA: '', fieldB: '', op: 'GT' })

async function load() {
  loading.value = true
  try {
    rules.value = (await fetchQualityRules(filterModelId.value)) || []
  } finally {
    loading.value = false
  }
}

async function loadModels() {
  models.value = (await fetchAllModels())?.list || []
}

function modelName(modelId) {
  if (modelId === null || modelId === undefined) return '全局'
  return models.value.find((m) => m.id === modelId)?.name || `模型#${modelId}`
}

function ruleTypeLabel(type) {
  return { COMPLIANCE: '合规', CONSISTENCY: '一致', COMPLETENESS: '完整' }[type] || type
}

function ruleTypeTag(type) {
  return { COMPLIANCE: 'primary', CONSISTENCY: 'warning', COMPLETENESS: 'info' }[type] || 'info'
}

function severityTag(severity) {
  return { CRITICAL: 'danger', WARNING: 'warning', INFO: 'info' }[severity] || 'info'
}

function formatExpression(row) {
  try {
    const expr = typeof row.expression === 'string' ? JSON.parse(row.expression || '{}') : row.expression || {}
    if (row.ruleType === 'COMPLIANCE') {
      if (expr.op === 'LENGTH_MAX') return `长度 ≤ ${expr.value}`
      if (expr.op === 'REGEX') return `匹配 ${expr.value}`
      if (expr.op === 'NUM_RANGE') return `范围 [${(expr.value || []).join(', ')}]`
      return JSON.stringify(expr)
    }
    if (row.ruleType === 'CONSISTENCY') {
      if (expr.inDomain) return `值域归属：${expr.refField || row.fieldName}`
      return `${expr.fieldA} ${expr.op} ${expr.fieldB}`
    }
    return '非空校验'
  } catch (e) {
    return row.expression
  }
}

async function onFormModelChange(modelId) {
  fieldOptions.value = []
  if (modelId === null || modelId === undefined) return
  const detail = await fetchModelDetail(modelId)
  fieldOptions.value = detail?.fieldDefs || []
}

function resetForm() {
  Object.assign(form, {
    id: null,
    modelId: filterModelId.value,
    ruleType: 'COMPLIANCE',
    fieldName: '',
    severity: 'WARNING',
    message: '',
    enabled: true
  })
  Object.assign(compliance, { op: 'LENGTH_MAX', maxLength: 20, regex: '', min: 0, max: 100 })
  Object.assign(consistency, { mode: 'inDomain', refField: '', fieldA: '', fieldB: '', op: 'GT' })
  fieldOptions.value = []
}

function openCreate() {
  resetForm()
  if (form.modelId !== null && form.modelId !== undefined) onFormModelChange(form.modelId)
  dialogVisible.value = true
}

async function openEdit(row) {
  resetForm()
  form.id = row.id
  form.modelId = row.modelId ?? null
  form.ruleType = row.ruleType
  form.fieldName = row.fieldName || ''
  form.severity = row.severity
  form.message = row.message
  form.enabled = row.enabled === 1
  try {
    const expr = typeof row.expression === 'string' ? JSON.parse(row.expression || '{}') : row.expression || {}
    if (row.ruleType === 'COMPLIANCE') {
      compliance.op = expr.op || 'LENGTH_MAX'
      if (expr.op === 'LENGTH_MAX') compliance.maxLength = expr.value
      if (expr.op === 'REGEX') compliance.regex = expr.value
      if (expr.op === 'NUM_RANGE') {
        compliance.min = expr.value?.[0] ?? 0
        compliance.max = expr.value?.[1] ?? 100
      }
    } else if (row.ruleType === 'CONSISTENCY') {
      if (expr.inDomain) {
        consistency.mode = 'inDomain'
        consistency.refField = expr.refField || ''
      } else {
        consistency.mode = 'compare'
        consistency.fieldA = expr.fieldA || ''
        consistency.fieldB = expr.fieldB || ''
        consistency.op = expr.op || 'GT'
      }
    }
  } catch (e) {
    // 表达式非法时仅保留原文，不阻塞编辑
  }
  if (form.modelId !== null && form.modelId !== undefined) {
    await onFormModelChange(form.modelId)
  }
  dialogVisible.value = true
}

function buildExpression() {
  if (form.ruleType === 'COMPLIANCE') {
    if (compliance.op === 'LENGTH_MAX') return { op: 'LENGTH_MAX', value: compliance.maxLength }
    if (compliance.op === 'REGEX') return { op: 'REGEX', value: compliance.regex }
    return { op: 'NUM_RANGE', value: [compliance.min, compliance.max] }
  }
  if (form.ruleType === 'CONSISTENCY') {
    if (consistency.mode === 'inDomain') {
      return { inDomain: true, refField: consistency.refField }
    }
    return { fieldA: consistency.fieldA, fieldB: consistency.fieldB, op: consistency.op }
  }
  return {}
}

async function save() {
  if (!form.fieldName.trim()) {
    ElMessage.warning('请填写目标字段')
    return
  }
  if (!form.message.trim()) {
    ElMessage.warning('请填写提示信息')
    return
  }
  if (form.ruleType === 'COMPLIANCE') {
    if (compliance.op === 'REGEX' && !compliance.regex.trim()) {
      ElMessage.warning('请填写正则表达式')
      return
    }
  }
  if (form.ruleType === 'CONSISTENCY') {
    if (consistency.mode === 'inDomain' && !consistency.refField.trim()) {
      ElMessage.warning('请填写值域来源字段')
      return
    }
    if (consistency.mode === 'compare' && (!consistency.fieldA || !consistency.fieldB)) {
      ElMessage.warning('请完整选择字段 A / 字段 B')
      return
    }
  }
  saving.value = true
  try {
    const payload = {
      modelId: form.modelId,
      ruleType: form.ruleType,
      fieldName: form.fieldName.trim(),
      expression: buildExpression(),
      severity: form.severity,
      message: form.message.trim(),
      enabled: form.enabled
    }
    if (form.id) {
      await updateQualityRule(form.id, payload)
      ElMessage.success('规则已更新')
    } else {
      await createQualityRule(payload)
      ElMessage.success('规则已创建')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除规则 #${row.id}（${ruleTypeLabel(row.ruleType)} · ${row.fieldName}）？`,
      '删除确认',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  await deleteQualityRule(row.id)
  ElMessage.success('规则已删除')
  await load()
}

onMounted(async () => {
  await loadModels()
  await load()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.range-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>
