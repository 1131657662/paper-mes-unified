import { Card, Descriptions, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PROCESS_MODE, STEP_TYPE } from '../../../constants/processOrder'
import type {
  PlanPreviewVO,
  ProcessOrderSubmitVO,
  ProcessPlanDTO,
  ProcessRoutePreviewVO,
} from '../../../types/processOrder'
import { formatKg } from '../../../utils/numberFormatters'
import { totalWeight } from '../draftMappers'
import { rollPreviewStatus } from '../previewStatusUtils'
import { mergedSourceLocks } from '../rewindConsumptionUtils'
import type { RollDraft } from '../types'
import {
  PreviewActions,
  PreviewStatusCell,
  SinglePlanPreview,
  SubmitBlockerAlert,
  type PreviewStatusDisplay,
} from './PreviewStepParts'
import RoutePreviewInline, { routeFinalCount, routePreviewSummary } from './RoutePreviewInline'
import SubmitSuccessPanel from './SubmitSuccessPanel'

interface Props {
  rolls: RollDraft[]
  plans: Record<string, ProcessPlanDTO>
  previews: Record<string, PlanPreviewVO>
  routePreviews: Record<string, ProcessRoutePreviewVO>
  submitting: boolean
  submitResult?: ProcessOrderSubmitVO
  onBackToList: () => void
  onCreateAnother: () => void
  onPrev: () => void
  onSubmit: () => void
  onViewDetail: (orderUuid: string) => void
}

export default function PreviewStep(props: Props) {
  const lockedRolls = mergedSourceLocks(props.rolls, props.plans)
  const blockers = blockingStatuses(props, lockedRolls)
  const columns = previewColumns(props, lockedRolls)

  return (
    <Card title="预览确认">
      <Descriptions bordered size="small" column={3} style={{ marginBottom: 16 }}>
        <Descriptions.Item label="原纸卷数">{props.rolls.length}</Descriptions.Item>
        <Descriptions.Item label="来料总重">{formatKg(totalWeight(props.rolls))}</Descriptions.Item>
        <Descriptions.Item label="预计正式号">{estimateFinishCount(props)}</Descriptions.Item>
      </Descriptions>
      {!props.submitResult && blockers.length > 0 && <SubmitBlockerAlert count={blockers.length} />}
      {props.submitResult && <SubmitSuccessPanel
        result={props.submitResult}
        onBackToList={props.onBackToList}
        onCreateAnother={props.onCreateAnother}
        onViewDetail={props.onViewDetail}
      />}
      <Table
        size="small"
        rowKey="localId"
        pagination={false}
        columns={columns}
        dataSource={props.rolls}
        expandable={{ expandedRowRender: (roll) => <RollPreview roll={roll} state={props} locks={lockedRolls} /> }}
      />
      {!props.submitResult && <PreviewActions
        disabled={blockers.length > 0}
        submitting={props.submitting}
        onPrev={props.onPrev}
        onSubmit={props.onSubmit}
      />}
    </Card>
  )
}

function RollPreview({ locks, roll, state }: RollPreviewProps) {
  const routePreview = routePreviewForRoll(roll, state.routePreviews)
  if (routePreview) return <RoutePreviewInline preview={routePreview} />

  const preview = state.previews[roll.localId]
  const status = rollPreviewStatus({ roll, preview, lock: locks[roll.localId] })
  if (status.kind === 'direct' || status.kind === 'merged' || status.kind === 'pending') {
    return <Typography.Text type={status.blocking ? 'danger' : 'secondary'}>{status.detail}</Typography.Text>
  }
  return <SinglePlanPreview roll={roll} plan={state.plans[roll.localId]} preview={preview} />
}

function previewColumns(state: Props, locks: ReturnType<typeof mergedSourceLocks>): ColumnsType<RollDraft> {
  return [
    { title: '母卷', width: 150, render: (_, roll) => roll.rollNo || roll.paperName || '-' },
    { title: '原纸规格', width: 160, render: (_, roll) => `${roll.gramWeight}g / ${roll.originalWidth}mm` },
    { title: '重量', width: 120, render: (_, roll) => formatKg(Number(roll.rollWeight) * (roll.pieceNum ?? 1)) },
    { title: '加工方式', width: 110, render: (_, roll) => <Tag>{PROCESS_MODE[roll.processMode ?? 1]}</Tag> },
    {
      title: '主工艺',
      width: 100,
      render: (_, roll) => (roll.processMode === 3 ? '-' : <Tag color="green">{STEP_TYPE[roll.mainStepType ?? 2]}</Tag>),
    },
    {
      title: '后端预览',
      width: 300,
      render: (_, roll) => <PreviewStatusCell status={previewStatusForRoll({ locks, roll, state })} />,
    },
  ]
}

function previewStatusForRoll({ locks, roll, state }: PreviewStatusOptions): PreviewStatusDisplay {
  const routePreview = routePreviewForRoll(roll, state.routePreviews)
  if (routePreview) {
    return {
      color: 'success',
      label: '链式工艺已校验',
      detail: routePreviewSummary(routePreview),
      blocking: false,
    } as const
  }
  return rollPreviewStatus({ roll, preview: state.previews[roll.localId], lock: locks[roll.localId] })
}

function blockingStatuses(state: Props, locks: ReturnType<typeof mergedSourceLocks>) {
  return state.rolls
    .map((roll) => previewStatusForRoll({ locks, roll, state }))
    .filter((status) => status.blocking)
}

function estimateFinishCount(state: Props) {
  return state.rolls.reduce((sum, roll) => {
    const routePreview = routePreviewForRoll(roll, state.routePreviews)
    if (routePreview) return sum + routeFinalCount(routePreview)
    return sum + Number(state.previews[roll.localId]?.finishCount ?? 0)
  }, 0)
}

function routePreviewForRoll(roll: RollDraft, routePreviews: Record<string, ProcessRoutePreviewVO>) {
  return roll.uuid ? routePreviews[roll.uuid] : undefined
}

interface PreviewStatusOptions {
  locks: ReturnType<typeof mergedSourceLocks>
  roll: RollDraft
  state: Props
}

interface RollPreviewProps {
  locks: ReturnType<typeof mergedSourceLocks>
  roll: RollDraft
  state: Props
}
