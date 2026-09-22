/** 标签简要信息 */
export interface TagBriefVo {
  id: string
  tagName: string
  tagColor: string
}

/** 附件 VO（8 字段，fileSize/fileType 来自文件层） */
export interface AttachmentVo {
  id: string
  fileId: string
  attachName?: string
  fileSize?: string
  fileType?: string
  description?: string
  sortOrder?: number
  createTime?: string
}

/** 知识条目详情 VO（扁平结构） */
export interface KnowledgeVo {
  id: string
  title: string
  code?: string
  type: number
  typeDesc: string
  summary?: string
  content?: string
  sourceProject?: string
  sourcePath?: string
  resourcePath?: string
  extJson?: string
  categoryId?: string
  categoryName?: string
  categoryPath?: string
  status: number
  createTime: string
  modifyTime?: string
  tags: TagBriefVo[]
  attachments: AttachmentVo[]
}

/** 知识列表 VO（列表页精简） */
export interface KnowledgeListVo {
  id: string
  title: string
  code?: string
  type: number
  typeDesc: string
  summary?: string
  categoryId?: string
  categoryName?: string
  categoryPath?: string
  status: number
  createTime: string
  tags: TagBriefVo[]
}

/** 知识库统计 */
export interface KnowledgeStatsVo {
  knowledgeCount: number
  categoryCount: number
  tagCount: number
  attachmentCount: number
  typeDistribution: TypeCountVo[]
  recentEdited: RecentItemVo[]
}

/** 类型分布统计项 */
export interface TypeCountVo {
  type: number
  typeDesc: string
  count: number
}

/** 最近编辑条目 */
export interface RecentItemVo {
  id: string
  title: string
  modifyTime: string
}

/** 知识查询参数 */
export interface KnowledgeQuery {
  current: number
  size: number
  title?: string
  keyword?: string
  type?: number
  categoryId?: string
  includeChildren?: boolean
  tagId?: string
  status?: number | null
}
