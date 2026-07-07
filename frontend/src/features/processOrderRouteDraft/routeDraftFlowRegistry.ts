import type { EdgeTypes, NodeTypes } from '@xyflow/react'
import { RouteFlowEdge, RouteFlowNode, RouteFlowProcessNode } from './RouteDraftFlowParts'

export const routeDraftNodeTypes: NodeTypes = {
  processNode: RouteFlowProcessNode,
  routeNode: RouteFlowNode,
}

export const routeDraftEdgeTypes: EdgeTypes = {
  routeEdge: RouteFlowEdge,
}
