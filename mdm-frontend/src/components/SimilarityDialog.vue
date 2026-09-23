<template>
  <el-dialog
    :model-value="visible"
    title="AI 查重提示"
    width="720px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
  >
    <div class="summary" v-if="items.length">
      <div class="summary-left">
        <div class="summary-label">最高相似度</div>
        <el-progress
          :percentage="Math.round(maxSimilarity)"
          :color="progressColor(maxSimilarity)"
          :stroke-width="14"
          :text-inside="true"
        />
      </div>
      <div class="summary-tip" :class="{ danger: maxSimilarity >= 80 }">
        {{
          maxSimilarity >= 80
            ? '存在高度相似数据（≥80），建议使用已有数据或调整关键信息'
            : '存在相似数据（≥60），请确认是否为新增数据'
        }}
      </div>
    </div>

    <el-table :data="items" border size="small" highlight-current-row @current-change="onSelect">
      <el-table-column prop="code" label="编码" width="180" />
      <el-table-column prop="name" label="名称" min-width="140" show-overflow-tooltip />
      <el-table-column label="关键属性" min-width="200">
        <template #default="{ row }">
          <span class="text-muted">{{ attributeSummary(row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="相似度" width="180">
        <template #default="{ row }">
          <div class="sim-bar">
            <el-progress
              :percentage="Math.round(row.similarity)"
              :color="progressColor(row.similarity)"
              :stroke-width="10"
              :show-text="false"
              style="flex: 1"
            />
            <span :style="{ color: progressColor(row.similarity), fontWeight: 600 }">
              {{ Math.round(row.similarity) }}%
            </span>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button @click="$emit('use-existing', selected)">
        {{ selected ? `使用已有数据：${selected.code}` : '使用已有数据' }}
      </el-button>
      <el-button type="primary" @click="$emit('continue')">确认是新数据，继续提交</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'

// 相似度弹窗（REQ-FED06）：使用已有 / 确认是新数据继续
defineProps({
  visible: { type: Boolean, default: false },
  items: { type: Array, default: () => [] },
  maxSimilarity: { type: Number, default: 0 }
})

defineEmits(['update:visible', 'continue', 'use-existing'])

const selected = ref(null)

function onSelect(row) {
  selected.value = row
}

function progressColor(value) {
  if (value >= 80) return '#f56c6c'
  if (value >= 60) return '#e6a23c'
  return '#67c23a'
}

function attributeSummary(row) {
  const attributes = row.attributes || {}
  const parts = []
  Object.keys(attributes).forEach((key) => {
    if (key === 'materialName' || key === 'name') return
    if (parts.length >= 3) return
    const value = attributes[key]
    if (value !== null && value !== undefined && value !== '') {
      parts.push(Array.isArray(value) ? value.join('、') : value)
    }
  })
  return parts.length ? parts.join(' / ') : '—'
}
</script>

<style scoped>
.summary {
  margin-bottom: 12px;
}

.summary-left {
  max-width: 480px;
}

.summary-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.summary-tip {
  margin-top: 8px;
  font-size: 13px;
  color: #e6a23c;
}

.summary-tip.danger {
  color: #f56c6c;
}
</style>
