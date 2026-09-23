import http from './http'

// 操作日志（契约 3.8）
export const fetchLogs = (params) => http.get('/logs', { params })
