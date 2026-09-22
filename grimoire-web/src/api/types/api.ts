/** 统一响应体 */
export interface ApiResponse<T = unknown> {
  code: number
  success: boolean
  msg: string
  data: T
}

/** 分页结果（分页元数据均为 string，Long 精度保护） */
export interface PageResult<T> {
  records: T[]
  total: string
  size: string
  current: string
  pages: string
}
