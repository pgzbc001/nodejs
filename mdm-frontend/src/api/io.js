import http, { download } from './http'

// 导入导出（契约 3.5）
export const downloadTemplate = (modelId) =>
  download(
    http.get(`/data/${modelId}/template`, { responseType: 'blob' }),
    `import-template-${modelId}.xlsx`
  )

export const exportData = (modelId, params = {}) =>
  download(
    http.get(`/data/${modelId}/export`, { params, responseType: 'blob' }),
    `export-${modelId}.xlsx`
  )

export const importData = (modelId, file) => {
  const form = new FormData()
  form.append('file', file)
  return http.post(`/data/${modelId}/import`, form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const exportImportErrors = (modelId, errors) =>
  download(
    http.post(`/data/${modelId}/import/errors`, errors, { responseType: 'blob' }),
    `import-errors-${modelId}.xlsx`
  )
