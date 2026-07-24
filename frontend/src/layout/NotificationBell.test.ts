import { describe, expect, it } from 'vitest'
import type { SystemNotification } from '../types/notification'
import { notificationPath } from './notificationPath'

describe('notificationPath', () => {
  it('routes report alert events to report subscriptions', () => {
    expect(notificationPath(notification('REPORT_ALERT_EVENT')))
      .toBe('/reports/management/subscriptions?eventId=source-1')
  })

  it('keeps backup task deep-link parameters', () => {
    expect(notificationPath(notification('BACKUP_TASK')))
      .toBe('/system-config?section=backup&view=tasks&task=source-1')
  })
})

function notification(sourceType: string): SystemNotification {
  return {
    uuid: 'notification-1',
    notificationType: 'REPORT_ALERT',
    severity: 'WARNING',
    title: 'title',
    content: 'content',
    sourceType,
    sourceUuid: 'source-1',
    read: false,
    createdAt: '2026-07-20T17:24:04',
  }
}
