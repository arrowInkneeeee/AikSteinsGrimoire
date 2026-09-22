import { get } from './request'
import type { TagVo } from './types/tag'

/** 全部标签（含 useCount） */
export function findAll() {
  return get<TagVo[]>('/tag/findAll')
}
