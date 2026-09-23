<template>
  <div class="category-panel">
    <div class="panel-toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索分类名称/编码"
        clearable
        size="small"
        @keyup.enter="load"
        @clear="load"
      >
        <template #append>
          <el-button @click="load">搜索</el-button>
        </template>
      </el-input>
    </div>
    <div class="panel-actions">
      <el-button size="small" type="primary" @click="openCreate(null, '')">新增根分类</el-button>
      <el-button size="small" @click="toggleExpand">
        {{ expandAll ? '折叠全部' : '展开全部' }}
      </el-button>
    </div>

    <el-tree
      :key="treeKey"
      :data="treeData"
      node-key="id"
      :props="{ label: 'name', children: 'children' }"
      :default-expand-all="expandAll"
      highlight-current
      :expand-on-click-node="false"
      @node-click="onNodeClick"
    >
      <template #default="{ data }">
        <div class="tree-node">
          <span class="node-label">{{ data.name }}</span>
          <el-tag v-if="data.modelCount > 0" size="small" type="info">{{ data.modelCount }}</el-tag>
          <el-dropdown
            trigger="click"
            size="small"
            @command="(cmd) => onCommand(cmd, data)"
            @click.stop
          >
            <el-icon class="more-icon" @click.stop><MoreFilled /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="add-sibling">新增同级</el-dropdown-item>
                <el-dropdown-item command="add-child">新增子级</el-dropdown-item>
                <el-dropdown-item command="edit">编辑</el-dropdown-item>
                <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </template>
    </el-tree>
    <el-empty v-if="!treeData.length" description="暂无分类，请新增" :image-size="60" />

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑分类' : '新增分类'"
      width="440px"
      append-to-body
    >
      <el-form :model="form" label-width="90px">
        <el-form-item label="上级分类">
          <span class="text-muted">{{ parentName || '（根级）' }}</span>
        </el-form-item>
        <el-form-item label="编码" required>
          <el-input v-model="form.code" placeholder="如 CAT0101" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="如 原料" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number
            v-model="form.sortNo"
            :min="0"
            :controls="false"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
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
import { MoreFilled } from '@element-plus/icons-vue'
import {
  createCategory,
  deleteCategory,
  fetchCategoryTree,
  searchCategories,
  updateCategory
} from '../api/category'

const emit = defineEmits(['select', 'refresh'])

const keyword = ref('')
const treeData = ref([])
const expandAll = ref(true)
const treeKey = ref(0)

const dialogVisible = ref(false)
const saving = ref(false)
const parentName = ref('')
const form = reactive({
  id: null,
  parentId: null,
  code: '',
  name: '',
  sortNo: 0,
  description: ''
})

async function load() {
  // 重建树以应用展开状态与搜索结果
  treeKey.value += 1
  const key = keyword.value.trim()
  treeData.value = key ? await searchCategories(key) : await fetchCategoryTree()
}

function toggleExpand() {
  expandAll.value = !expandAll.value
  treeKey.value += 1
}

function onNodeClick(data) {
  emit('select', data)
}

function onCommand(command, data) {
  if (command === 'add-sibling') {
    openCreate(data.parentId ?? null, findName(treeData.value, data.parentId))
  } else if (command === 'add-child') {
    openCreate(data.id, data.name)
  } else if (command === 'edit') {
    openEdit(data)
  } else if (command === 'delete') {
    remove(data)
  }
}

function openCreate(parentId, parentLabel) {
  Object.assign(form, {
    id: null,
    parentId: parentId ?? null,
    code: '',
    name: '',
    sortNo: 0,
    description: ''
  })
  parentName.value = parentLabel
  dialogVisible.value = true
}

function openEdit(data) {
  Object.assign(form, {
    id: data.id,
    parentId: data.parentId ?? null,
    code: data.code,
    name: data.name,
    sortNo: data.sortNo ?? 0,
    description: data.description || ''
  })
  parentName.value = findName(treeData.value, data.parentId)
  dialogVisible.value = true
}

async function save() {
  if (!form.code.trim() || !form.name.trim()) {
    ElMessage.warning('编码与名称为必填项')
    return
  }
  saving.value = true
  try {
    const payload = {
      code: form.code.trim(),
      name: form.name.trim(),
      parentId: form.parentId,
      sortNo: form.sortNo,
      description: form.description
    }
    if (form.id) {
      await updateCategory(form.id, payload)
      ElMessage.success('分类已更新')
    } else {
      await createCategory(payload)
      ElMessage.success('分类已创建')
    }
    dialogVisible.value = false
    await load()
    emit('refresh')
  } catch (e) {
    // 错误提示由 http 拦截器处理
  } finally {
    saving.value = false
  }
}

async function remove(data) {
  try {
    await ElMessageBox.confirm(
      `确认删除分类「${data.name}」？存在子分类或关联模型时不允许删除。`,
      '删除确认',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    await deleteCategory(data.id)
    ElMessage.success('分类已删除')
    await load()
    emit('refresh')
  } catch (e) {
    // 40901 等错误由 http 拦截器提示
  }
}

function findName(nodes, id) {
  if (!id) return ''
  for (const node of nodes || []) {
    if (node.id === id) return node.name
    const found = findName(node.children, id)
    if (found) return found
  }
  return ''
}

onMounted(load)

defineExpose({ refresh: load })
</script>

<style scoped>
.category-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
}

.panel-toolbar :deep(.el-input-group__append) {
  padding: 0 10px;
}

.panel-actions {
  display: flex;
  justify-content: space-between;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  padding-right: 8px;
}

.node-label {
  flex-shrink: 0;
}

.node-actions,
.more-icon {
  visibility: hidden;
}

.tree-node:hover .more-icon {
  visibility: visible;
  cursor: pointer;
  color: #909399;
}
</style>
