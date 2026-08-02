import dayjs from 'dayjs'
import { describe, expect, it } from 'vitest'
import { resolveSettleCollectionDisplay } from './settleCollectionStatus'

const today = dayjs('2026-08-01')

describe('settlement collection display', () => {
  it.each([
    [3, 0, '已结清', false],
    [4, 100, '已作废', false],
  ])('does not mark terminal status as overdue', (settleStatus, unreceivedAmount, text, overdue) => {
    const result = resolveSettleCollectionDisplay({ settleStatus, unreceivedAmount, dueDate: '2026-07-01' }, today)

    expect(result.text).toBe(text)
    expect(result.overdue).toBe(overdue)
  })

  it('shows overdue text only for active amount', () => {
    const result = resolveSettleCollectionDisplay({ settleStatus: 1, unreceivedAmount: 10, dueDate: '2026-07-31' }, today)

    expect(result.text).toContain('逾期 1 天')
    expect(result.active).toBe(true)
  })

  it('allows reminder without a due date but does not show overdue', () => {
    const result = resolveSettleCollectionDisplay({ settleStatus: 2, unreceivedAmount: 10 }, today)

    expect(result.text).toBe('未设置到期日')
    expect(result.overdue).toBe(false)
    expect(result.active).toBe(true)
  })
})
