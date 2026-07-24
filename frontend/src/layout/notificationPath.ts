import type { SystemNotification } from '../types/notification'

export function notificationPath(item: SystemNotification): string | undefined {
  if (!item.sourceUuid) return undefined
  if (item.sourceType === 'REPORT_ALERT_EVENT') {
    const params = new URLSearchParams({ eventId: item.sourceUuid })
    return `/reports/management/subscriptions?${params.toString()}`
  }
  if (item.sourceType === 'DATA_HEALTH') return '/system-config?section=health'
  if (item.sourceType === 'BACKUP_TASK') {
    const params = new URLSearchParams({ section: 'backup', view: 'tasks', task: item.sourceUuid })
    return `/system-config?${params.toString()}`
  }
  return undefined
}
