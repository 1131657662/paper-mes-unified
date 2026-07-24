import type { PlanPreviewVO } from '../../types/processOrder'
import type { MergedSourceLock } from './rewindConsumptionUtils'
import type { RollDraft } from './types'

export type RollPreviewStatusKind = 'ready' | 'blocked' | 'direct' | 'merged' | 'pending'

export interface RollPreviewStatus {
  kind: RollPreviewStatusKind
  color: string
  label: string
  detail: string
  blocking: boolean
}

export function rollPreviewStatus(options: RollPreviewStatusOptions): RollPreviewStatus {
  const { roll, preview, lock, serviceConfigured } = options
  if (lock) {
    return {
      kind: 'merged',
      color: 'purple',
      label: '已合并使用',
      detail: `由 ${lock.ownerLabel} 合并消耗 ${lock.consumeRatio}%`,
      blocking: false,
    }
  }
  if (roll.processMode === 3) {
    return {
      kind: 'direct',
      color: 'default',
      label: '直发无需配置',
      detail: '直发卷不生成加工成品号，回录阶段沿用母卷信息',
      blocking: false,
    }
  }
  if (roll.processMode === 4) {
    const configured = serviceConfigured ?? preview?.ready === true
    if (configured) {
      return {
        kind: 'ready',
        color: 'success',
        label: '附加工艺已就绪',
        detail: preview?.summary || '已配置剥损整理或重新包装，提交时按服务工艺生成整理成品',
        blocking: false,
      }
    }
    const hasBackendResult = serviceConfigured === undefined && Boolean(preview)
    return {
      kind: hasBackendResult ? 'blocked' : 'pending',
      color: hasBackendResult ? 'error' : 'warning',
      label: hasBackendResult ? '附加工艺未通过' : '待配置附加工艺',
      detail: preview?.errors?.join('；') || '请在工艺配置中选择剥损整理或重新包装',
      blocking: true,
    }
  }
  if (!preview) {
    return {
      kind: 'pending',
      color: 'warning',
      label: '待预览',
      detail: '尚未取得后端预览，请返回工艺配置保存或刷新预览',
      blocking: true,
    }
  }
  if (!preview.ready) {
    return {
      kind: 'blocked',
      color: 'error',
      label: '需修正',
      detail: preview.errors?.join('；') || '后端预览未通过，请返回工艺配置检查参数',
      blocking: true,
    }
  }
  return {
    kind: 'ready',
    color: 'success',
    label: readyLabel(roll),
    detail: preview.summary || '后端预览已通过',
    blocking: false,
  }
}

function readyLabel(roll: RollDraft): string {
  if (roll.processMode === 4) return '附加工艺已就绪'
  if (roll.processMode === 2) return '现场定尺已就绪'
  if (roll.mainStepType === 1) return '锯纸已就绪'
  return '复卷已就绪'
}

interface RollPreviewStatusOptions {
  roll: RollDraft
  preview?: PlanPreviewVO
  lock?: MergedSourceLock
  serviceConfigured?: boolean
}
