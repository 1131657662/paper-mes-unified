import type { Node } from '@xyflow/react'
import type { RollProductionVO } from '../../types/processOrder'
import { formatGram, formatKgWithMaxDecimals, formatMm } from '../../utils/numberFormatters'
import { formatProductionEstimateKg, formatProductionKg } from './orderDetailUtils'
import { productionSourceEstimateWeight } from './productionSourceWeight'
import { descendantEdges, routeEdge, type ProductionFlowEdge } from './productionRouteEdges'
import type { RouteNode } from './productionRouteTree'
import type { ProcessRouteConfigTarget } from './routeConfigTypes'

export interface ProductionFlowNodeData extends Record<string, unknown> {
  appendable?: boolean
  isTrim?: boolean
  kind: 'source' | 'output'
  layerText?: string
  lines: string[]
  onConfigureRoute?: (target: ProcessRouteConfigTarget) => void
  originalUuid?: string
  outputKey?: string
  reconfigurable?: boolean
  stageText?: string
  statusColor?: string
  statusText?: string
  title: string
}

export type ProductionFlowNode = Node<ProductionFlowNodeData, 'productionRoute'>
export interface ProductionRouteFlowOptions {
  canAppendRoute?: boolean
  onConfigureRoute?: (target: ProcessRouteConfigTarget) => void
  originalUuid?: string
  production: RollProductionVO
  roots: RouteNode[]
  sourceProductions?: RollProductionVO[]
}

export interface ProductionRouteFlowModel {
  edges: ProductionFlowEdge[]
  height: number
  nodes: ProductionFlowNode[]
}

interface PositionedRouteNode {
  children: PositionedRouteNode[]
  node: RouteNode
  x: number
  y: number
}

const LEVEL_GAP = 370
const NODE_HEIGHT = 108
const NODE_WIDTH = 286
const ROW_GAP = 144
const TOP_PADDING = 56

export function buildProductionRouteFlow(options: ProductionRouteFlowOptions): ProductionRouteFlowModel {
  const { production, roots } = options
  const sources = normalizeSources(production, options.sourceProductions)
  const positionedRoots = layoutRoots(roots)
  const routeNodes = positionedRoots.flatMap((root) => flattenRouteNode(root, options))
  const sourceNodes = layoutSources(sources, sourceCenterY(positionedRoots))
  const nodes = [...sourceNodes.map(({ source, y, index }) => sourceNode(source, y, index)), ...routeNodes]
  const knownNodeKeys = new Set(routeNodes.map((node) => node.id))
  const edges = sourceNodes.flatMap(({ source, index }) => positionedRoots.map((root) => routeEdge(sourceId(source, index), root.node)))
    .concat(positionedRoots.flatMap((root) => descendantEdges(root, knownNodeKeys)))
  const maxY = Math.max(...sourceNodes.map(({ y }) => y), ...positionedRoots.map(maxNodeY))

  return {
    nodes,
    edges,
    height: Math.max(220, maxY + NODE_HEIGHT + TOP_PADDING),
  }
}

function layoutRoots(roots: RouteNode[]): PositionedRouteNode[] {
  let cursor = TOP_PADDING
  return roots.map((root) => {
    const result = layoutNode(root, 1, cursor)
    cursor = result.nextY
    return result.positioned
  })
}

function layoutNode(node: RouteNode, depth: number, startY: number): { nextY: number; positioned: PositionedRouteNode } {
  if (node.children.length === 0) {
    return {
      nextY: startY + ROW_GAP,
      positioned: { children: [], node, x: depth * LEVEL_GAP, y: startY },
    }
  }

  let cursor = startY
  const children = node.children.map((child) => {
    const result = layoutNode(child, depth + 1, cursor)
    cursor = result.nextY
    return result.positioned
  })
  return {
    nextY: cursor,
    positioned: { children, node, x: depth * LEVEL_GAP, y: midpoint(children) },
  }
}

function flattenRouteNode(item: PositionedRouteNode, options: ProductionRouteFlowOptions): ProductionFlowNode[] {
  const node = outputNode(item, options)
  return [node, ...item.children.flatMap((child) => flattenRouteNode(child, options))]
}

function sourceNode(production: RollProductionVO, y: number, index: number): ProductionFlowNode {
  const weight = sourceWeight(production)
  return {
    id: sourceId(production, index),
    type: 'productionRoute',
    position: { x: 0, y: y - NODE_HEIGHT / 2 },
    data: {
      kind: 'source',
      title: production.rollNo || production.extraNo || production.paperName || '母卷',
      lines: [sourceSpec(production), `${weight.label} ${weight.actual
        ? formatProductionKg(weight.value, production)
        : formatProductionEstimateKg(weight.value)}`],
      statusText: '原卷',
      statusColor: 'blue',
    },
    style: { width: NODE_WIDTH },
  }
}

function outputNode(item: PositionedRouteNode, options: ProductionRouteFlowOptions): ProductionFlowNode {
  const { canAppendRoute, onConfigureRoute, originalUuid } = options
  const actionable = Boolean(canAppendRoute && originalUuid && item.node.outputKey)
  const appendable = actionable && item.node.appendable
  return {
    id: item.node.key,
    type: 'productionRoute',
    position: { x: item.x, y: item.y - NODE_HEIGHT / 2 },
    data: {
      appendable,
      isTrim: item.node.isTrim,
      kind: 'output',
      layerText: item.node.layerText,
      lines: outputLines(item.node),
      onConfigureRoute,
      originalUuid,
      outputKey: item.node.outputKey,
      reconfigurable: actionable && !item.node.appendable,
      stageText: `第${item.node.level}道`,
      statusColor: item.node.statusColor,
      statusText: item.node.statusText,
      title: item.node.title,
    },
    style: { width: NODE_WIDTH },
  }
}

function sourceWeight(production: RollProductionVO): { actual: boolean; label: string; value: number } {
  if (production.actualWeight != null && Number.isFinite(production.actualWeight) && production.actualWeight > 0) {
    return { actual: true, label: '实际', value: production.actualWeight }
  }
  return { actual: false, label: '来料', value: productionSourceEstimateWeight(production) }
}

function outputLines(node: RouteNode): string[] {
  if (node.weight == null) return [node.meta]
  return [node.meta, `${node.weightLabel ?? '预估'} ${formatKgWithMaxDecimals(
    node.weight,
    node.weightLabel === '实际' ? node.weightDigits : 0,
  )}`]
}

function normalizeSources(production: RollProductionVO, sources?: RollProductionVO[]) {
  const candidates = sources?.length ? sources : [production]
  const seen = new Set<string>()
  return candidates.filter((source, index) => {
    const key = sourceId(source, index)
    if (!key || seen.has(key)) return false
    seen.add(key)
    return true
  })
}

function layoutSources(sources: RollProductionVO[], centerY: number) {
  const startY = Math.max(TOP_PADDING, centerY - ((sources.length - 1) * ROW_GAP) / 2)
  return sources.map((source, index) => ({ index, source, y: startY + index * ROW_GAP }))
}

function sourceId(production: RollProductionVO, index: number) {
  return `source-${production.originalUuid || production.rollNo || production.extraNo || `anonymous-${index}`}`
}

function sourceCenterY(roots: PositionedRouteNode[]) {
  if (roots.length === 0) return TOP_PADDING
  return midpoint(roots)
}

function midpoint(nodes: PositionedRouteNode[]) {
  const first = nodes[0]
  const last = nodes.at(-1)
  if (!first || !last) return TOP_PADDING
  return (first.y + last.y) / 2
}

function maxNodeY(node: PositionedRouteNode): number {
  return Math.max(node.y, ...node.children.map(maxNodeY))
}

function sourceSpec(production: RollProductionVO) {
  return `${production.paperName || '-'} / ${formatGram(production.gramWeight)} / ${formatMm(production.originalWidth)}`
}
