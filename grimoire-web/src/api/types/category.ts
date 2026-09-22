/** 分类 VO */
export interface CategoryVo {
  id: string
  parentId: string
  categoryName: string
  categoryCode: string
  sortOrder: number
  status: number
}

/** 分类树 VO（含聚合计数 + 子节点） */
export interface CategoryTreeVo extends CategoryVo {
  directCount: number
  totalCount: number
  children: CategoryTreeVo[]
}
