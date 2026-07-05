import { Button, Popover, Tag } from 'antd'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import {
  BaseEdge,
  Handle,
  Position,
  getSmoothStepPath,
  type EdgeProps,
  type EdgeTypes,
  type NodeProps,
  type NodeTypes,
} from '@xyflow/react'
import MesTooltip from '../../components/biz/MesTooltip'
import type { RouteDraftEdge, RouteOutputNode, RouteProcessNode } from './routeDraftFlowModel'

export const routeDraftNodeTypes: NodeTypes = {
  processNode: RouteFlowProcessNode,
  routeNode: RouteFlowNode,
}

export const routeDraftEdgeTypes: EdgeTypes = {
  routeEdge: RouteFlowEdge,
}

function RouteFlowNode({ data }: NodeProps<RouteOutputNode>) {
  return (
    <div className={data.selected ? 'route-draft-node route-draft-node--selected' : 'route-draft-node'} onClick={data.onSelect}>
      <Handle type="target" position={Position.Left} className="route-draft-node__handle" />
      <div className="route-draft-node__head">
        <strong>{data.title}</strong>
        <Tag color={data.status === '最终成品' ? 'green' : data.status === '母卷' ? 'purple' : 'blue'}>{data.status}</Tag>
      </div>
      <div className="route-draft-node__body">
        {data.lines.map((line) => <span key={line}>{line}</span>)}
      </div>
      <div className="route-draft-node__actions">
        {data.appendable && <QuickAppendButton onAppend={data.onQuickAppend} />}
        {data.deletable && (
          <MesTooltip title="删除此产物对应工艺及后续路线">
            <Button
              danger
              size="small"
              shape="circle"
              className="nodrag nopan"
              icon={<DeleteOutlined />}
              aria-label="删除路线产物"
              onClick={(event) => { event.stopPropagation(); data.onDelete?.() }}
            />
          </MesTooltip>
        )}
      </div>
      <Handle type="source" position={Position.Right} className="route-draft-node__handle" />
    </div>
  )
}

function RouteFlowProcessNode({ data }: NodeProps<RouteProcessNode>) {
  return (
    <button
      type="button"
      className={data.selected ? 'route-draft-process-node route-draft-process-node--selected' : 'route-draft-process-node'}
      onClick={data.onSelect}
    >
      <Handle type="target" position={Position.Left} className="route-draft-node__handle" />
      <strong>{data.title}</strong>
      <span>{data.caption}</span>
      <Handle type="source" position={Position.Right} className="route-draft-node__handle" />
    </button>
  )
}

function RouteFlowEdge(props: EdgeProps<RouteDraftEdge>) {
  const [edgePath] = getSmoothStepPath(props)
  return <BaseEdge path={edgePath} markerEnd={props.markerEnd} style={props.style} />
}

function QuickAppendButton({ onAppend }: { onAppend?: (stepType: number) => void }) {
  const content = (
    <div className="route-draft-quick-menu">
      <Button size="small" onClick={(event) => { event.stopPropagation(); onAppend?.(1) }}>锯纸</Button>
      <Button size="small" type="primary" onClick={(event) => { event.stopPropagation(); onAppend?.(2) }}>复卷</Button>
    </div>
  )
  return (
    <Popover trigger="hover" content={content} placement="right">
      <Button
        size="small"
        shape="circle"
        className="nodrag nopan"
        icon={<PlusOutlined />}
        aria-label="配置下一道工艺"
        onClick={(event) => event.stopPropagation()}
      />
    </Popover>
  )
}
