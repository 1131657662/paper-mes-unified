import type { PageQuery } from './common'

/** 仓库档案，与后端 Warehouse 对应（含 BaseEntity 通用字段，按需取用）。 */
export interface Warehouse {
  uuid: string
  warehouseCode?: string
  warehouseName: string
  /** 库位/地址 */
  location?: string
  /** 1启用 2停用 */
  status?: number
  isDefault?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

/** 仓库新增/修改入参，与后端 WarehouseSaveDTO 对应。 */
export interface WarehouseSaveDTO {
  warehouseCode?: string
  warehouseName: string
  location?: string
  status?: number
  isDefault?: number
  remark?: string
}

/** 仓库列表查询入参。 */
export interface WarehouseQuery extends PageQuery {
  keyword?: string
  status?: number
}
