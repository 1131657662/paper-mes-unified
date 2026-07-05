import type { Edge, Node } from '@xyflow/react'
import type { OriginalRoll } from '../../types/processOrder'
import { formatKg } from '../../utils/numberFormatters'
import type { DetailRouteOutputRow } from '../processOrderDetail/routeConfigDetail'
import { routeStepName } from '../processOrderDetail/routeConfigDetail'
import {
  ORIGINAL_OUTPUT_KEY,
  allRouteOutputs,
  routeOriginalSource,
  routeOutputsByStage,
} from './routeDraftModel'
import { ROUTE_SOURCE_Y, buildRouteLayout, type RouteDraftLayout } from './routeDraftLayout'
import type { RouteDraftStage } from './routeDraftModel'

export interface RouteOutputNodeData extends Record<string, unknown> {
  appendable?: boolean
  deletable?: boolean
  lines: string[]
  onDelete?: () => void
  onQuickAppend?: (stepType: number) => void
  onSelect?: () => void
  selected?: boolean
  status: string
  title: string
}

export interface RouteProcessNodeData extends Record<string, unknown> {
  caption: string
  onSelect?: () => void
  selected?: boolean
  title: string
}

export type RouteOutputNode = Node<RouteOutputNodeData, 'routeNode'>
export type RouteProcessNode = Node<RouteProcessNodeData, 'processNode'>
export type RouteDraftNode = RouteOutputNode | RouteProcessNode
export type RouteDraftEdge = Edge<Record<string, unknown>, 'routeEdge'>

export interface RouteDraftFlowOptions {
  onDeleteFrom: (sourceKey: string) => void
  onQuickAppend: (row: DetailRouteOutputRow, stepType: number) => void
  onSelect: (key: string) => void
  roll: OriginalRoll
  selectedKey?: string
  stages: RouteDraftStage[]
}

const NODE_WIDTH = 238
const PROCESS_WIDTH = 112

export function buildRouteFlowModel(options: RouteDraftFlowOptions): { edges: RouteDraftEdge[]; nodes: RouteDraftNode[] } {
  const outputs = allRouteOutputs(options.roll, options.stages)
  const outputsByStage = routeOutputsByStage(options.roll, options.stages)
  const layout = buildRouteLayout(options.stages, outputsByStage)
  return {
    nodes: [
      sourceNode(options),
      ...processNodes(options.stages, outputsByStage, options, layout),
      ...outputs.map((row) => outputNode(row, options, layout)),
    ],
    edges: stageEdges(options.stages, outputsByStage),
  }
}

function sourceNode(options: RouteDraftFlowOptions): RouteOutputNode {
  const source = routeOriginalSource(options.roll)
  return {
    id: ORIGINAL_OUTPUT_KEY,
    type: 'routeNode',
    position: { x: 0, y: ROUTE_SOURCE_Y },
    data: {
      appendable: true,
      onQuickAppend: (stepType) => options.onQuickAppend(source, stepType),
      onSelect: () => options.onSelect(ORIGINAL_OUTPUT_KEY),
      selected: options.selectedKey === ORIGINAL_OUTPUT_KEY,
      title: options.roll.rollNo || options.roll.extraNo || options.roll.paperName || '母卷',
      status: '母卷',
      lines: [
        `${options.roll.paperName || '-'} / ${options.roll.gramWeight ?? '-'}g / ${options.roll.originalWidth ?? '-'}mm`,
        `来料 ${formatKg(source.estimateWeight)}`,
      ],
    },
    style: { width: NODE_WIDTH },
  }
}

function outputNode(row: DetailRouteOutputRow, options: RouteDraftFlowOptions, layout: RouteDraftLayout): RouteOutputNode {
  const consumed = consumedKeys(options.stages).has(row.outputKey)
  return {
    id: row.outputKey,
    type: 'routeNode',
    position: layout.outputPositions.get(row.outputKey) ?? { x: 0, y: ROUTE_SOURCE_Y },
    data: {
      appendable: !consumed,
      deletable: true,
      onDelete: () => options.onDeleteFrom(row.outputKey),
      onQuickAppend: (stepType) => options.onQuickAppend(row, stepType),
      onSelect: () => options.onSelect(row.outputKey),
      selected: options.selectedKey === row.outputKey,
      status: consumed ? '中间产物' : '最终成品',
      title: row.outputKey,
      lines: [
        `${row.paperName || '-'} / ${row.gramWeight ?? '-'}g / ${row.finishWidth ?? '-'}mm`,
        `预估 ${formatKg(row.estimateWeight)}`,
      ],
    },
    style: { width: NODE_WIDTH },
  }
}

function processNodes(
  stages: RouteDraftStage[],
  outputsByStage: Map<string, DetailRouteOutputRow[]>,
  options: RouteDraftFlowOptions,
  layout: RouteDraftLayout,
): RouteProcessNode[] {
  return stages.map((stage) => {
    const rows = outputsByStage.get(stage.id) ?? []
    const sourceKey = stageSourceKeys(stage)[0] ?? ORIGINAL_OUTPUT_KEY
    return {
      id: processNodeId(stage.id),
      type: 'processNode',
      position: layout.processPositions.get(stage.id) ?? { x: 0, y: ROUTE_SOURCE_Y },
      data: {
        caption: `第${stage.stageLevel}道 · ${rows.length}个产物`,
        onSelect: () => options.onSelect(sourceKey),
        selected: options.selectedKey === sourceKey,
        title: routeStepName(stage.stepType),
      },
      style: { width: PROCESS_WIDTH },
    }
  })
}

function stageEdges(stages: RouteDraftStage[], outputsByStage: Map<string, DetailRouteOutputRow[]>): RouteDraftEdge[] {
  return stages.flatMap((stage) => {
    const processId = processNodeId(stage.id)
    const inputEdges = stageSourceKeys(stage).map((sourceKey) => routeEdge(`${sourceKey}-${processId}`, sourceKey, processId))
    const outputEdges = (outputsByStage.get(stage.id) ?? []).map((row) => routeEdge(`${processId}-${row.outputKey}`, processId, row.outputKey))
    return [...inputEdges, ...outputEdges]
  })
}

function routeEdge(id: string, source: string, target: string): RouteDraftEdge {
  return {
    id,
    source,
    target,
    type: 'routeEdge',
    style: { stroke: '#93c5fd', strokeWidth: 1.5 },
  }
}

function stageSourceKeys(stage: RouteDraftStage): string[] {
  if (stage.stageLevel <= 1) return [ORIGINAL_OUTPUT_KEY]
  return stage.inputOutputKeys
}

function processNodeId(stageId: string) {
  return `process-${stageId}`
}

function consumedKeys(stages: RouteDraftStage[]) {
  return new Set(stages.flatMap((stage) => stage.inputOutputKeys))
}
