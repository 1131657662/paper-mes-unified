import type { ReportAlertEventStatus, ReportAlertSignalCode } from '../types'

export const eventStatusLabels: Record<ReportAlertEventStatus, string> = {
  1: '活动',
  2: '已恢复',
  3: '已忽略',
}

export const eventStatusColors: Record<ReportAlertEventStatus, string> = {
  1: 'error',
  2: 'success',
  3: 'default',
}

export function signalLabel(code: ReportAlertSignalCode): string {
  return code === 'LOSS_RATIO' ? '损耗率' : '已结算未收占比'
}

export function operatorLabel(operator: string): string {
  return { GT: '>', GTE: '≥', LT: '<', LTE: '≤' }[operator] ?? operator
}
