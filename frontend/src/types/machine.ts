import type { PageQuery } from './common'

/** 机台档案，与后端 Machine 对应（含 BaseEntity 通用字段，按需取用）。 */
export interface Machine {
  uuid: string
  machineCode?: string
  machineName: string
  /** 机台类型 1锯纸 2复卷 3通用 */
  machineType?: number
  /** 1启用 2停用 */
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

/** 机台新增/修改入参，与后端 MachineSaveDTO 对应。 */
export interface MachineSaveDTO {
  machineCode?: string
  machineName: string
  machineType?: number
  status?: number
  remark?: string
}

/** 机台列表查询入参。 */
export interface MachineQuery extends PageQuery {
  keyword?: string
}
