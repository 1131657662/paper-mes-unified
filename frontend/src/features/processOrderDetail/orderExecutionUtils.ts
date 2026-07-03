import type { ProcessOrderDetailVO, RollProductionVO } from '../../types/processOrder'

export interface ExecutionSummary {
  officialCount: number
  spareCount: number
  printableWarnings: string[]
  statusHint: string
}

export function buildExecutionSummary(detail?: ProcessOrderDetailVO): ExecutionSummary {
  const productions = detail?.rollProductions ?? []
  const officialCount = (detail?.finishRolls ?? []).filter((roll) => roll.isSpare !== 1 && roll.rollNoStatus !== 3).length
  const spareCount = (detail?.finishRolls ?? []).filter((roll) => roll.isSpare === 1 && roll.rollNoStatus !== 3).length

  const status = detail?.order?.orderStatus

  return {
    officialCount,
    spareCount,
    printableWarnings: status === 1 ? buildPrintableWarnings(productions, officialCount) : [],
    statusHint: buildStatusHint(detail),
  }
}

function buildPrintableWarnings(productions: RollProductionVO[], officialCount: number): string[] {
  const warnings: string[] = []
  const processRolls = productions.filter((roll) => roll.processMode !== 3)
  const missingStep = processRolls.filter((roll) => !(roll.steps ?? []).some((step) => step.isMain === 1))

  if (missingStep.length > 0) {
    warnings.push(`${missingStep.length} 卷缺少主工序，打印下发会被后端拦截`)
  }
  if (processRolls.length > 0 && officialCount === 0) {
    warnings.push('尚未看到正式成品卷号，请先确认加工方案或管理成品号')
  }
  return warnings
}

function buildStatusHint(detail?: ProcessOrderDetailVO): string {
  const status = detail?.order?.orderStatus
  if (status === 1) return '当前可以预览车间单据并打印下发，首打会锁定下发快照。'
  if (status === 2) return '车间加工中，可补打加工单，完工后转入待回录。'
  if (status === 3) return '等待录入车间实测重量，回录通过后生成完成快照。'
  if (status === 4) return '加工已完成，可以出库、结算，并查看下发与完成差异。'
  if (status === 5) return '单据已结算，金额和关键生产数据应保持锁定。'
  if (status === 6) return '单据已作废，不再参与生产、出库、结算和报表统计。'
  return '草稿或未知状态，请先完成新建加工单提交。'
}
