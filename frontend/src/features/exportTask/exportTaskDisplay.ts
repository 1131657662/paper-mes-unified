import type { ExportTask } from '../../types/exportTask'

export type ExportTaskAction = 'download' | 'retry' | 'cancel' | 'acknowledge'

export const exportTaskStatus: Record<number, { color: string; text: string }> = {
  1: { color: 'default', text: '等待执行' },
  2: { color: 'processing', text: '正在导出' },
  3: { color: 'success', text: '可下载' },
  4: { color: 'error', text: '导出失败' },
  5: { color: 'default', text: '已取消' },
  6: { color: 'warning', text: '已过期' },
}

export function exportTaskActions(task: ExportTask): ExportTaskAction[] {
  if (task.taskStatus === 1) return task.resourceAccessible ? ['cancel'] : []
  if (task.taskStatus === 3) return task.resourceAccessible ? ['download'] : []
  if (task.taskStatus === 4 || task.taskStatus === 6) {
    const retryable = task.resourceAccessible && !exportTaskRetryLimitReached(task)
    const actions: ExportTaskAction[] = retryable ? ['retry'] : []
    if (!task.acknowledged) actions.push('acknowledge')
    return actions
  }
  return []
}

export function exportTaskRetryLimitReached(task: ExportTask): boolean {
  if (task.attemptCount === undefined || task.maxAttempts === undefined) return false
  return task.attemptCount >= task.maxAttempts
}

export function exportTaskOperationLabel(operationCode?: string): string {
  if (operationCode === 'detail-export') return '详情导出'
  if (operationCode === 'inventory-export') return '库存导出'
  if (operationCode === 'reconciliation-export') return '对账导出'
  if (operationCode === 'full-export') return '报表导出'
  if (operationCode === 'scheduled-export') return '定时报表'
  return operationCode || '未知操作'
}

export function exportTaskModuleLabel(moduleCode?: string): string {
  if (moduleCode === 'settle') return '结算'
  if (moduleCode === 'delivery') return '出库'
  if (moduleCode === 'inventory') return '库存'
  if (moduleCode === 'report') return '报表'
  if (moduleCode === 'process-order') return '加工单'
  return '系统'
}
