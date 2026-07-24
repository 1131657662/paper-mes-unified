import type { ProcessOrder } from '../../types/processOrder'

export interface ProcessSummaryText {
  compact: string
  full: string
}

export function processSummaryText(record: ProcessOrder): ProcessSummaryText {
  const names = record.processNames?.filter(Boolean) ?? []
  if (!names.length) {
    const fallback = record.isMixProcess === 1 ? '混合工艺' : '单一工艺'
    return { compact: fallback, full: fallback }
  }
  const full = names.join(' + ')
  if (names.length === 1 && record.isMixProcess === 1) {
    return { compact: `${full} · 多段`, full: `${full} · 多段工艺` }
  }
  if (names.length <= 2) return { compact: full, full }
  return { compact: `${names.slice(0, 2).join(' + ')} +${names.length - 2}`, full }
}
