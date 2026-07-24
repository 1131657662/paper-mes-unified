import { Alert, Empty, Spin, Tag, Typography } from 'antd'
import { STEP_TYPE } from '../../../constants/processOrder'
import type { ProcessStep } from '../../../types/processOrder'
import { formatGram, formatKg, formatMm } from '../../../utils/numberFormatters'
import type { RollDraft } from '../types'
import { formatMoney } from '../../processOrderDetail/orderDetailUtils'
import './ServiceOnlyPreviewPanel.css'
import type { ServiceEditorStatus } from '../serviceStepEditorTypes'

interface Props {
  loading: boolean
  roll?: RollDraft
  steps: ProcessStep[]
  pending?: ServiceEditorStatus
}

export default function ServiceOnlyPreviewPanel({ loading, pending, roll, steps }: Props) {
  if (!roll) return <Empty description="请选择母卷" />

  return (
    <div className="service-only-preview">
      <div className="service-only-preview__identity">
        <Typography.Text strong>{roll.paperName || '未命名原纸'}</Typography.Text>
        <Typography.Text type="secondary">
          卷号：{roll.rollNo || '-'} / 编号：{roll.extraNo || '-'}
        </Typography.Text>
        <Typography.Text type="secondary">
          {formatGram(roll.gramWeight)} / {formatMm(roll.originalWidth)} / {formatKg(totalWeight(roll))}
        </Typography.Text>
      </div>
      {pending?.dirty && <PendingState status={pending} />}
      <Spin spinning={loading}>
        {steps.length ? <ConfiguredState steps={steps} roll={roll} /> : <EmptyState />}
      </Spin>
    </div>
  )
}

function PendingState({ status }: { status: ServiceEditorStatus }) {
  const scope = `新增 ${status.analysis.createCount} 卷，更新 ${status.analysis.updateCount} 卷`
  return (
    <Alert
      type="warning"
      showIcon
      message="待应用配置尚未保存"
      description={`${status.previousSummary ? `${status.previousSummary} → ` : ''}${status.summary}；${scope}`}
    />
  )
}

function ConfiguredState({ steps, roll }: { steps: ProcessStep[]; roll: RollDraft }) {
  return (
    <div className="service-only-preview__content">
      <Alert
        type="success"
        showIcon
        message={`已配置 ${steps.length} 项附加工艺`}
        description={`保持 ${formatMm(roll.originalWidth)} 原门幅和原纸规格，回录时只记录整理后的实际重量。`}
      />
      <div className="service-only-preview__steps">
        {steps.map((step) => (
          <div className="service-only-preview__step" key={step.uuid}>
            <div>
              <Typography.Text strong>{step.stepName || STEP_TYPE[step.stepType ?? 0] || '附加工艺'}</Typography.Text>
              <Typography.Text type="secondary">{step.machineNameSnap || '未指定机台'}</Typography.Text>
            </div>
            <Tag color="blue">{pricingText(step)}</Tag>
          </div>
        ))}
      </div>
      <Typography.Text type="secondary" className="service-only-preview__hint">
        这类母卷不生成锯纸或复卷排布数据，提交后进入车间整理/包装流程。
      </Typography.Text>
    </div>
  )
}

function EmptyState() {
  return (
    <div className="service-only-preview__empty">
      <Alert
        type="warning"
        showIcon
        message="尚未配置附加工艺"
        description="请在中栏选择剥损整理或重新包装，并保存当前卷，或直接应用到左侧选中的仅附加工艺母卷。"
      />
    </div>
  )
}

function pricingText(step: ProcessStep) {
  if (step.billingMode === 4) return '本单免收'
  if (step.billingMode === 3) return `固定 ${formatMoney(step.billingAmount)}`
  const basis = step.billingBasis === 'TON' ? '按吨' : '按件'
  const unitPrice = step.unitPrice ?? step.billingUnitPrice
  if (unitPrice == null) return `${basis} · 待定价`
  return `${basis} · ${formatMoney(unitPrice)}`
}

function totalWeight(roll: RollDraft) {
  return Number(roll.rollWeight ?? 0) * Number(roll.pieceNum ?? 1)
}
