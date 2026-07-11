import { useEffect, useMemo, useState } from 'react'
import { Button, Card, Empty, Select, Space, Tag, Typography, message } from 'antd'
import type { Machine } from '../../types/machine'
import type { OriginalRoll } from '../../types/processOrder'
import ProcessPlanEditor from '../processOrderCreate/components/ProcessPlanEditor'
import type { DefaultPlanOptions } from '../processOrderCreate/draftMappers'
import { STEP_TYPE_REWIND, STEP_TYPE_SAW, sourceRollFromOutput } from '../processOrderDetail/routeConfigDetail'
import type { DetailRoutePriceDefaults } from '../processOrderDetail/routeConfigDetail'
import { formatGram, formatKg, formatMm } from '../../utils/numberFormatters'
import {
  ORIGINAL_OUTPUT_KEY,
  draftStageForSource,
  removeStageAndAfter,
  sourceRowsForRoute,
  stageForSourceKey,
  upsertRouteStage,
} from './routeDraftModel'
import type { RouteDraftStage } from './routeDraftModel'
import '../processOrderCreate/components/CreateOrderEditors.css'

interface Props {
  defaultPlanOptions: DefaultPlanOptions
  machines: Machine[]
  onChange: (stages: RouteDraftStage[]) => void
  prices: DetailRoutePriceDefaults
  quickAction?: RouteQuickAction
  roll: OriginalRoll
  selectedKey: string
  stages: RouteDraftStage[]
}

export interface RouteQuickAction {
  nonce: number
  sourceKey: string
  stepType: number
}

const stepOptions = [
  { label: '锯纸', value: STEP_TYPE_SAW },
  { label: '复卷', value: STEP_TYPE_REWIND },
]

export default function RouteDraftStagePanel({
  defaultPlanOptions,
  machines,
  onChange,
  prices,
  quickAction,
  roll,
  selectedKey,
  stages,
}: Props) {
  const routeSources = useMemo(() => sourceRowsForRoute(roll, stages), [roll, stages])
  const source = routeSources.find((row) => row.outputKey === selectedKey)
  const existingStage = stageForSourceKey(stages, selectedKey)
  const [draftStage, setDraftStage] = useState<RouteDraftStage>()

  useEffect(() => {
    setDraftStage(existingStage)
  }, [existingStage, selectedKey])

  useEffect(() => {
    if (!source || existingStage || quickAction?.sourceKey !== selectedKey) return
    setDraftStage(draftStageForSource(source, quickAction.stepType, prices))
  }, [existingStage, prices, quickAction?.nonce, quickAction?.sourceKey, quickAction?.stepType, selectedKey, source])

  if (!source) return <Empty description="请选择母卷或阶段产物" />

  const startStage = (stepType: number) => setDraftStage(draftStageForSource(source, stepType, prices))
  const changeType = (stepType: number) => setDraftStage(draftStageForSource(source, stepType, prices))
  const applyStage = () => {
    if (!draftStage) return
    onChange(upsertRouteStage(stages, draftStage))
    message.success(existingStage ? '当前工艺已更新' : '下一道工艺已加入路线')
  }
  const deleteStage = () => {
    if (!existingStage) {
      setDraftStage(undefined)
      return
    }
    onChange(removeStageAndAfter(stages, existingStage.id))
    setDraftStage(undefined)
    message.success('已删除该工艺及其后续路线')
  }

  return (
    <Space className="route-draft-stage-panel" direction="vertical" size={12}>
      <SourceSummary source={source} />
      {!draftStage ? (
        <Card size="small" className="route-draft-empty-editor">
          <Typography.Text type="secondary">
            {selectedKey === ORIGINAL_OUTPUT_KEY ? '选择首道工艺后再填写加工参数。' : '选择下一道工艺后，再为当前产物填写加工参数。'}
          </Typography.Text>
          <Space wrap>
            <Button onClick={() => startStage(STEP_TYPE_SAW)}>配置锯纸</Button>
            <Button type="primary" onClick={() => startStage(STEP_TYPE_REWIND)}>配置复卷</Button>
          </Space>
        </Card>
      ) : (
        <Card
          size="small"
          title={<StageTitle sourceKey={selectedKey} stage={draftStage} saved={Boolean(existingStage)} />}
          extra={<Button danger size="small" onClick={deleteStage}>{existingStage ? '删除此段及后续' : '取消'}</Button>}
        >
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Space wrap>
              <Typography.Text strong>工艺类型</Typography.Text>
              <Select value={draftStage.stepType} options={stepOptions} style={{ width: 120 }} onChange={changeType} />
            </Space>
            <ProcessPlanEditor
              defaultPlanOptions={defaultPlanOptions}
              machines={machines}
              plan={draftStage.plan}
              roll={sourceRollFromOutput(source)}
              rolls={[sourceRollFromOutput(source)]}
              onChange={(plan) => setDraftStage({ ...draftStage, plan, stepType: plan.mainStepType ?? draftStage.stepType })}
            />
            <div className="route-draft-editor-footer">
              <Button onClick={deleteStage}>{existingStage ? '撤销修改' : '取消配置'}</Button>
              <Button type="primary" onClick={applyStage}>{existingStage ? '更新到路线' : '加入路线'}</Button>
            </div>
          </Space>
        </Card>
      )}
    </Space>
  )
}

function SourceSummary({ source }: { source: ReturnType<typeof sourceRowsForRoute>[number] }) {
  return (
    <Card size="small" className="route-draft-source-summary">
      <Typography.Text strong>{source.outputKey === ORIGINAL_OUTPUT_KEY ? '母卷' : source.outputKey}</Typography.Text>
      <span>{source.paperName || '-'} / {formatGram(source.gramWeight)} / {formatMm(source.finishWidth)}</span>
      <span>预估重量 {formatKg(source.estimateWeight)}</span>
    </Card>
  )
}

function StageTitle({ saved, sourceKey, stage }: { saved: boolean; sourceKey: string; stage: RouteDraftStage }) {
  return (
    <Space size={8}>
      <span>{sourceKey === ORIGINAL_OUTPUT_KEY ? '首道工艺' : '下一道工艺'}</span>
      <Tag color={stage.stepType === STEP_TYPE_SAW ? 'orange' : 'cyan'}>{stage.stepType === STEP_TYPE_SAW ? '锯纸' : '复卷'}</Tag>
      <Tag color={saved ? 'green' : 'blue'}>{saved ? '已在路线中' : '待加入'}</Tag>
    </Space>
  )
}
