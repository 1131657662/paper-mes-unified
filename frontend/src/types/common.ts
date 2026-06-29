/** 统一响应体，与后端 com.paper.mes.common.R 对应。 */
export interface R<T> {
  code: number
  data: T | null
  message: string
  /** 业务错误码 E001-E006，成功时后端不下发。 */
  errorCode?: string
}

/** 分页结果，与后端 com.paper.mes.common.PageResult 对应。 */
export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

/** 列表查询通用分页入参（后端用 current/size，非 pageNum/pageSize）。 */
export interface PageQuery {
  current?: number
  size?: number
}
