import type { DetailRouteOutputRow } from '../processOrderDetail/routeConfigDetail'
import { ORIGINAL_OUTPUT_KEY } from './routeDraftModel'
import type { RouteDraftStage } from './routeDraftModel'

export interface RoutePoint {
  x: number
  y: number
}

export interface RouteDraftLayout {
  outputPositions: Map<string, RoutePoint>
  processPositions: Map<string, RoutePoint>
}

export const ROUTE_SOURCE_Y = 120
export const ROUTE_X_GAP = 440
export const ROUTE_Y_GAP = 158

const PROCESS_X_OFFSET = 190

export function buildRouteLayout(
  stages: RouteDraftStage[],
  outputsByStage: Map<string, DetailRouteOutputRow[]>,
): RouteDraftLayout {
  const outputPositions = new Map<string, RoutePoint>([[ORIGINAL_OUTPUT_KEY, { x: 0, y: ROUTE_SOURCE_Y }]])
  const itemsByLevel = new Map<number, LayoutItem[]>()

  stages.forEach((stage) => {
    const sourceY = averageY(stageSourceKeys(stage), outputPositions)
    const rows = outputsByStage.get(stage.id) ?? []
    rows.forEach((row, index) => {
      const point = { x: ROUTE_X_GAP * Number(row.stageLevel || 1), y: childY(sourceY, rows.length, index) }
      outputPositions.set(row.outputKey, point)
      addLevelItem(itemsByLevel, row.stageLevel, { key: row.outputKey, point })
    })
  })

  settleLevelItems(itemsByLevel)
  return { outputPositions, processPositions: processPositions(stages, outputsByStage, outputPositions) }
}

function processPositions(
  stages: RouteDraftStage[],
  outputsByStage: Map<string, DetailRouteOutputRow[]>,
  outputPositions: Map<string, RoutePoint>,
): Map<string, RoutePoint> {
  return new Map(stages.map((stage) => [
    stage.id,
    {
      x: ROUTE_X_GAP * stage.stageLevel - PROCESS_X_OFFSET,
      y: processY(stage, outputsByStage.get(stage.id) ?? [], outputPositions),
    },
  ]))
}

function processY(stage: RouteDraftStage, outputs: DetailRouteOutputRow[], positions: Map<string, RoutePoint>): number {
  const sourceY = averageY(stageSourceKeys(stage), positions)
  if (!outputs.length) return sourceY
  return average(outputs.map((row) => positions.get(row.outputKey)?.y ?? sourceY))
}

function childY(sourceY: number, count: number, index: number): number {
  return sourceY + (index - (count - 1) / 2) * ROUTE_Y_GAP
}

function averageY(keys: string[], positions: Map<string, RoutePoint>): number {
  return average(keys.map((key) => positions.get(key)?.y ?? ROUTE_SOURCE_Y))
}

function average(values: number[]): number {
  if (!values.length) return ROUTE_SOURCE_Y
  return values.reduce((sum, value) => sum + value, 0) / values.length
}

function addLevelItem(itemsByLevel: Map<number, LayoutItem[]>, level: number, item: LayoutItem): void {
  const items = itemsByLevel.get(level) ?? []
  items.push(item)
  itemsByLevel.set(level, items)
}

function settleLevelItems(itemsByLevel: Map<number, LayoutItem[]>): void {
  itemsByLevel.forEach((items) => {
    const sorted = [...items].sort((a, b) => a.point.y - b.point.y || a.key.localeCompare(b.key))
    sorted.forEach((item, index) => {
      const prev = sorted[index - 1]
      if (prev && item.point.y - prev.point.y < ROUTE_Y_GAP) {
        item.point.y = prev.point.y + ROUTE_Y_GAP
      }
    })
  })
}

function stageSourceKeys(stage: RouteDraftStage): string[] {
  if (stage.stageLevel <= 1) return [ORIGINAL_OUTPUT_KEY]
  return stage.inputOutputKeys
}

interface LayoutItem {
  key: string
  point: RoutePoint
}
