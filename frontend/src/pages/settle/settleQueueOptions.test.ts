import { describe, expect, it } from 'vitest'
import type { SettleCollectionSummary } from '../../types/settle'
import { collectionQueueOptions } from './settleQueueOptions'

describe('settlement collection queue options', () => {
  it('shows operational bucket counts returned by the backend', () => {
    const summary: SettleCollectionSummary = {
      dueTodayCount: 2,
      dueTodayAmount: 300,
      overdueCount: 3,
      overdueAmount: 500,
      upcomingCount: 4,
      upcomingAmount: 700,
      remindedTodayCount: 1,
      remindedTodayAmount: 100,
    }

    expect(collectionQueueOptions(summary).map((item) => item.label)).toEqual([
      '已逾期 3', '今日待收 2', '后续到期 4', '今日已提醒 1',
    ])
  })
})
