import { get, post } from './request'
import type { PageResult } from './types/api'
import type { KnowledgeVo, KnowledgeListVo, KnowledgeStatsVo, KnowledgeQuery } from './types/knowledge'

/** 分页查询 */
export function findPage(query: KnowledgeQuery) {
  return post<PageResult<KnowledgeListVo>>('/knowledge/page', query)
}

/** 查询详情 */
export function findById(id: string) {
  return get<KnowledgeVo>('/knowledge/findById', { params: { id } })
}

/** 知识库统计 */
export function stats() {
  return get<KnowledgeStatsVo>('/knowledge/stats')
}

/** 全量查询（不分页，返回全部已启用知识条目） */
export function findAll() {
  return get<KnowledgeListVo[]>('/knowledge/findAll')
}
