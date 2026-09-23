import http from './http'

// 主数据 + 版本管理（契约 3.3 / 3.4）
export const fetchDataView = (modelId) => http.get(`/data/${modelId}/view`)

export const fetchDataPage = (modelId, params) => http.get(`/data/${modelId}`, { params })

export const fetchDataDetail = (modelId, id) => http.get(`/data/${modelId}/${id}`)

export const checkDuplicate = (modelId, attributes, excludeDataId = null) =>
  http.post(`/data/${modelId}/check-duplicate`, { attributes, excludeDataId })

export const checkQuality = (modelId, attributes) =>
  http.post(`/data/${modelId}/check-quality`, { attributes })

export const createData = (modelId, payload) => http.post(`/data/${modelId}`, payload)

export const updateData = (modelId, id, payload) => http.put(`/data/${modelId}/${id}`, payload)

export const disableData = (modelId, id, force = false) =>
  http.post(`/data/${modelId}/${id}/disable`, null, { params: { force } })

export const enableData = (modelId, id) => http.post(`/data/${modelId}/${id}/enable`)

export const deleteData = (modelId, id, force = false) =>
  http.delete(`/data/${modelId}/${id}`, { params: { force } })

export const fetchDataVersions = (modelId, id) => http.get(`/data/${modelId}/${id}/versions`)

export const fetchDataVersionDetail = (modelId, id, versionNo) =>
  http.get(`/data/${modelId}/${id}/versions/${versionNo}`)

export const diffDataVersions = (modelId, id, fromVersion, toVersion) =>
  http.get(`/data/${modelId}/${id}/versions/${fromVersion}/diff/${toVersion}`)

export const rollbackData = (modelId, id, versionNo) =>
  http.post(`/data/${modelId}/${id}/versions/${versionNo}/rollback`)
