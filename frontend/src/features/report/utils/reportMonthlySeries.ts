import dayjs from 'dayjs'
import type { ReportDimensionVO } from '../../../types/report'

interface PeriodRange {
  dateFrom?: string
  dateTo?: string
}

export function fillMonthlySeries(rows: ReportDimensionVO[], period: PeriodRange): ReportDimensionVO[] {
  if (rows.length === 0) return []
  const sorted = [...rows].sort((left, right) => left.dimensionKey.localeCompare(right.dimensionKey))
  const start = monthStart(period.dateFrom)
  const end = monthStart(period.dateTo)
  if (!start || !end || start.isAfter(end, 'month')) return sorted

  const byKey = new Map(sorted.map((row) => [row.dimensionKey, row]))
  const filled: ReportDimensionVO[] = []
  for (let cursor = start; !cursor.isAfter(end, 'month'); cursor = cursor.add(1, 'month')) {
    const key = cursor.format('YYYY-MM')
    filled.push(byKey.get(key) ?? emptyMonth(key))
  }
  return filled
}

function monthStart(value?: string) {
  if (!value) return undefined
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.startOf('month') : undefined
}

function emptyMonth(key: string): ReportDimensionVO {
  return {
    cashReceivedAmount: 0, dimensionKey: key, dimensionName: key, extraAmount: 0,
    finishRollCount: 0, finishWeight: 0, knifeCount: 0, lossRatio: 0, lossWeight: 0,
    orderCount: 0, originalRollCount: 0, originalWeight: 0, pendingSettleAmount: 0,
    processAmount: 0, receivedAmount: 0, rewindAmount: 0, sawAmount: 0,
    scrapOffsetAmount: 0, settledAmount: 0, totalAmount: 0, unreceivedAmount: 0,
  }
}
