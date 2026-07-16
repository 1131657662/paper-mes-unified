import { isDeliverableProductionFinish } from '../../../components/processOrder/shared/detailHelpers'
import {
  compareFinishProductions,
  compareFinishSources,
  compareRollProductions,
} from '../../../components/processOrder/shared/productionSpecificationOrder'
import type {
  FinishProductionVO,
  FinishSourceVO,
  RollProductionVO,
} from '../../../types/processOrder'

export interface FinishedProductRow {
  finish: FinishProductionVO
  key: string
  sources: FinishSourceVO[]
}

export interface FinishedProductTotals {
  actualWeight: number
  deliverableCount: number
  difference: number
  estimateWeight: number
  recordedCount: number
}

export function buildFinishedProductRows(
  productions: RollProductionVO[],
): FinishedProductRow[] {
  const rows: FinishedProductRow[] = []
  const seen = new Set<string>()

  for (const production of [...productions].sort(compareRollProductions)) {
    for (const finish of [...(production.finishes ?? [])].sort(compareFinishProductions)) {
      if (seen.has(finish.uuid)) continue
      seen.add(finish.uuid)
      rows.push({
        finish,
        key: finish.uuid,
        sources: finish.sources?.length
          ? [...finish.sources].sort(compareFinishSources)
          : fallbackSources(production),
      })
    }
  }
  return rows
}

export function calculateFinishedProductTotals(
  rows: FinishedProductRow[],
): FinishedProductTotals {
  const deliverable = rows.filter(({ finish }) => isDeliverableProductionFinish(finish))
  const estimateWeight = sumWeight(deliverable, 'estimateWeight')
  const actualWeight = sumWeight(deliverable, 'actualWeight')
  return {
    actualWeight,
    deliverableCount: deliverable.length,
    difference: actualWeight - estimateWeight,
    estimateWeight,
    recordedCount: deliverable.filter(({ finish }) => finish.actualWeight != null).length,
  }
}

function fallbackSources(production: RollProductionVO): FinishSourceVO[] {
  if (!production.rollNo && !production.paperName) return []
  return [{ rollNo: production.rollNo, paperName: production.paperName }]
}

function sumWeight(
  rows: FinishedProductRow[],
  field: 'actualWeight' | 'estimateWeight',
): number {
  return rows.reduce((sum, { finish }) => sum + (finish[field] ?? 0), 0)
}
