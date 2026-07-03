import type { SettleCandidateVO } from '../../../types/settle'
import {
  formatKg as formatWeightKg,
  formatMoney as formatCurrency,
  formatTonFromKg,
} from '../../../utils/numberFormatters'

export function formatMoney(value?: number) {
  return formatCurrency(value)
}

export function formatKg(value?: number) {
  return formatWeightKg(value)
}

export function formatTon(value?: number) {
  return formatTonFromKg(value)
}

export function formatPercent(value: number, total: number) {
  if (total <= 0) return '0%'
  return `${Math.round((value / total) * 100)}%`
}

export function settleModeText(settleType?: number, settleDay?: number) {
  if (settleType === 1) return '次结'
  if (settleType === 2) return settleDay ? `月结 ${settleDay}日` : '月结'
  return '-'
}

export function selectedTotals(items: SettleCandidateVO[]) {
  return items.reduce(
    (total, item) => ({
      extra: total.extra + (item.extraAmount ?? 0),
      finishCount: total.finishCount + (item.finishRollCount ?? 0),
      finishWeight: total.finishWeight + (item.finishRollWeight ?? 0),
      orderCount: total.orderCount + 1,
      rewind: total.rewind + (item.rewindAmount ?? 0),
      saw: total.saw + (item.sawAmount ?? 0),
      total: total.total + (item.totalAmount ?? 0),
    }),
    { extra: 0, finishCount: 0, finishWeight: 0, orderCount: 0, rewind: 0, saw: 0, total: 0 },
  )
}
