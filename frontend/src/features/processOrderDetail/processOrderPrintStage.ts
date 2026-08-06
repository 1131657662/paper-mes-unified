import type { ProcessOrderPrintStage } from '../../types/processOrder'

const STAGE_STATUS: Partial<Record<ProcessOrderPrintStage, number>> = {
  DRAFT: 0,
  PENDING_ISSUE: 1,
  PENDING_MANUAL_CONFIRM: 2,
  WAITING_BACK_RECORD: 3,
  COMPLETED: 4,
  SETTLED: 5,
  VOIDED: 6,
}

/** Prefer the server's explicit stage while keeping older responses usable. */
export function resolveProcessOrderStatus(
  printStage: ProcessOrderPrintStage | undefined,
  fallbackStatus?: number,
): number {
  const stageStatus = printStage == null ? undefined : STAGE_STATUS[printStage]
  return stageStatus ?? fallbackStatus ?? 0
}

export function hasConfirmedProcessOrderPrint(printStatus?: number, printCount?: number): boolean {
  return printStatus === 1 && (printCount ?? 0) > 0
}

export function hasHistoricalUnconfirmedPrint(status: number, hasConfirmedPrint: boolean): boolean {
  return status >= 4 && status <= 5 && !hasConfirmedPrint
}
