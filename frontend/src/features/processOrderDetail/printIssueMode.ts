import type { PrintViewVersion } from '../../types/processOrder'

export type PrintIssueMode = 'issue' | 'unprinted' | 'reprint' | 'audited-reprint' | 'preview'

export function resolvePrintIssueMode(
  orderStatus?: number,
  printCount?: number,
  version: PrintViewVersion = 'ISSUED',
  printStatus?: number,
): PrintIssueMode {
  if (version === 'FINISHED') return orderStatus != null && orderStatus >= 4 && orderStatus <= 5 ? 'audited-reprint' : 'preview'
  if (orderStatus === 1 && (printCount ?? 0) === 0) return 'issue'
  if (orderStatus === 2 && printStatus === 0 && (printCount ?? 0) === 0) return 'unprinted'
  if (orderStatus === 2 && (printCount ?? 0) > 0) return 'reprint'
  if (orderStatus != null && orderStatus >= 3 && orderStatus <= 5) return 'audited-reprint'
  return 'preview'
}
