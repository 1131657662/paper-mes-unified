import { orvalRequest } from '../orvalRequest'
export interface MachineCapabilityVO {
  catalogUuid: string
  defaultCapability: boolean
  maxDiameter?: number
  maxRollWeight?: number
  maxWidth?: number
  minWidth?: number
  priority: number
  processCategory: string
  processCode: string
  processName: string
  remark?: string
  stepType: number
}

export type MachineVOResourceKind =
  (typeof MachineVOResourceKind)[keyof typeof MachineVOResourceKind]

export const MachineVOResourceKind = {
  MACHINE: 'MACHINE',
  WORKSTATION: 'WORKSTATION',
} as const

export interface MachineVO {
  capabilities?: MachineCapabilityVO[]
  createTime?: string
  machineCode?: string
  machineName: string
  machineType?: number
  remark?: string
  resourceKind?: MachineVOResourceKind
  status?: number
  updateTime?: string
  uuid: string
  version?: number
}

export interface PageResultMachineVO {
  current: number
  records: MachineVO[]
  size: number
  total: number
}

export type ListMachinesParams = {
  keyword?: string
  status?: number
  current?: number
  size?: number
}

type SecondParameter<T extends (...args: never) => unknown> = Parameters<T>[1]

/**
 * @summary List machines
 */
export const listMachines = (
  params?: ListMachinesParams,
  options?: SecondParameter<typeof orvalRequest<PageResultMachineVO>>,
) => {
  return orvalRequest<PageResultMachineVO>(
    { url: `/api/machines`, method: 'GET', params },
    options,
  )
}

/**
 * @summary Get a machine profile
 */
export const getMachine = (
  uuid: string,
  options?: SecondParameter<typeof orvalRequest<MachineVO>>,
) => {
  return orvalRequest<MachineVO>(
    { url: `/api/machines/${uuid}`, method: 'GET' },
    options,
  )
}

export type ListMachinesResult = NonNullable<
  Awaited<ReturnType<typeof listMachines>>
>
export type GetMachineResult = NonNullable<
  Awaited<ReturnType<typeof getMachine>>
>
