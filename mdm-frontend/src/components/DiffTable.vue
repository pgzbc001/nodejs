<template>
  <el-table :data="items" border size="small" :row-class-name="rowClass">
    <el-table-column prop="field" label="字段" width="200" show-overflow-tooltip />
    <el-table-column prop="label" label="说明" width="180" show-overflow-tooltip v-if="hasLabel" />
    <el-table-column label="旧值" min-width="200">
      <template #default="{ row }">
        <span class="old-value">{{ display(row.oldValue) }}</span>
      </template>
    </el-table-column>
    <el-table-column label="新值" min-width="200">
      <template #default="{ row }">
        <span class="new-value">{{ display(row.newValue) }}</span>
      </template>
    </el-table-column>
    <template #empty>
      <el-empty description="两版本无差异" :image-size="60" />
    </template>
  </el-table>
</template>

<script setup>
import { computed } from 'vue'

// 版本差异表格（REQ-FED06）：字段/旧值/新值，差异行高亮
const props = defineProps({
  items: { type: Array, default: () => [] }
})

const hasLabel = computed(() => props.items.some((item) => item.label))

function display(value) {
  if (value === null || value === undefined || value === '') return '（空）'
  if (Array.isArray(value)) return value.join('、')
  return String(value)
}

function rowClass({ row }) {
  return row.oldValue !== row.newValue ? 'diff-row' : ''
}
</script>

<style scoped>
.old-value {
  color: #909399;
  text-decoration: line-through;
}

.new-value {
  color: #67c23a;
  font-weight: 600;
}

:deep(.diff-row) {
  background: #fff8e6;
}
</style>
