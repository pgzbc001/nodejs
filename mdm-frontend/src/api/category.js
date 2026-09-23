import http from './http'

// 分类管理（契约 3.1）
export const fetchCategoryTree = () => http.get('/categories/tree')

export const searchCategories = (keyword) =>
  http.get('/categories/search', { params: { keyword } })

export const createCategory = (data) => http.post('/categories', data)

export const updateCategory = (id, data) => http.put(`/categories/${id}`, data)

export const deleteCategory = (id) => http.delete(`/categories/${id}`)
