import type { Edge } from '@xyflow/react'
import type { RouteNode } from './productionRouteTree'

export type ProductionFlowEdge = Edge<Record<string, unknown>, 'smoothstep'>

export interface PositionedRouteNodeForEdges {
  children: PositionedRouteNodeForEdges[]
  node: RouteNode
}

export function descendantEdges(
  item: PositionedRouteNodeForEdges,
  knownNodeKeys: ReadonlySet<string> = new Set(),
): ProductionFlowEdge[] {
  return item.children.flatMap((child) => [
    ...parentEdges(item.node.key, child.node, knownNodeKeys),
    ...descendantEdges(child, knownNodeKeys),
  ])
}

function parentEdges(
  fallbackParentKey: string,
  child: RouteNode,
  knownNodeKeys: ReadonlySet<string>,
): ProductionFlowEdge[] {
  const parents = child.parentKeys?.length ? child.parentKeys : [fallbackParentKey]
  return parents
    .filter((parentKey) => parentKey === fallbackParentKey || knownNodeKeys.has(parentKey))
    .map((parentKey) => routeEdge(parentKey, child))
}

export function routeEdge(parentId: string, node: RouteNode): ProductionFlowEdge {
  return {
    id: `${parentId}-${node.key}`,
    source: parentId,
    target: node.key,
    type: 'smoothstep',
    label: node.processLabel,
    labelBgBorderRadius: 10,
    labelBgPadding: [8, 4],
    labelBgStyle: { fill: '#eff6ff', fillOpacity: 0.96 },
    labelStyle: { fill: '#0958d9', fontSize: 12, fontWeight: 650 },
    style: { stroke: '#93c5fd', strokeWidth: 1.5 },
  }
}
