import { buildDisplayRows } from '../../../components/processOrder/shared/displayRowBuilder'
import type {
  ProcessOrderDetailVO,
  RollProductionVO,
} from '../../../types/processOrder'
import {
  printMergedSourceItems,
  printRollTitle,
  printSourceItems,
} from './printPreviewSource'
import { buildPrintRouteStages } from './printPreviewStages'
import { applyPrintAnnotations } from './printPreviewAnnotations'
import type { PrintRollBlock, PrintSheetModel } from './printPreviewTypes'

export { buildPrintSummary } from './printPreviewSummary'
export type {
  PrintRollBlock,
  PrintRouteOutput,
  PrintRouteStage,
  PrintAnnotation,
  PrintAnnotationField,
  PrintSheetModel,
  PrintSummaryItem,
} from './printPreviewTypes'

export function buildPrintSheetModel(detail: ProcessOrderDetailVO): PrintSheetModel {
  return applyPrintAnnotations(detail.finishRolls, buildPrintRollBlocks(detail))
}

export function buildPrintRollBlocks(detail: ProcessOrderDetailVO): PrintRollBlock[] {
  return buildDisplayRows(detail.rollProductions ?? []).map((row) => {
    const production = row.isMergeGroup
      ? { ...row.mainProduction, finishes: row.finishes }
      : row.mainProduction
    return {
      key: row.key,
      title: printRollTitle(row.seq, production, row.isMergeGroup),
      sourceItems: row.isMergeGroup
        ? printMergedSourceItems(row.rollProductions)
        : printSourceItems(production),
      remark: mergedRemark(row.rollProductions),
      routeStages: buildPrintRouteStages(production, detail.steps, detail.finishRolls),
    }
  })
}

function mergedRemark(productions: RollProductionVO[]) {
  const remarks = productions.filter((item) => item.remark?.trim())
  if (remarks.length <= 1) return remarks[0]?.remark?.trim()
  return remarks.map((item, index) => {
    const name = item.rollNo || item.extraNo || `母卷 ${index + 1}`
    return `${name}：${item.remark?.trim()}`
  }).join('；')
}
