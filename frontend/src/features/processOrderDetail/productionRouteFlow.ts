import type { Edge, Node } from '@xyflow/react'
import type { RollProductionVO } from '../../types/processOrder'
import { formatGram, formatKgWithMaxDecimals, formatMm } from '../../utils/numberFormatters'
import { formatProductionKg } from './orderDetailUtils'
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
export type ProductionFlowEdge = Edge<Record<string, unknown>, 'smoothstep'>

export interface ProductionRouteFlowOptions {
  canAppendRoute?: boolean
  onConfigureRoute?: (target: ProcessRouteConfigTarget) => void
  originalUuid?: string
  production: RollProductionVO
  roots: RouteNode[]
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

const SOURCE_ID = 'source'
const LEVEL_GAP = 370
const NODE_HEIGHT = 108
const NODE_WIDTH = 286
const ROW_GAP = 144
const TOP_PADDING = 56

export function buildProductionRouteFlow(options: ProductionRouteFlowOptions): ProductionRouteFlowModel {
  const { production, roots } = options
  const positionedRoots = layoutRoots(roots)
  const routeNodes = positionedRoots.flatMap((root) => flattenRouteNode(root, options))
  const sourceY = sourceCenterY(positionedRoots)
  const nodes = [sourceNode(production, sourceY), ...routeNodes]
  const edges = positionedRoots.flatMap((root) => routeEdges(root, SOURCE_ID))
  const maxY = Math.max(sourceY, ...positionedRoots.map(maxNodeY))

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

function sourceNode(production: RollProductionVO, y: number): ProductionFlowNode {
  const weight = sourceWeight(production)
  return {
    id: SOURCE_ID,
    type: 'productionRoute',
    position: { x: 0, y: y - NODE_HEIGHT / 2 },
    data: {
      kind: 'source',
      title: production.rollNo || production.extraNo || production.paperName || '母卷',
      lines: [sourceSpec(production), `${weight.label} ${formatProductionKg(weight.value, production)}`],
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

function sourceWeight(production: RollProductionVO): { label: string; value: number } {
  if (production.actualWeight != null) return { label: '实际', value: production.actualWeight }
  return { label: '来料', value: (production.rollWeight ?? 0) * (production.pieceNum ?? 1) }
}

function outputLines(node: RouteNode): string[] {
  if (node.weight == null) return [node.meta]
  return [node.meta, `${node.weightLabel ?? '预估'} ${formatKgWithMaxDecimals(node.weight, node.weightDigits)}`]
}

function routeEdges(item: PositionedRouteNode, parentId: string): ProductionFlowEdge[] {
  const currentEdge: ProductionFlowEdge = {
    id: `${parentId}-${item.node.key}`,
    source: parentId,
    target: item.node.key,
    type: 'smoothstep',
    label: item.node.processLabel,
    labelBgBorderRadius: 10,
    labelBgPadding: [8, 4],
    labelBgStyle: { fill: '#eff6ff', fillOpacity: 0.96 },
    labelStyle: { fill: '#0958d9', fontSize: 12, fontWeight: 650 },
    style: { stroke: '#93c5fd', strokeWidth: 1.5 },
  }
  return [currentEdge, ...item.children.flatMap((child) => routeEdges(child, item.node.key))]
}

function sourceCenterY(roots: PositionedRouteNode[]) {
  if (roots.length === 0) return TOP_PADDING
  return midpoint(roots)
}

function midpoint(nodes: PositionedRouteNode[]) {
  return (nodes[0].y + nodes[nodes.length - 1].y) / 2
}

function maxNodeY(node: PositionedRouteNode): number {
  return Math.max(node.y, ...node.children.map(maxNodeY))
}

function sourceSpec(production: RollProductionVO) {
  return `${production.paperName || '-'} / ${formatGram(production.gramWeight)} / ${formatMm(production.originalWidth)}`
}
