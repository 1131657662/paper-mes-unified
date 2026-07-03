import { Alert, Button, Card, Descriptions, Space, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PROCESS_MODE, STEP_TYPE } from '../../../constants/processOrder'
import type { PlanPreviewVO, ProcessOrderSubmitVO, ProcessPlanDTO } from '../../../types/processOrder'
import type { RollDraft } from '../types'
import { totalWeight } from '../draftMappers'
import { mergedSourceLocks } from '../rewindConsumptionUtils'
import { rollPreviewStatus } from '../previewStatusUtils'
import { formatKg } from '../../../utils/numberFormatters'
import SubmitSuccessPanel from './SubmitSuccessPanel'

interface Props {
  rolls: RollDraft[]
  plans: Record<string, ProcessPlanDTO>
  previews: Record<string, PlanPreviewVO>
  submitting: boolean
  submitResult?: ProcessOrderSubmitVO
  onBackToList: () => void
  onCreateAnother: () => void
  onPrev: () => void
  onSubmit: () => void
  onViewDetail: (orderUuid: string) => void
}

export default function PreviewStep({
  rolls,
  plans,
  previews,
  submitting,
  submitResult,
  onBackToList,
  onCreateAnother,
  onPrev,
  onSubmit,
  onViewDetail,
}: Props) {
  const lockedRolls = mergedSourceLocks(rolls, plans)
  const blockers = rolls
    .map((roll) => rollPreviewStatus({ roll, preview: previews[roll.localId], lock: lockedRolls[roll.localId] }))
    .filter((status) => status.blocking)
  const columns: ColumnsType<RollDraft> = [
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
      width: 280,
      render: (_, roll) => {
        const preview = previews[roll.localId]
        const status = rollPreviewStatus({ roll, preview, lock: lockedRolls[roll.localId] })
        return <PreviewStatusCell status={status} />
      },
    },
  ]

  return (
    <Card title="预览确认">
      <Descriptions bordered size="small" column={3} style={{ marginBottom: 16 }}>
        <Descriptions.Item label="原纸卷数">{rolls.length}</Descriptions.Item>
        <Descriptions.Item label="来料总重">{formatKg(totalWeight(rolls))}</Descriptions.Item>
        <Descriptions.Item label="预计正式号">{estimateFinishCount(previews)}</Descriptions.Item>
      </Descriptions>
      {!submitResult && blockers.length > 0 && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          message={`还有 ${blockers.length} 卷会阻塞提交`}
          description="只有待预览、需修正这类状态会阻塞提交；直发和已合并使用不会单独要求配置。"
        />
      )}
      {submitResult && (
        <SubmitSuccessPanel
          result={submitResult}
          onBackToList={onBackToList}
          onCreateAnother={onCreateAnother}
          onViewDetail={onViewDetail}
        />
      )}
      <Table
        size="small"
        rowKey="localId"
        pagination={false}
        columns={columns}
        dataSource={rolls}
        expandable={{
          expandedRowRender: (roll) => (
            <RollPreview
              roll={roll}
              plan={plans[roll.localId]}
              preview={previews[roll.localId]}
              lock={lockedRolls[roll.localId]}
            />
          ),
        }}
      />
      {!submitResult && (
        <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 16 }}>
          <Space>
            <Button onClick={onPrev}>上一步</Button>
            <Button type="primary" loading={submitting} disabled={blockers.length > 0} onClick={onSubmit}>提交并生成真实卷号</Button>
          </Space>
        </div>
      )}
    </Card>
  )
}

function PreviewStatusCell({ status }: { status: ReturnType<typeof rollPreviewStatus> }) {
  return (
    <Space direction="vertical" size={2}>
      <Tag color={status.color}>{status.label}</Tag>
      <Typography.Text type={status.blocking ? 'danger' : 'secondary'}>{status.detail}</Typography.Text>
    </Space>
  )
}

function RollPreview({ roll, plan, preview, lock }: RollPreviewProps) {
  const status = rollPreviewStatus({ roll, preview, lock })
  if (status.kind === 'direct' || status.kind === 'merged' || status.kind === 'pending') {
    return <Typography.Text type={status.blocking ? 'danger' : 'secondary'}>{status.detail}</Typography.Text>
  }
  return (
    <Space direction="vertical" style={{ width: '100%' }}>
      <Typography.Text>
        方案：{plan?.mainStepType === 1 ? '锯纸' : `复卷模式 ${plan?.rewindMode ?? '-'}`} / 备用号 {plan?.spareCount ?? 0}
      </Typography.Text>
      {preview?.errors?.length ? <Typography.Text type="danger">{preview.errors.join('；')}</Typography.Text> : null}
      <Table
        size="small"
        rowKey={(_, index) => `${roll.localId}-${index}`}
        pagination={false}
        columns={[
          { title: '段', dataIndex: 'segmentSort' },
          { title: '门幅', dataIndex: 'finishWidth', render: (value) => `${value ?? 0}mm` },
          { title: '预估重量', dataIndex: 'estimateWeight', render: (value) => formatKg(Number(value ?? 0)) },
          { title: '来源', dataIndex: 'sourceSummary' },
        ]}
        dataSource={preview?.finishes ?? []}
      />
    </Space>
  )
}

interface RollPreviewProps {
  roll: RollDraft
  plan?: ProcessPlanDTO
  preview?: PlanPreviewVO
  lock?: ReturnType<typeof mergedSourceLocks>[string]
}

function estimateFinishCount(previews: Record<string, PlanPreviewVO>) {
  return Object.values(previews).reduce((sum, preview) => sum + Number(preview.finishCount ?? 0), 0)
}
