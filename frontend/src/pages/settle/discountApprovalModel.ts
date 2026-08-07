import type { SettleDiscountApproval } from '../../types/settle'
import { roundMoney, type ReceiveFormValues } from './receiveFormModel'

export const DISCOUNT_APPROVAL_STATUS = {
  pending: 1,
  approved: 2,
  used: 3,
  rejected: 4,
  cancelled: 5,
  stale: 6,
} as const

export function isRestorableApproval(
  approval: SettleDiscountApproval | null | undefined,
): approval is SettleDiscountApproval {
  return approval?.approvalStatus === DISCOUNT_APPROVAL_STATUS.pending
    || approval?.approvalStatus === DISCOUNT_APPROVAL_STATUS.approved
}

export function shouldLockApproval(
  approval: SettleDiscountApproval | null | undefined,
  matches: boolean,
  editing: boolean,
  requiresApproval: boolean,
): boolean {
  return requiresApproval && !editing && matches && isRestorableApproval(approval)
}

export function approvalStatusText(status: number): string {
  return {
    1: '待审批',
    2: '已通过',
    3: '已使用',
    4: '已驳回',
    5: '已取消',
    6: '已失效',
  }[status] ?? '未知状态'
}

export function approvalStatusColor(status: number): string {
  return {
    1: 'processing',
    2: 'success',
    3: 'default',
    4: 'error',
    5: 'default',
    6: 'warning',
  }[status] ?? 'default'
}

export function approvalMatches(approval: SettleDiscountApproval | null | undefined,
  values: ReceiveFormValues, unreceivedAmount: number): boolean {
  if (!approval) return false
  return roundMoney(approval.cashAmount) === roundMoney(values.cashAmount)
    && roundMoney(approval.scrapOffsetAmount) === roundMoney(values.scrapOffsetAmount)
    && roundMoney(approval.discountAmount) === roundMoney(values.discountAmount)
    && roundMoney(approval.unreceivedSnapshot) === roundMoney(unreceivedAmount)
    && approval.reason.trim() === (values.discountReason?.trim() ?? '')
}

export function approvalLevelText(level: 'DIRECT' | 'FINANCE' | 'ADMIN'): string {
  if (level === 'DIRECT') return '免审'
  return level === 'FINANCE' ? '财务复核' : '管理员审批'
}
