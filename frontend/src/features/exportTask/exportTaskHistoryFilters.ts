import { exportTaskModuleLabel, exportTaskStatus } from './exportTaskDisplay'

export const exportTaskStatusOptions = Object.entries(exportTaskStatus).map(([value, status]) => ({
  value: Number(value),
  label: status.text,
}))

export const exportTaskModuleOptions = ['settle', 'delivery', 'inventory', 'report', 'process-order']
  .map((value) => ({ value, label: exportTaskModuleLabel(value) }))
