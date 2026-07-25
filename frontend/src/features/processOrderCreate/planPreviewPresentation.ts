import type { PlanPreviewVO } from '../../types/processOrder'

export interface PlanPreviewSaveStatus {
  color: 'error' | 'success' | 'warning'
  label: string
}

export function planPreviewSaveStatus(
  preview: PlanPreviewVO | undefined,
  configured: boolean,
): PlanPreviewSaveStatus | undefined {
  if (!preview) return undefined
  if (!preview.ready) return { color: 'error', label: '需修正' }
  if (!configured) return { color: 'warning', label: '预览通过，待保存' }
  return { color: 'success', label: '已保存，可进入预览确认' }
}
