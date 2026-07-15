import type { PrintViewVersion } from '../../types/processOrder'

export type PrintIssueMode = 'issue' | 'reprint' | 'preview'

export function resolvePrintIssueMode(
  orderStatus?: number,
  printCount?: number,
  version: PrintViewVersion = 'ISSUED',
): PrintIssueMode {
  if (version === 'FINISHED') return 'preview'
  if (orderStatus === 1 && (printCount ?? 0) === 0) return 'issue'
  if (orderStatus === 2 && (printCount ?? 0) > 0) return 'reprint'
  return 'preview'
}
