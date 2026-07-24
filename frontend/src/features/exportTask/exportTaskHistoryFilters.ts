import { exportTaskModuleLabel, exportTaskStatus } from './exportTaskDisplay'

export const exportTaskStatusOptions = Object.entries(exportTaskStatus).map(([value, status]) => ({
  value: Number(value),
  label: status.text,
}))

export const exportTaskModuleOptions = ['settle', 'delivery', 'inventory', 'report', 'process-order']
  .map((value) => ({ value, label: exportTaskModuleLabel(value) }))

export const exportTaskOperationOptions = [
  ['detail-export', '详情导出'],
  ['inventory-export', '库存导出'],
  ['reconciliation-export', '对账导出'],
  ['full-export', '报表导出'],
  ['scheduled-export', '定时报表'],
].map(([value, label]) => ({ value, label }))
