import { buildDisplayRows } from '../../../components/processOrder/shared/displayRowBuilder'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import { printRollTitle, printSourceItems } from './printPreviewSource'
import { buildPrintRouteStages } from './printPreviewStages'
import type { PrintRollBlock } from './printPreviewTypes'

export { buildPrintSummary } from './printPreviewSummary'
export type {
  PrintRollBlock,
  PrintRouteOutput,
  PrintRouteStage,
  PrintSummaryItem,
} from './printPreviewTypes'

export function buildPrintRollBlocks(detail: ProcessOrderDetailVO): PrintRollBlock[] {
  return buildDisplayRows(detail.rollProductions ?? []).map((row) => {
    const production = row.mainProduction
    return {
      key: row.key,
      title: printRollTitle(row.seq, production, row.isMergeGroup),
      sourceItems: printSourceItems(production),
      remark: production.remark,
      routeStages: buildPrintRouteStages(production, detail.steps),
    }
  })
}
