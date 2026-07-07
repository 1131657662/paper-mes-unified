import {
  Background,
  Controls,
  ReactFlow,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import type { OriginalRoll } from '../../types/processOrder'
import type { DetailRouteOutputRow } from '../processOrderDetail/routeConfigDetail'
import { buildRouteFlowModel } from './routeDraftFlowModel'
import { routeDraftEdgeTypes, routeDraftNodeTypes } from './routeDraftFlowRegistry'
import type { RouteDraftStage } from './routeDraftModel'

interface Props {
  onDeleteFrom: (sourceKey: string) => void
  onQuickAppend: (row: DetailRouteOutputRow, stepType: number) => void
  onSelect: (key: string) => void
  roll: OriginalRoll
  selectedKey?: string
  stages: RouteDraftStage[]
}

export default function RouteDraftFlow(props: Props) {
  const model = buildRouteFlowModel(props)

  return (
    <ReactFlow
      nodes={model.nodes}
      edges={model.edges}
      nodeTypes={routeDraftNodeTypes}
      edgeTypes={routeDraftEdgeTypes}
      fitView
      fitViewOptions={{ padding: 0.18, maxZoom: 1.02 }}
      minZoom={0.28}
      maxZoom={1.2}
      nodesDraggable={false}
      nodesConnectable={false}
      proOptions={{ hideAttribution: true }}
    >
      <Background gap={24} size={1} color="#dbeafe" />
      <Controls position="bottom-right" showInteractive={false} />
    </ReactFlow>
  )
}
