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
  const { configured, roll, preview, lock, serviceConfigured } = options
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
        label: '已保存，可预览',
        detail: preview?.summary || '已配置剥损整理或重新包装，提交时按服务工艺生成整理成品',
        blocking: false,
      }
    }
    const hasBackendResult = serviceConfigured === undefined && Boolean(preview)
    return {
      kind: hasBackendResult ? 'blocked' : 'pending',
      color: hasBackendResult ? 'error' : 'warning',
      label: hasBackendResult ? '附加工艺未通过' : '待保存附加工艺',
      detail: preview?.errors?.join('；') || '请在工艺配置中选择剥损整理或重新包装',
      blocking: true,
    }
  }
  if (!preview) {
    return {
      kind: 'pending',
      color: 'warning',
      label: configured ? '已保存，待刷新' : '待保存加工方案',
      detail: configured ? '方案已保存，尚未取得后端预览' : '加工方案尚未保存',
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
  if (configured === false) {
    return {
      kind: 'pending',
      color: 'warning',
      label: '已验证，未保存',
      detail: '当前方案预览已通过，但尚未保存到加工单',
      blocking: true,
    }
  }
  return {
    kind: 'ready',
    color: 'success',
    label: '已保存，可预览',
    detail: preview.summary || '后端预览已通过',
    blocking: false,
  }
}

interface RollPreviewStatusOptions {
  configured?: boolean
  roll: RollDraft
  preview?: PlanPreviewVO
  lock?: MergedSourceLock
  serviceConfigured?: boolean
}
