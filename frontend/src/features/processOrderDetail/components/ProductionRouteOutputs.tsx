import { BranchesOutlined, SyncOutlined } from '@ant-design/icons'
import type { MouseEvent } from 'react'
import { Button, Tag } from 'antd'
import {
  Background,
  Controls,
  Handle,
  Position,
  ReactFlow,
  type NodeProps,
  type NodeTypes,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import MesTooltip from '../../../components/biz/MesTooltip'
import type { FinishProductionVO, RollProductionVO, StageOutputVO } from '../../../types/processOrder'
import { buildProductionRouteFlow, type ProductionFlowNode } from '../productionRouteFlow'
import { buildRouteTree } from '../productionRouteTree'
import type { ProcessRouteConfigTarget } from '../routeConfigTypes'

interface Props {
  canAppendRoute?: boolean
  finishes?: FinishProductionVO[]
  onConfigureRoute?: (target: ProcessRouteConfigTarget) => void
  originalUuid?: string
  outputs?: StageOutputVO[]
  production: RollProductionVO
}

const nodeTypes: NodeTypes = {
  productionRoute: ProductionRouteNode,
}

export default function ProductionRouteOutputs({
  canAppendRoute,
  finishes = [],
  onConfigureRoute,
  originalUuid,
  outputs = [],
  production,
}: Props) {
  const roots = buildRouteTree(outputs, finishes, fallbackProcessLabel(production), production)
  if (!roots.length) return null

  const flow = buildProductionRouteFlow({ canAppendRoute, onConfigureRoute, originalUuid, production, roots })

  return (
    <div className="production-route-map">
      <div className="production-route-map__title">链式产物路线</div>
      <div className="production-route-flow" style={{ height: flow.height }}>
        <ReactFlow
          nodes={flow.nodes}
          edges={flow.edges}
          nodeTypes={nodeTypes}
          fitView
          fitViewOptions={{ padding: 0.18, maxZoom: 1.05 }}
          minZoom={0.42}
          maxZoom={1.15}
          nodesDraggable={false}
          nodesConnectable={false}
          elementsSelectable={false}
          panOnDrag={false}
          panOnScroll={false}
          preventScrolling={false}
          zoomOnScroll={false}
          proOptions={{ hideAttribution: true }}
        >
          <Background gap={24} size={1} color="#dbeafe" />
          <Controls position="bottom-right" showInteractive={false} />
        </ReactFlow>
      </div>
    </div>
  )
}

function fallbackProcessLabel(production: RollProductionVO) {
  if (production.processMode === 3) return '直发'
  return production.mainStepType === 2 ? '复卷' : '锯纸'
}

function ProductionRouteNode({ data }: NodeProps<ProductionFlowNode>) {
  return (
    <div className={`production-route-flow-card production-route-flow-card--${data.kind}`}>
      <Handle type="target" position={Position.Left} className="production-route-flow-card__handle" />
      <div className="production-route-flow-card__header">
        <strong>{data.title}</strong>
        <NodeTags data={data} />
      </div>
      <div className="production-route-flow-card__lines">
        {data.lines.map((line) => (
          <span key={line}>{line}</span>
        ))}
      </div>
      <Handle type="source" position={Position.Right} className="production-route-flow-card__handle" />
    </div>
  )
}

function NodeTags({ data }: { data: ProductionFlowNode['data'] }) {
  const color = data.statusColor || 'blue'
  const handleAppend = (event: MouseEvent<HTMLElement>) => {
    event.stopPropagation()
    if (!data.originalUuid || !data.outputKey) return
    data.onConfigureRoute?.({ mode: 'append', originalUuid: data.originalUuid, outputKey: data.outputKey })
  }
  const handleReplace = (event: MouseEvent<HTMLElement>) => {
    event.stopPropagation()
    if (!data.originalUuid || !data.outputKey) return
    data.onConfigureRoute?.({ mode: 'replace', originalUuid: data.originalUuid, outputKey: data.outputKey })
  }
  return (
    <span className="production-route-flow-card__tags">
      {data.stageText && <Tag color={color}>{data.stageText}</Tag>}
      {data.statusText && <Tag color={color}>{data.statusText}</Tag>}
      {data.appendable && data.originalUuid && data.outputKey && (
        <MesTooltip title="从这个产物继续配置下一道工艺">
          <Button
            aria-label="追加下一道工艺"
            className="production-route-flow-card__action nodrag nopan"
            type="text"
            size="small"
            icon={<BranchesOutlined />}
            onClick={handleAppend}
          />
        </MesTooltip>
      )}
      {data.reconfigurable && data.originalUuid && data.outputKey && (
        <MesTooltip title="该产物已有下道工艺，点击重配当前母卷路线并定位到此产物">
          <Button
            aria-label="重配当前母卷路线"
            className="production-route-flow-card__action nodrag nopan"
            type="text"
            size="small"
            icon={<SyncOutlined />}
            onClick={handleReplace}
          />
        </MesTooltip>
      )}
    </span>
  )
}
