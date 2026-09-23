<template>
  <div class="quality-panel" v-if="violations.length">
    <!-- 严重级：阻止提交 -->
    <template v-if="criticalList.length">
      <el-alert
        type="error"
        :closable="false"
        show-icon
        title="存在严重级质量问题，必须修正后才能提交"
        class="mb8"
      />
      <div v-for="item in criticalList" :key="'c-' + item.ruleId" class="violation-item critical">
        <el-tag type="danger" size="small">严重</el-tag>
        <span class="field-name">{{ fieldLabel(item.fieldName) }}</span>
        <span class="message">{{ item.message }}</span>
      </div>
    </template>

    <!-- 警告级：可忽略（需原因） -->
    <template v-if="warningList.length">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="存在告警级质量问题，可填写原因后忽略"
        class="mb8"
      />
      <div v-for="item in warningList" :key="'w-' + item.ruleId" class="violation-item warning">
        <el-checkbox
          :model-value="ignoredIds.includes(item.ruleId)"
          @update:model-value="toggle(item.ruleId, $event)"
        >
          <el-tag type="warning" size="small">告警</el-tag>
          <span class="field-name">{{ fieldLabel(item.fieldName) }}</span>
          <span class="message">{{ item.message }}</span>
        </el-checkbox>
      </div>
    </template>

    <!-- 提示级：可忽略（需原因） -->
    <template v-if="infoList.length">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="存在提示级质量问题，可填写原因后忽略"
        class="mb8"
      />
      <div v-for="item in infoList" :key="'i-' + item.ruleId" class="violation-item info">
        <el-checkbox
          :model-value="ignoredIds.includes(item.ruleId)"
          @update:model-value="toggle(item.ruleId, $event)"
        >
          <el-tag type="info" size="small">提示</el-tag>
          <span class="field-name">{{ fieldLabel(item.fieldName) }}</span>
          <span class="message">{{ item.message }}</span>
        </el-checkbox>
      </div>
    </template>

    <!-- 忽略原因（存在被勾选的告警/提示时必填） -->
    <div v-if="ignoredIds.length" class="reason-row">
      <div class="reason-label">忽略原因（必填）</div>
      <el-input
        :model-value="reason"
        type="textarea"
        :rows="2"
        placeholder="请说明忽略原因，例如：已人工复核确认无误"
        @update:model-value="$emit('update:reason', $event)"
      />
    </div>
  </div>
  <el-empty v-else description="未发现质量问题" :image-size="60" />
</template>

<script setup>
import { computed } from 'vue'

// 质量三级面板（REQ-FED06 / design 3.4）：CRITICAL 禁提交；WARNING/INFO 勾选忽略 + 原因
const props = defineProps({
  violations: { type: Array, default: () => [] },
  ignoredIds: { type: Array, default: () => [] },
  reason: { type: String, default: '' }
})

const emit = defineEmits(['update:ignoredIds', 'update:reason'])

const criticalList = computed(() =>
  props.violations.filter((v) => v.severity === 'CRITICAL')
)
const warningList = computed(() => props.violations.filter((v) => v.severity === 'WARNING'))
const infoList = computed(() => props.violations.filter((v) => v.severity === 'INFO'))

function toggle(ruleId, checked) {
  const next = new Set(props.ignoredIds)
  if (checked) {
    next.add(ruleId)
  } else {
    next.delete(ruleId)
  }
  emit('update:ignoredIds', Array.from(next))
  if (next.size === 0) {
    emit('update:reason', '')
  }
}

function fieldLabel(fieldName) {
  return fieldName ? `【${fieldName}】` : ''
}
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}

.violation-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  border-radius: 4px;
  margin-bottom: 6px;
}

.violation-item.critical {
  background: #fef0f0;
}

.violation-item.warning {
  background: #fdf6ec;
}

.violation-item.info {
  background: #f4f4f5;
}

.field-name {
  font-weight: 600;
  margin: 0 4px;
}

.message {
  color: #606266;
}

.reason-row {
  margin-top: 10px;
}

.reason-label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 4px;
}
</style>
