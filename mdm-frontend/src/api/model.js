import http from './http'

// 模型管理（契约 3.2）
export const fetchModels = (params) => http.get('/models', { params })

export const fetchModelDetail = (id) => http.get(`/models/${id}`)

export const createModel = (data) => http.post('/models', data)

export const updateModel = (id, data) => http.put(`/models/${id}`, data)

export const onlineModel = (id) => http.post(`/models/${id}/online`)

export const offlineModel = (id) => http.post(`/models/${id}/offline`)

export const deleteModel = (id) => http.delete(`/models/${id}`)

export const fetchModelVersions = (id) => http.get(`/models/${id}/versions`)

export const diffModelVersions = (id, fromVersion, toVersion) =>
  http.get(`/models/${id}/versions/${fromVersion}/diff/${toVersion}`)

export const rollbackModel = (id, versionNo) =>
  http.post(`/models/${id}/versions/${versionNo}/rollback`)

/** 全量模型（下拉/树用）：分页拉大页 */
export const fetchAllModels = (params = {}) =>
  http.get('/models', { params: { page: 1, size: 200, ...params } })
