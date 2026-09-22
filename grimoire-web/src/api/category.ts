import { get } from './request'
import type { CategoryTreeVo } from './types/category'

/** 分类树 */
export function findTree() {
  return get<CategoryTreeVo[]>('/category/findTree')
}
