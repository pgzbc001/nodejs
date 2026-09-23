import http from './http'

// 质量规则（契约 3.7）
export const fetchQualityRules = (modelId = null) =>
  http.get('/quality/rules', { params: modelId ? { modelId } : {} })

export const createQualityRule = (data) => http.post('/quality/rules', data)

export const updateQualityRule = (id, data) => http.put(`/quality/rules/${id}`, data)

export const deleteQualityRule = (id) => http.delete(`/quality/rules/${id}`)

export const fetchQualityResults = (dataId) =>
  http.get('/quality/results', { params: { dataId } })

export const ignoreQualityResult = (id, reason) =>
  http.post(`/quality/results/${id}/ignore`, { reason })
