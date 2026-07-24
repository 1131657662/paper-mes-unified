import type { PageQuery } from './common'

export type MachineResourceKind = 'MACHINE' | 'WORKSTATION'

export interface MachineCapability {
  catalogUuid: string
  stepType: number
  processCode: string
  processName: string
  processCategory: string
  defaultCapability: boolean
  priority: number
  minWidth?: number
  maxWidth?: number
  maxRollWeight?: number
  maxDiameter?: number
  remark?: string
}

export interface MachineCapabilitySaveDTO {
  catalogUuid: string
  isDefault?: number
  priority?: number
  minWidth?: number
  maxWidth?: number
  maxRollWeight?: number
  maxDiameter?: number
  remark?: string
}

/** 机台档案，与后端 Machine 对应（含 BaseEntity 通用字段，按需取用）。 */
export interface Machine {
  uuid: string
  machineCode?: string
  machineName: string
  /** 机台类型 1锯纸 2复卷 3通用 */
  machineType?: number
  resourceKind?: MachineResourceKind
  /** 1启用 2停用 */
  status?: number
  capabilities?: MachineCapability[]
  remark?: string
  createTime?: string
  updateTime?: string
}

/** 机台新增/修改入参，与后端 MachineSaveDTO 对应。 */
export interface MachineSaveDTO {
  machineCode?: string
  machineName: string
  machineType?: number
  resourceKind?: MachineResourceKind
  status?: number
  capabilities?: MachineCapabilitySaveDTO[]
  remark?: string
}

/** 机台列表查询入参。 */
export interface MachineQuery extends PageQuery {
  keyword?: string
  status?: number
}
