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
  const amountsVisible = rows.some((row) => row.totalAmount != null || row.processAmount != null)
  const filled: ReportDimensionVO[] = []
  for (let cursor = start; !cursor.isAfter(end, 'month'); cursor = cursor.add(1, 'month')) {
    const key = cursor.format('YYYY-MM')
    filled.push(byKey.get(key) ?? emptyMonth(key, amountsVisible))
  }
  return filled
}

function monthStart(value?: string) {
  if (!value) return undefined
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.startOf('month') : undefined
}

function emptyMonth(key: string, amountsVisible: boolean): ReportDimensionVO {
  const amount = amountsVisible ? 0 : undefined
  return {
    cashReceivedAmount: amount, dimensionKey: key, dimensionName: key, extraAmount: amount,
    finishRollCount: 0, finishWeight: 0, knifeCount: 0, lossRatio: 0, lossWeight: 0,
    orderCount: 0, originalRollCount: 0, originalWeight: 0, pendingSettleAmount: amount,
    processAmount: amount, receivedAmount: amount, rewindAmount: amount, sawAmount: amount,
    scrapOffsetAmount: amount, settledAmount: amount, totalAmount: amount, unreceivedAmount: amount,
  }
}
