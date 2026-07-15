import type { FinishedProductRow } from './finishedProductRows'

export interface CustomerFinishedProductRow {
  count: number
  gramWeight?: number
  isTrim: boolean
  key: string
  paperName?: string
  weight: number
  width?: number
}

export interface CustomerFinishedProductTotals {
  count: number
  weight: number
}

export function buildCustomerFinishedProductRows(
  rows: FinishedProductRow[],
): CustomerFinishedProductRow[] {
  const groups = new Map<string, CustomerFinishedProductRow>()
  for (const row of rows.filter(isCustomerFinishedProductVisible)) {
    const key = customerGroupKey(row)
    const current = groups.get(key)
    if (current) {
      current.count += 1
      current.weight += customerFinishedProductWeight(row)
      continue
    }
    groups.set(key, toCustomerRow(row, key))
  }
  return Array.from(groups.values())
}

export function calculateCustomerFinishedProductTotals(
  rows: CustomerFinishedProductRow[],
): CustomerFinishedProductTotals {
  return rows.reduce(
    (totals, row) => ({ count: totals.count + row.count, weight: totals.weight + row.weight }),
    { count: 0, weight: 0 },
  )
}

export function isCustomerFinishedProductVisible({ finish }: FinishedProductRow) {
  return finish.rollNoStatus !== 3 && finish.isSpare !== 1
}

function customerGroupKey(row: FinishedProductRow) {
  const { finish } = row
  return [
    finish.paperName,
    finish.gramWeight,
    finish.finishWidth,
    finish.isRemain === 1 ? 'trim' : 'product',
  ].join('::')
}

function toCustomerRow(row: FinishedProductRow, key: string): CustomerFinishedProductRow {
  const { finish } = row
  return {
    count: 1,
    gramWeight: finish.gramWeight,
    isTrim: finish.isRemain === 1,
    key,
    paperName: finish.paperName,
    weight: customerFinishedProductWeight(row),
    width: finish.finishWidth,
  }
}

export function customerFinishedProductWeight({ finish }: FinishedProductRow) {
  return finish.actualWeight ?? finish.estimateWeight ?? 0
}
