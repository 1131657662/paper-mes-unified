import type { ServiceEditorStatus } from './serviceStepEditorTypes'

export function serviceEditorActionBlockedReason(status?: ServiceEditorStatus): string | undefined {
  return status?.dirty
    ? '当前附加工艺有未保存修改，请先保存或还原后再操作已保存配置'
    : undefined
}
