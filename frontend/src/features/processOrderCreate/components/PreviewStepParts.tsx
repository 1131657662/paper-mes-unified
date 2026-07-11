import { Alert, Button, Space, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { PlanPreviewVO, ProcessPlanDTO } from '../../../types/processOrder'
import { formatKg, formatMm } from '../../../utils/numberFormatters'
import type { RollDraft } from '../types'

export interface PreviewStatusDisplay {
  blocking: boolean
  color: string
  detail: string
  label: string
}

export function SubmitBlockerAlert({ count }: { count: number }) {
  return (
    <Alert
      type="warning"
      showIcon
      style={{ marginBottom: 16 }}
      message={`还有 ${count} 卷会阻塞提交`}
      description="只有待预览、需修正这类状态会阻塞提交；直发、已合并使用、链式工艺已校验的母卷不会单独要求配置。"
    />
  )
}

export function PreviewActions({ disabled, submitting, onPrev, onSubmit }: PreviewActionsProps) {
  return (
    <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 16 }}>
      <Space>
        <Button onClick={onPrev}>上一步</Button>
        <Button type="primary" loading={submitting} disabled={disabled} onClick={onSubmit}>
          提交并生成真实卷号
        </Button>
      </Space>
    </div>
  )
}

export function PreviewStatusCell({ status }: { status: PreviewStatusDisplay }) {
  return (
    <Space direction="vertical" size={2}>
      <Tag color={status.color}>{status.label}</Tag>
      <Typography.Text type={status.blocking ? 'danger' : 'secondary'}>{status.detail}</Typography.Text>
    </Space>
  )
}

export function SinglePlanPreview({ plan, preview, roll }: SinglePlanPreviewProps) {
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
        columns={finishColumns}
        dataSource={preview?.finishes ?? []}
      />
    </Space>
  )
}

const finishColumns: ColumnsType<NonNullable<PlanPreviewVO['finishes']>[number]> = [
  { title: '段', dataIndex: 'segmentSort' },
  { title: '门幅', dataIndex: 'finishWidth', render: (value) => formatMm(value ?? 0) },
  { title: '预估重量', dataIndex: 'estimateWeight', render: (value) => formatKg(Number(value ?? 0)) },
  { title: '来源', dataIndex: 'sourceSummary' },
]

interface PreviewActionsProps {
  disabled: boolean
  submitting: boolean
  onPrev: () => void
  onSubmit: () => void
}

interface SinglePlanPreviewProps {
  plan?: ProcessPlanDTO
  preview?: PlanPreviewVO
  roll: RollDraft
}
