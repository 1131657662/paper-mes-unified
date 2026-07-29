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

export function printIssueDrawerTitle(mode: PrintIssueMode, version: PrintViewVersion): string {
  if (mode === 'issue') return '下发加工单'
  if (mode === 'unprinted') return '确认加工单打印'
  if (mode === 'reprint') return '补打加工单'
  if (mode === 'audited-reprint') return version === 'FINISHED' ? '补打完工版本' : '补打下发版本'
  return version === 'FINISHED' ? '完工版本打印预览' : '下发版本打印预览'
}
