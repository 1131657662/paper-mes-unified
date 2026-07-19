import type { SettleCollectionSummary, SettleListSummary } from '../../types/settle'

export function documentQueueOptions(summary: SettleListSummary | undefined, activeCount: number | string) {
  return [
    { label: `有效 ${activeCount}`, value: 'all' },
    { label: `待收款 ${summary?.pendingDocumentCount ?? '-'}`, value: 'pending' },
    { label: `部分收款 ${summary?.partialDocumentCount ?? '-'}`, value: 'partial' },
    { label: `已结清 ${summary?.paidDocumentCount ?? '-'}`, value: 'paid' },
    { label: `已作废 ${summary?.voidDocumentCount ?? '-'}`, value: 'void' },
  ]
}

export function collectionQueueOptions(summary?: SettleCollectionSummary) {
  return [
    { label: `已逾期 ${summary?.overdueCount ?? '-'}`, value: 'overdue' },
    { label: `今日待收 ${summary?.dueTodayCount ?? '-'}`, value: 'today' },
    { label: `后续到期 ${summary?.upcomingCount ?? '-'}`, value: 'upcoming' },
    { label: `今日已提醒 ${summary?.remindedTodayCount ?? '-'}`, value: 'reminded' },
  ]
}
