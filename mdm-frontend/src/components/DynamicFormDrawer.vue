<template>
  <el-drawer
    :model-value="visible"
    :title="dataId ? '编辑主数据' : '新增主数据'"
    size="680px"
    :close-on-click-modal="false"
    @update:model-value="(v) => $emit('update:visible', v)"
  >
    <el-form
      v-loading="loading"
      :model="attributes"
      label-width="120px"
      label-position="right"
    >
      <el-collapse v-model="activeGroups">
        <el-collapse-item v-for="group in groups" :key="group.name" :name="group.name">
          <template #title>
            <span class="group-title">{{ group.name }}</span>
          </template>
          <el-form-item
            v-for="field in group.fields"
            :key="field.name"
            :label="field.label"
            :required="field.required"
            :error="fieldErrors[field.name]"
          >
            <div class="field-wrap">
              <DynamicField
                v-model="attributes[field.name]"
                :field="field"
                :disabled="submitting"
                @open-ref="openRef"
              />
              <div class="field-tags" v-if="field.encrypted || field.unique">
                <el-tag v-if="field.encrypted" size="small" type="warning">
                  {{ securityLabel(field) }}·加密
                </el-tag>
                <el-tag v-if="field.unique" size="small" type="danger">唯一</el-tag>
              </div>
            </div>
          </el-form-item>
        </el-collapse-item>
      </el-collapse>
      <el-empty v-if="!groups.length" description="模型暂无字段定义" :image-size="60" />
    </el-form>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">
        {{ dataId ? '提交修改' : '提交' }}
      </el-button>
    </template>

    <!-- 查重弹窗 -->
    <SimilarityDialog
      v-model:visible="showSimilarity"
      :items="dupResult.items || []"
      :max-similarity="dupResult.maxSimilarity || 0"
      @continue="onDupContinue"
      @use-existing="onUseExisting"
    />

    <!-- 质量校验弹窗 -->
    <el-dialog
      v-model="showQuality"
      title="质量校验结果"
      width="640px"
      append-to-body
      :close-on-click-modal="false"
    >
      <QualityPanel
        v-model:ignored-ids="ignoredIds"
        v-model:reason="ignoreReason"
        :violations="violations"
      />
      <template #footer>
        <el-button @click="showQuality = false">返回修改</el-button>
        <el-button
          type="primary"
          :disabled="hasCritical || (ignoredIds.length > 0 && !ignoreReason.trim())"
          :loading="submitting"
          @click="confirmQuality"
        >
          {{ hasCritical ? '请先修正严重问题' : '确认提交' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 引用选择弹窗 -->
    <RefSelectDialog
      v-model:visible="showRef"
      :ref-model-code="refField?.refModelCode || ''"
      @picked="onRefPicked"
    />
  </el-drawer>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { checkDuplicate, checkQuality, createData, fetchDataDetail, updateData } from '../api/data'
import DynamicField from './DynamicField.vue'
import QualityPanel from './QualityPanel.vue'
import RefSelectDialog from './RefSelectDialog.vue'
import SimilarityDialog from './SimilarityDialog.vue'

// 动态表单抽屉（REQ-FED05）：分组渲染 + 查重 → 质量 → 提交完整链路
const props = defineProps({
  visible: { type: Boolean, default: false },
  modelId: { type: [Number, String], default: null },
  dataId: { type: [Number, String], default: null },
  viewSchema: { type: Object, default: null }
})

const emit = defineEmits(['update:visible', 'success', 'use-existing'])

const attributes = reactive({})
const fieldErrors = reactive({})
const loading = ref(false)
const submitting = ref(false)
const activeGroups = ref([])

const showSimilarity = ref(false)
const dupResult = ref({})
let pendingAttributes = null

const showQuality = ref(false)
const violations = ref([])
const ignoredIds = ref([])
const ignoreReason = ref('')

const showRef = ref(false)
const refField = ref(null)

const allFields = computed(() => props.viewSchema?.allFields || [])

// 按 field.group 分组（无分组归入「基本信息」）
const groups = computed(() => {
  const map = new Map()
  for (const field of allFields.value) {
    const name = (field.group || '').trim() || '基本信息'
    if (!map.has(name)) map.set(name, [])
    map.get(name).push(field)
  }
  return Array.from(map.entries()).map(([name, fields]) => ({ name, fields }))
})

const hasCritical = computed(() => violations.value.some((v) => v.severity === 'CRITICAL'))

watch(
  () => props.visible,
  async (visible) => {
    if (!visible) return
    reset()
    if (props.dataId) {
      await loadDetail()
    } else {
      applyDefaults()
    }
    activeGroups.value = groups.value.map((g) => g.name)
  }
)

function reset() {
  Object.keys(attributes).forEach((key) => delete attributes[key])
  Object.keys(fieldErrors).forEach((key) => delete fieldErrors[key])
  violations.value = []
  ignoredIds.value = []
  ignoreReason.value = ''
  dupResult.value = {}
  pendingAttributes = null
  showSimilarity.value = false
  showQuality.value = false
}

async function loadDetail() {
  loading.value = true
  try {
    const detail = await fetchDataDetail(props.modelId, props.dataId)
    Object.assign(attributes, detail?.data?.attributes || {})
  } finally {
    loading.value = false
  }
}

function applyDefaults() {
  allFields.value.forEach((field) => {
    if (field.defaultValue !== undefined && field.defaultValue !== null && field.defaultValue !== '') {
      attributes[field.name] = field.defaultValue
    }
  })
}

function validateRequired() {
  Object.keys(fieldErrors).forEach((key) => delete fieldErrors[key])
  let valid = true
  for (const field of allFields.value) {
    if (!field.required) continue
    const value = attributes[field.name]
    const empty =
      value === null ||
      value === undefined ||
      value === '' ||
      (Array.isArray(value) && value.length === 0)
    if (empty) {
      fieldErrors[field.name] = '必填字段不能为空'
      valid = false
    }
  }
  if (!valid) {
    ElMessage.warning('请完整填写必填字段')
  }
  return valid
}

function collectAttributes() {
  const result = {}
  Object.keys(attributes).forEach((key) => {
    const value = attributes[key]
    if (value === undefined) return
    result[key] = value
  })
  return result
}

async function submit() {
  if (!validateRequired()) return
  submitting.value = true
  try {
    const attrs = collectAttributes()
    // 1. 提交前 AI 查重
    const dup = await checkDuplicate(props.modelId, attrs, props.dataId || null)
    if (dup && (dup.items || []).length) {
      pendingAttributes = attrs
      dupResult.value = dup
      showSimilarity.value = true
      return
    }
    await checkQualityAndProceed(attrs)
  } catch (e) {
    applyFieldErrors(e)
  } finally {
    submitting.value = false
  }
}

async function onDupContinue() {
  showSimilarity.value = false
  submitting.value = true
  try {
    await checkQualityAndProceed(pendingAttributes)
  } catch (e) {
    applyFieldErrors(e)
  } finally {
    submitting.value = false
  }
}

function onUseExisting(item) {
  emit('use-existing', item)
  emit('update:visible', false)
  ElMessage.info(`已定位到已有数据：${item.code} ${item.name || ''}`)
}

// 2. 质量校验：存在违规 → 打开面板；否则直接落库
async function checkQualityAndProceed(attrs) {
  const result = await checkQuality(props.modelId, attrs)
  if (result && result.length) {
    pendingAttributes = attrs
    violations.value = result
    ignoredIds.value = []
    ignoreReason.value = ''
    showQuality.value = true
  } else {
    await realSubmit(attrs, [], null)
  }
}

async function confirmQuality() {
  submitting.value = true
  try {
    showQuality.value = false
    await realSubmit(pendingAttributes, ignoredIds.value, ignoreReason.value.trim() || null)
  } catch (e) {
    applyFieldErrors(e)
    showQuality.value = true
  } finally {
    submitting.value = false
  }
}

// 3. 落库
async function realSubmit(attrs, ignoredWarnings, ignoreReasonText) {
  const payload = {
    attributes: attrs,
    ignoredWarnings,
    ignoreReason: ignoreReasonText
  }
  if (props.dataId) {
    await updateData(props.modelId, props.dataId, payload)
    ElMessage.success('修改成功' + (ignoredWarnings.length ? `（已忽略 ${ignoredWarnings.length} 条告警）` : ''))
  } else {
    await createData(props.modelId, payload)
    ElMessage.success('新增成功' + (ignoredWarnings.length ? `（已忽略 ${ignoredWarnings.length} 条告警）` : ''))
  }
  emit('success')
  emit('update:visible', false)
}

// 字段级错误（40000 时 data 为 { 字段名: 提示 }）
function applyFieldErrors(error) {
  if (error?.code === 40000 && error.detail && typeof error.detail === 'object') {
    Object.assign(fieldErrors, error.detail)
  }
}

function openRef(field) {
  refField.value = field
  showRef.value = true
}

function onRefPicked(picked) {
  const field = refField.value
  if (!field) return
  if (field.multiSelect) {
    const current = attributes[field.name]
    const list = Array.isArray(current)
      ? [...current]
      : current
        ? String(current).split(',').filter(Boolean)
        : []
    if (!list.includes(picked.code)) list.push(picked.code)
    attributes[field.name] = list
  } else {
    attributes[field.name] = picked.code
  }
}

function securityLabel(field) {
  const map = {
    TOP_SECRET: '绝密',
    CONFIDENTIAL: '机密',
    SECRET: '秘密',
    SENSITIVE: '敏感',
    PUBLIC: '公开'
  }
  return map[field.securityLevel] || '敏感'
}
</script>

<style scoped>
.group-title {
  font-weight: 600;
  font-size: 14px;
}

.field-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.field-wrap > :first-child {
  flex: 1;
}

.field-tags {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}
</style>
