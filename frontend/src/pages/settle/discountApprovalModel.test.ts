import { describe, expect, it } from 'vitest'
import { approvalMatches, approvalStatusText, isRestorableApproval,
  shouldLockApproval } from './discountApprovalModel'
import type { SettleDiscountApproval } from '../../types/settle'

describe('discountApprovalModel', () => {
  it('matches the complete approved receipt plan', () => {
    expect(approvalMatches(approval(), {
      cashAmount: 500,
      scrapOffsetAmount: 0,
      discountAmount: 1000,
      discountReason: '业务减免',
    }, 1500)).toBe(true)
    expect(approvalMatches(approval(), {
      cashAmount: 499,
      scrapOffsetAmount: 0,
      discountAmount: 1000,
      discountReason: '业务减免',
    }, 1500)).toBe(false)
  })

  it('exposes terminal approval status text', () => {
    expect(approvalStatusText(4)).toBe('已驳回')
    expect(approvalStatusText(6)).toBe('已失效')
  })

  it.each([1, 2])('restores active approval status %s', (status) => {
    expect(isRestorableApproval({ ...approval(), approvalStatus: status })).toBe(true)
  })

  it.each([3, 4, 5, 6])('does not restore terminal approval status %s', (status) => {
    expect(isRestorableApproval({ ...approval(), approvalStatus: status })).toBe(false)
  })

  it('unlocks an active plan when current settings no longer require approval', () => {
    expect(shouldLockApproval(approval(), true, false, true)).toBe(true)
    expect(shouldLockApproval(approval(), true, false, false)).toBe(false)
  })
})

function approval(): SettleDiscountApproval {
  return {
    uuid: 'approval-1', settleUuid: 'settle-1', cashAmount: 500, scrapOffsetAmount: 0,
    discountAmount: 1000, unreceivedSnapshot: 1500, discountPercent: 66.67,
    requiredLevel: 'ADMIN', reason: '业务减免', approvalStatus: 2,
    requestByName: '财务甲', requestTime: '2026-08-07T10:00:00',
  }
}
