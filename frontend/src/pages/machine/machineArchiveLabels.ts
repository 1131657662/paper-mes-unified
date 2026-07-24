import type { MachineResourceKind } from '../../types/machine'

export const RESOURCE_KIND_LABEL: Record<MachineResourceKind, string> = {
  MACHINE: '设备',
  WORKSTATION: '工位',
}

export const PROCESS_CATEGORY_LABEL: Record<string, string> = {
  PRODUCTION: '生产',
  SERVICE: '服务',
  QUALITY: '质量',
  PACKAGING: '包装',
  LOGISTICS: '物流',
}

export const MACHINE_STATUS_LABEL: Record<number, string> = {
  1: '启用',
  2: '停用',
}
