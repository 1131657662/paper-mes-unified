import { DeleteOutlined, EditOutlined } from '@ant-design/icons'
import { Button, Popconfirm, Tag } from 'antd'
import type { ProcessStep } from '../../../types/processOrder'
import { formatMoney, formatNumber } from '../../processOrderDetail/orderDetailUtils'

interface Props {
  step: ProcessStep
  onDelete: () => void
  onEdit: () => void
}

export default function DraftServiceStepRow(props: Props) {
  return (
    <div className="draft-service-process-row">
      <div className="draft-service-process-row__main">
        <strong>{props.step.stepName || processName(props.step.stepType)}</strong>
        <span>{pricingText(props.step)}</span>
      </div>
      <Tag color="blue">{basisText(props.step)}</Tag>
      <div className="draft-service-process-row__actions">
        <Button type="text" size="small" icon={<EditOutlined />}
          aria-label="编辑附加工艺" onClick={props.onEdit} />
        <Popconfirm title="删除这条附加工艺？" onConfirm={props.onDelete}>
          <Button type="text" danger size="small" icon={<DeleteOutlined />} aria-label="删除附加工艺" />
        </Popconfirm>
      </div>
    </div>
  )
}

function processName(stepType?: number) {
  return stepType === 3 ? '剥损整理' : stepType === 4 ? '重新包装' : '附加工艺'
}

function basisText(step: ProcessStep) {
  if (step.billingMode === 1 && step.unitPrice == null && step.billingUnitPrice == null) return '待定价'
  if (step.billingMode === 3) return '固定金额'
  if (step.billingMode === 4) return '免费'
  return step.billingBasis === 'PIECE' ? '按件' : '按吨'
}

function pricingText(step: ProcessStep) {
  if (step.billingMode === 1 && step.unitPrice == null && step.billingUnitPrice == null) {
    return `${step.billingBasis === 'TON' ? '按吨' : '按件'} · 数量自动计算 · 费用待核定`
  }
  if (step.billingMode === 3) return `固定 ${formatMoney(step.billingAmount)}`
  if (step.billingMode === 4) return '本单免收'
  const piece = step.billingBasis === 'PIECE'
  const quantity = formatNumber(step.serviceQuantity ?? 0, piece ? 0 : 3)
  return `${quantity} ${piece ? '件' : '吨'} × ${formatMoney(step.unitPrice)}`
}
