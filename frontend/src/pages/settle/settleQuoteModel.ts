import type { SettleCandidateVO, SettleQuoteLine } from '../../types/settle'

export function applyQuoteLines(
  candidates: SettleCandidateVO[],
  lines: SettleQuoteLine[] = [],
): SettleCandidateVO[] {
  const quotedByOrder = new Map(lines.map((line) => [line.orderUuid, line]))
  return candidates.map((candidate) => {
    const quote = quotedByOrder.get(candidate.orderUuid)
    if (!quote) return candidate
    return {
      ...candidate,
      sawAmount: quote.sawAmount,
      rewindAmount: quote.rewindAmount,
      extraAmount: quote.extraAmount,
      totalAmount: quote.totalAmount,
    }
  })
}
