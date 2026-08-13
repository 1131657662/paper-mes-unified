import type { ProcessOrderDetailVO, RollProductionVO } from '../../types/processOrder'
import { processModeRequiresMain } from '../../constants/processOrder'
import { hasConfirmedProcessOrderPrint, hasHistoricalUnconfirmedPrint } from './processOrderPrintStage'

export interface ExecutionSummary {
  officialCount: number
  spareCount: number
  printableWarnings: string[]
  statusHint: string
}

export function buildExecutionSummary(detail?: ProcessOrderDetailVO): ExecutionSummary {
  const productions = detail?.rollProductions ?? []
  const officialCount = (detail?.finishRolls ?? []).filter((roll) => roll.isSpare !== 1 && roll.isRemain !== 1 && roll.rollNoStatus !== 3).length
  const spareCount = (detail?.finishRolls ?? []).filter((roll) => roll.isSpare === 1 && roll.rollNoStatus !== 3).length

  const status = detail?.order?.orderStatus
  const hasConfirmedPrint = hasConfirmedProcessOrderPrint(
    detail?.order?.printStatus,
    detail?.order?.printCount,
  )
  const historicalPrintRisk = status != null
    && hasHistoricalUnconfirmedPrint(status, hasConfirmedPrint)

  return {
    officialCount,
    spareCount,
    printableWarnings: historicalPrintRisk
      ? ['该历史加工单没有人工确认打印记录，请完成补打确认后再出库或结算']
      : status === 1 ? buildPrintableWarnings(productions, officialCount) : [],
    statusHint: buildStatusHint(detail),
  }
}

function buildPrintableWarnings(productions: RollProductionVO[], officialCount: number): string[] {
  const warnings: string[] = []
  const processRolls = productions.filter((roll) => roll.processMode !== 3)
  const mainProcessRolls = productions.filter((roll) => processModeRequiresMain(roll.processMode))
  const mergedSources = mergedRewindSourceIds(productions)
  const missingStep = mainProcessRolls.filter((roll) => {
    const hasMainStep = (roll.steps ?? []).some((step) => step.isMain === 1)
    const coveredByMergedRewind = roll.mainStepType === 2
      && roll.originalUuid != null
      && mergedSources.has(roll.originalUuid)
    return !hasMainStep && !coveredByMergedRewind
  })

  if (missingStep.length > 0) {
    warnings.push(`${missingStep.length} 卷缺少主工序，打印下发会被后端拦截`)
  }
  if (processRolls.length > 0 && officialCount === 0) {
    warnings.push('尚未看到正式成品卷号，请先确认加工方案或管理成品号')
  }
  return warnings
}

function mergedRewindSourceIds(productions: RollProductionVO[]): Set<string> {
  const sourceIds = new Set<string>()
  for (const production of productions) {
    const mergedPlan = (production.rewindParams ?? []).some((param) => param.paramMode === 5)
    const rewindMain = (production.steps ?? []).some((step) => step.isMain === 1 && step.stepType === 2)
    if (!mergedPlan || !rewindMain) continue
    for (const finish of production.finishes ?? []) {
      if (!isFormalFinish(finish)) continue
      const sources = [...new Set((finish.sources ?? []).flatMap((source) => source.originalUuid ?? []))]
      if (sources.length > 1) sources.forEach((sourceUuid) => sourceIds.add(sourceUuid))
    }
  }
  return sourceIds
}

function isFormalFinish(finish: NonNullable<RollProductionVO['finishes']>[number]): boolean {
  return finish.isSpare !== 1
    && finish.isRemain !== 1
    && finish.rollNoStatus !== 3
    && finish.finishStatus !== 4
}

function buildStatusHint(detail?: ProcessOrderDetailVO): string {
  const status = detail?.order?.orderStatus
  const hasConfirmedPrint = hasConfirmedProcessOrderPrint(
    detail?.order?.printStatus,
    detail?.order?.printCount,
  )
  if (status != null && hasHistoricalUnconfirmedPrint(status, hasConfirmedPrint)) {
    return '历史单缺少人工确认打印记录，完成补打确认后才能继续流转。'
  }
  if (status === 1) return '当前可以预览车间单据并打印下发，首打会锁定下发快照。'
  if (status === 2) return '车间加工中，可补打加工单，完工后转入待回录。'
  if (status === 3) return '等待录入车间实测重量，回录通过后生成完成快照。'
  if (status === 4) return '加工已完成，可以出库、结算，并查看下发与完成差异。'
  if (status === 5) return '单据已结算，金额和关键生产数据应保持锁定。'
  if (status === 6) return '单据已作废，不再参与生产、出库、结算和报表统计。'
  return '草稿或未知状态，请先完成新建加工单提交。'
}
