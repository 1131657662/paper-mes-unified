import type { PageQuery } from './common'

/** 纸张档案，与后端 Paper 对应（含 BaseEntity 通用字段，按需取用）。 */
export interface Paper {
  uuid: string
  paperCode?: string
  paperName: string
  /** 常用克重 g/㎡ */
  gramWeight?: number
  paperType?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

/** 纸张新增/修改入参，与后端 PaperSaveDTO 对应。 */
export interface PaperSaveDTO {
  paperCode?: string
  paperName: string
  gramWeight?: number
  paperType?: string
  remark?: string
}

/** 纸张列表查询入参。 */
export interface PaperQuery extends PageQuery {
  keyword?: string
}
