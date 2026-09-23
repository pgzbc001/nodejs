import http from './http'

// 首页统计（契约 3.8）
export const fetchOverview = () => http.get('/stats/overview')
