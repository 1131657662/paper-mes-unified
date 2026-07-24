import type { OriginalRoll } from '../../types/processOrder'
import type { DetailRouteOutputRow } from './routeConfigModel'

export interface RouteOutputSeed {
  estimateWeight: number
  finishCoreDiameter?: number
  finishDiameter?: number
  finishWidth: number
  gramWeight?: number
  isRemain?: number
  paperName?: string
}

export function seedFromSource(source: DetailRouteOutputRow): RouteOutputSeed {
  return {
    estimateWeight: source.estimateWeight,
    finishCoreDiameter: source.finishCoreDiameter,
    finishDiameter: source.finishDiameter,
    finishWidth: source.finishWidth,
    gramWeight: source.gramWeight,
    paperName: source.paperName,
  }
}

export function appendTrimSeed(
  rows: RouteOutputSeed[],
  source: DetailRouteOutputRow,
  width: number,
  weight: number,
): RouteOutputSeed[] {
  const finishWidth = Math.max(0, Math.round(width))
  if (finishWidth <= 0 && weight <= 0) return rows
  return [...rows, {
    estimateWeight: roundWeight(weight),
    finishCoreDiameter: source.finishCoreDiameter,
    finishDiameter: source.finishDiameter,
    finishWidth,
    gramWeight: source.gramWeight,
    isRemain: 1,
    paperName: source.paperName,
  }]
}

export function calcTrimWeight(sourceWeight: number, sourceWidth: number, trimWidth: number) {
  if (sourceWeight <= 0 || sourceWidth <= 0 || trimWidth <= 0) return 0
  return roundWeight(sourceWeight * trimWidth / sourceWidth)
}

export function rollTotalWeight(roll: OriginalRoll) {
  return Number(
    roll.actualWeight
    ?? roll.totalWeight
    ?? (Number(roll.rollWeight ?? 0) * Number(roll.pieceNum ?? 1)),
  )
}

export function finishWeight(roll: OriginalRoll, value?: number, width?: number) {
  const explicit = Number(value ?? 0)
  if (explicit > 0) return explicit
  return roundWeight(
    rollTotalWeight(roll)
    * Number(width ?? roll.originalWidth ?? 1)
    / Number(roll.originalWidth ?? 1),
  )
}

export function roundWeight(value: number) {
  return Number(value.toFixed(3))
}

export function combinedSource(sources: DetailRouteOutputRow[]): DetailRouteOutputRow {
  const [first] = sources
  if (!first) return emptySource()
  if (sources.length === 1) return first
  const keys = sources.map((row) => row.outputKey)
  return {
    ...first,
    estimateWeight: roundWeight(sources.reduce((sum, row) => sum + row.estimateWeight, 0)),
    label: `${sources.length}个来源合并`,
    outputKey: keys.join('+'),
    sourceOutputKey: keys.join('、'),
    sourceRollNo: sources
      .map((row) => row.finishRollNo || row.sourceRollNo || row.outputKey)
      .join('、'),
  }
}

export function sourceRowsFromKeys(rows: DetailRouteOutputRow[], keys: string[]) {
  const keySet = new Set(keys)
  return rows.filter((row) => keySet.has(row.outputKey))
}

export function roundPercent(weight: number, total: number) {
  if (!total) return 0
  return Number((weight * 100 / total).toFixed(2))
}

function emptySource(): DetailRouteOutputRow {
  return {
    estimateWeight: 0,
    finishWidth: 1,
    label: '未选择来源',
    outputKey: '',
    stageLevel: 1,
  }
}
