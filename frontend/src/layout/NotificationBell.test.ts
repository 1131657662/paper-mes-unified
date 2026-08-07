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

  it('routes a pending discount request to the approval inbox', () => {
    expect(notificationPath(notification('SETTLE_DISCOUNT_APPROVAL', 'SETTLE_DISCOUNT_REQUESTED')))
      .toBe('/settle-orders/discount-approvals?scope=pending&approval=source-1')
  })

  it('routes a discount decision to the requester history', () => {
    expect(notificationPath(notification('SETTLE_DISCOUNT_APPROVAL', 'SETTLE_DISCOUNT_APPROVED')))
      .toBe('/settle-orders/discount-approvals?scope=mine&approval=source-1')
  })
})

function notification(sourceType: string, notificationType = 'REPORT_ALERT'): SystemNotification {
  return {
    uuid: 'notification-1',
    notificationType,
    severity: 'WARNING',
    title: 'title',
    content: 'content',
    sourceType,
    sourceUuid: 'source-1',
    read: false,
    createdAt: '2026-07-20T17:24:04',
  }
}
