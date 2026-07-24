import type { ReactNode } from 'react'
import { Alert, Button, Card, Empty, Space, Spin, Tag, Typography } from 'antd'
import { RedoOutlined, UndoOutlined } from '@ant-design/icons'
import MesTooltip from '../../components/biz/MesTooltip'
import MesPageHeader from '../../components/layout/MesPageHeader'
import RouteDraftFlow from '../../features/processOrderRouteDraft/RouteDraftFlow'
import RouteDraftPreviewResult from '../../features/processOrderRouteDraft/RouteDraftPreviewResult'
import RouteDraftStagePanel, { type RouteQuickAction } from '../../features/processOrderRouteDraft/RouteDraftStagePanel'
import RouteDraftSummaryBar from '../../features/processOrderRouteDraft/RouteDraftSummaryBar'
import { ORIGINAL_OUTPUT_KEY, allRouteOutputs, finalRouteOutputs } from '../../features/processOrderRouteDraft/routeDraftModel'
import type { RouteDraftStage } from '../../features/processOrderRouteDraft/routeDraftModel'
import type { DefaultPlanOptions } from '../../features/processOrderCreate/draftMappers'
import type { DetailRouteOutputRow, DetailRoutePriceDefaults } from '../../features/processOrderDetail/routeConfigDetail'
import type { Machine } from '../../types/machine'
import type { OriginalRoll, ProcessRoutePreviewVO } from '../../types/processOrder'
import { formatGram, formatKg, formatMm } from '../../utils/numberFormatters'

export interface RouteDesignerActionState {
  applyDisabledReason?: string
  canRedo: boolean
  canUndo: boolean
  previewDisabledReason?: string
  saveDisabledReason?: string
}

export interface RouteDesignerCommands {
  onApply: () => void
  onBack: () => void
  onDeleteFrom: (sourceKey: string) => void
  onPreview: () => void
  onQuickAppend: (row: DetailRouteOutputRow, stepType: number) => void
  onRedo: () => void
  onSave: () => void
  onSelect: (key: string) => void
  onStagesChange: (stages: RouteDraftStage[]) => void
  onUndo: () => void
}

interface Props {
  actionState: RouteDesignerActionState
  busy: boolean
  commands: RouteDesignerCommands
  defaultPlanOptions: DefaultPlanOptions
  feedback?: { description: string; title: string }
  machines: Machine[]
  orderLabel: string
  preview?: ProcessRoutePreviewVO
  prices: DetailRoutePriceDefaults
  quickAction?: RouteQuickAction
  roll: OriginalRoll
  selectedOutputKey: string
  stages: RouteDraftStage[]
}

export default function RouteDesignerWorkspace(props: Props) {
  return (
    <Spin spinning={props.busy}>
      <div className="route-draft-page">
        <MesPageHeader
          actions={<RouteDesignerActions commands={props.commands} state={props.actionState} />}
          backText="返回新建单"
          description={`${props.orderLabel} · ${rollLabel(props.roll)}`}
          eyebrow="链式工艺"
          title="工艺路线设计"
          onBack={props.commands.onBack}
        />
        <Alert type="info" showIcon message="多序路线独立配置" description="从母卷或任一阶段产物继续配置下一道工艺；普通单道加工仍在新建单第四步完成。" />
        <section className="route-draft-layout">
          <RouteSourcePanel roll={props.roll} stages={props.stages} selectedKey={props.selectedOutputKey} onSelect={props.commands.onSelect} />
          <Card className="route-draft-canvas-card" title="路线图">
            <RouteDraftFlow roll={props.roll} stages={props.stages} selectedKey={props.selectedOutputKey}
              onDeleteFrom={props.commands.onDeleteFrom} onQuickAppend={props.commands.onQuickAppend} onSelect={props.commands.onSelect} />
          </Card>
          <Card className="route-draft-editor-card" title="工艺参数">
            <RouteDraftStagePanel defaultPlanOptions={props.defaultPlanOptions} machines={props.machines}
              prices={props.prices} quickAction={props.quickAction} roll={props.roll}
              selectedKey={props.selectedOutputKey} stages={props.stages} onChange={props.commands.onStagesChange} />
          </Card>
        </section>
        {props.feedback && <Alert type="error" showIcon message={props.feedback.title} description={props.feedback.description} />}
        {props.preview && <RouteDraftPreviewResult preview={props.preview} />}
        <RouteDraftSummaryBar roll={props.roll} stages={props.stages} preview={props.preview} />
      </div>
    </Spin>
  )
}

export function RouteDesignerActions({ commands, state }: {
  commands: RouteDesignerCommands
  state: RouteDesignerActionState
}) {
  return (
    <Space wrap>
      <Button onClick={commands.onBack}>返回新建单</Button>
      <ActionWithReason reason={state.applyDisabledReason} label="应用到同规格" onClick={commands.onApply} />
      <ActionWithReason reason={!state.canUndo ? '当前没有可撤销的修改' : undefined} label="撤销" icon={<UndoOutlined />} onClick={commands.onUndo} />
      <ActionWithReason reason={!state.canRedo ? '当前没有可重做的修改' : undefined} label="重做" icon={<RedoOutlined />} onClick={commands.onRedo} />
      <ActionWithReason reason={state.previewDisabledReason} label="预览校验" onClick={commands.onPreview} />
      <ActionWithReason primary reason={state.saveDisabledReason} label="保存草稿" onClick={commands.onSave} />
    </Space>
  )
}

function ActionWithReason(props: {
  icon?: ReactNode
  label: string
  onClick: () => void
  primary?: boolean
  reason?: string
}) {
  return (
    <MesTooltip title={props.reason}>
      <span className="route-draft-action-slot">
        <Button aria-label={props.reason ? `${props.label}：${props.reason}` : props.label}
          disabled={Boolean(props.reason)} icon={props.icon} type={props.primary ? 'primary' : 'default'} onClick={props.onClick}>
          {props.label}
        </Button>
      </span>
    </MesTooltip>
  )
}

function RouteSourcePanel({ onSelect, roll, selectedKey, stages }: {
  onSelect: (key: string) => void
  roll: OriginalRoll
  selectedKey?: string
  stages: RouteDraftStage[]
}) {
  const outputs = allRouteOutputs(roll, stages)
  const finals = new Set(finalRouteOutputs(roll, stages).map((row) => row.outputKey))
  return (
    <Card className="route-draft-source-card" title="母卷与产物">
      <button type="button" className={selectedKey === ORIGINAL_OUTPUT_KEY ? 'route-draft-roll route-draft-roll--selected' : 'route-draft-roll'}
        onClick={() => onSelect(ORIGINAL_OUTPUT_KEY)}>
        <Typography.Text strong>{roll.rollNo || roll.extraNo || '未编号母卷'}</Typography.Text>
        <span>{roll.paperName || '-'} / {formatGram(roll.gramWeight)} / {formatMm(roll.originalWidth)}</span>
        <span>来料 {formatKg(Number(roll.totalWeight ?? 0))}</span>
      </button>
      <div className="route-draft-output-list">
        {outputs.length ? outputs.map((row) => (
          <button type="button" key={row.outputKey}
            className={selectedKey === row.outputKey ? 'route-draft-output route-draft-output--selected' : 'route-draft-output'}
            onClick={() => onSelect(row.outputKey)}>
            <span><b>{row.outputKey}</b><Tag color={finals.has(row.outputKey) ? 'green' : 'blue'}>{finals.has(row.outputKey) ? '最终' : '中间'}</Tag></span>
            <small>{row.paperName || '-'} / {formatGram(row.gramWeight)} / {formatMm(row.finishWidth)}</small>
            <em>{formatKg(row.estimateWeight)}</em>
          </button>
        )) : <Empty description="配置首道工艺后生成阶段产物" />}
      </div>
    </Card>
  )
}

function rollLabel(roll: OriginalRoll) {
  return `母卷：${roll.rollNo || '-'} / 编号：${roll.extraNo || '-'} / ${roll.paperName || '-'}`
}
