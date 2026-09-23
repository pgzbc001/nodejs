<template>
  <!-- LONG_TEXT 多行文本 -->
  <el-input
    v-if="type === 'LONG_TEXT'"
    type="textarea"
    :rows="3"
    :model-value="textValue"
    :disabled="disabled"
    :placeholder="placeholder"
    @update:model-value="update"
  />
  <!-- NUMBER 数值 -->
  <el-input-number
    v-else-if="type === 'NUMBER'"
    :model-value="numberValue"
    :disabled="disabled"
    :placeholder="placeholder"
    :controls="false"
    style="width: 100%"
    @update:model-value="update"
  />
  <!-- DATE 日期 -->
  <el-date-picker
    v-else-if="type === 'DATE'"
    :model-value="textValue || null"
    type="date"
    value-format="YYYY-MM-DD"
    :disabled="disabled"
    :placeholder="placeholder"
    style="width: 100%"
    @update:model-value="update"
  />
  <!-- REF 引用字段：手输编码 + 弹窗选择 -->
  <el-input
    v-else-if="isRef"
    :model-value="refTextValue"
    :disabled="disabled"
    :placeholder="refPlaceholder"
    @update:model-value="updateRefValue"
  >
    <template #append>
      <el-button :disabled="disabled" @click="emit('open-ref', field)">选择</el-button>
    </template>
  </el-input>
  <!-- SELECT 下拉（手动值域） -->
  <el-select
    v-else-if="isSelect"
    :model-value="selectValue"
    :multiple="isMulti"
    :disabled="disabled"
    :placeholder="placeholder"
    filterable
    clearable
    style="width: 100%"
    @update:model-value="update"
  >
    <el-option
      v-for="option in field.domainValues || []"
      :key="option"
      :value="option"
      :label="option"
    />
  </el-select>
  <!-- 默认 TEXT -->
  <el-input
    v-else
    :model-value="textValue"
    :disabled="disabled"
    :placeholder="placeholder"
    @update:model-value="update"
  />
</template>

<script setup>
import { computed } from 'vue'

// 动态字段渲染（REQ-FED05）：按 FieldDef.type + 属性映射控件
const props = defineProps({
  field: { type: Object, required: true },
  modelValue: { type: [String, Number, Array, null], default: null },
  disabled: { type: Boolean, default: false },
  placeholder: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'open-ref'])

const type = computed(() => (props.field?.type || 'TEXT').toUpperCase())
const isRef = computed(() => props.field?.domainSource === 'REF')
const isSelect = computed(
  () => !isRef.value && props.field?.selectable && (props.field?.domainValues || []).length > 0
)
const isMulti = computed(() => Boolean(props.field?.multiSelect))

const label = computed(() => props.field?.label || props.field?.name || '')
const placeholder = computed(
  () =>
    props.placeholder ||
    (isSelect.value || isRef.value ? `请选择${label.value}` : `请输入${label.value}`)
)
const refPlaceholder = computed(
  () => props.placeholder || `请输入或选择${label.value}编码`
)

// 文本类模型值
const textValue = computed(() => {
  const value = props.modelValue
  if (value === null || value === undefined) return ''
  if (Array.isArray(value)) return value.join(',')
  return String(value)
})

// NUMBER 数值（空值转 undefined 以显示 placeholder）
const numberValue = computed(() => {
  const value = props.modelValue
  if (value === null || value === undefined || value === '') return undefined
  const num = Number(value)
  return Number.isNaN(num) ? undefined : num
})

// SELECT 值（多选保证数组）
const selectValue = computed(() => {
  const value = props.modelValue
  if (isMulti.value) {
    if (Array.isArray(value)) return value
    return value ? String(value).split(',').filter(Boolean) : []
  }
  if (Array.isArray(value)) return value[0] ?? ''
  return value ?? ''
})

// REF 值文本
const refTextValue = computed(() => {
  const value = props.modelValue
  if (Array.isArray(value)) return value.join(',')
  return value === null || value === undefined ? '' : String(value)
})

function update(value) {
  emit('update:modelValue', value === undefined ? null : value)
}

function updateRefValue(value) {
  emit('update:modelValue', value === '' ? null : value)
}
</script>
