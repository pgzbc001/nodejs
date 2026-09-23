<template>
  <el-dialog
    :model-value="visible"
    title="导入 / 导出"
    width="640px"
    append-to-body
    @update:model-value="(v) => $emit('update:visible', v)"
  >
    <div class="action-row">
      <el-button type="primary" plain @click="onDownloadTemplate">下载导入模板</el-button>
      <el-button type="success" plain @click="onExport">导出数据</el-button>
      <span class="text-muted">导出/模板格式均为 xlsx（表头为字段显示名）</span>
    </div>

    <el-divider />

    <el-upload
      drag
      :show-file-list="false"
      :before-upload="beforeUpload"
      :http-request="customUpload"
      accept=".xlsx,.xls"
    >
      <el-icon class="upload-icon"><UploadFilled /></el-icon>
      <div>将 Excel 文件拖到此处，或点击上传</div>
      <template #tip>
        <div class="text-muted">仅支持 xlsx/xls；逐行校验，失败行将给出明细</div>
      </template>
    </el-upload>
    <div v-if="uploading" class="uploading">
      <el-icon class="is-loading"><Loading /></el-icon>
      正在导入，请稍候…
    </div>

    <!-- 导入结果 -->
    <div v-if="result" class="result">
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="总行数">{{ result.total }}</el-descriptions-item>
        <el-descriptions-item label="成功">
          <span style="color: #67c23a">{{ result.successCount }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="失败">
          <span :style="{ color: result.failCount ? '#f56c6c' : '#909399' }">
            {{ result.failCount }}
          </span>
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="result.errors && result.errors.length" class="errors">
        <div class="errors-header">
          <span>失败行明细（{{ result.errors.length }} 条）</span>
          <el-button size="small" type="primary" plain @click="downloadErrors">
            下载错误明细
          </el-button>
        </div>
        <el-table :data="result.errors" size="small" border max-height="220">
          <el-table-column prop="row" label="行号" width="70" />
          <el-table-column prop="field" label="字段" width="130" show-overflow-tooltip />
          <el-table-column prop="message" label="错误说明" min-width="200" show-overflow-tooltip />
        </el-table>
      </div>
    </div>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Loading } from '@element-plus/icons-vue'
import { downloadTemplate, exportData, exportImportErrors, importData } from '../api/io'

// 导入导出对话框（REQ-BKD07 / REQ-FED07）
const props = defineProps({
  visible: { type: Boolean, default: false },
  modelId: { type: [Number, String], default: null }
})

const emit = defineEmits(['update:visible', 'success'])

const uploading = ref(false)
const result = ref(null)

watch(
  () => props.visible,
  (visible) => {
    if (visible) result.value = null
  }
)

async function onDownloadTemplate() {
  await downloadTemplate(props.modelId)
}

async function onExport() {
  await exportData(props.modelId)
}

function beforeUpload(file) {
  const valid = /\.(xlsx|xls)$/i.test(file.name)
  if (!valid) {
    ElMessage.warning('仅支持 xlsx/xls 文件')
  }
  return valid
}

async function customUpload({ file }) {
  uploading.value = true
  try {
    result.value = await importData(props.modelId, file)
    if (result.value?.failCount > 0) {
      ElMessage.warning(`导入完成：成功 ${result.value.successCount} 行，失败 ${result.value.failCount} 行`)
    } else {
      ElMessage.success(`导入完成：共 ${result.value?.total || 0} 行全部成功`)
    }
    emit('success')
  } finally {
    uploading.value = false
  }
}

async function downloadErrors() {
  if (!result.value?.errors?.length) return
  await exportImportErrors(props.modelId, result.value.errors)
}
</script>

<style scoped>
.action-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.upload-icon {
  font-size: 40px;
  color: #c0c4cc;
  margin-bottom: 8px;
}

.uploading {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  color: #409eff;
}

.result {
  margin-top: 14px;
}

.errors {
  margin-top: 10px;
}

.errors-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  font-weight: 600;
}
</style>
