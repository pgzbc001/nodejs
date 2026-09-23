import http from './http'

// 推送与协同（契约 3.6）
export const fetchSystems = () => http.get('/push/systems')

export const precheckPush = (dataIds, systemIds) =>
  http.post('/push/precheck', { dataIds, systemIds })

export const executePush = (payload) => http.post('/push/execute', payload)

export const fetchPushLogs = (params) => http.get('/push/logs', { params })

export const fetchCollaborations = (params) => http.get('/collaborations', { params })

export const confirmCollaboration = (id, comment) =>
  http.post(`/collaborations/${id}/confirm`, { comment })

export const rejectCollaboration = (id, comment) =>
  http.post(`/collaborations/${id}/reject`, { comment })
