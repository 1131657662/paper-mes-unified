import { Button, Empty, Popconfirm, Space, Table, Tag } from 'antd'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { ProcessOrderDetailVO, ProcessStep } from '../../../types/processOrder'
import { STEP_TYPE } from '../../../constants/processOrder'
import { dict } from '../../../components/processOrder/shared/detailHelpers'
import { formatMoney, formatNumber } from '../orderDetailUtils'

interface Props {
  detail?: ProcessOrderDetailVO
  onAdd: () => void
  onEdit: (step: ProcessStep) => void
  onDelete: (stepUuid: string) => void
}

export default function StepTableSection({ detail, onAdd, onEdit, onDelete }: Props) {
  const canAdd = detail?.order.orderStatus === 1
  const columns = buildColumns({ detail, onEdit, onDelete })

  return (
    <section className="order-detail-section">
      <div className="order-detail-section__header">
        <h2 className="order-detail-section__title">工序与费用</h2>
        {canAdd && (
          <Button type="primary" size="small" icon={<PlusOutlined />} onClick={onAdd}>
            新增工序
          </Button>
        )}
      </div>
      <div className="order-detail-section__body order-detail-table-wrap">
        <Table
          rowKey="uuid"
          size="small"
          columns={columns}
          dataSource={detail?.steps ?? []}
          pagination={false}
          scroll={{ x: 'max-content' }}
          locale={{ emptyText: <Empty description="暂无工序" /> }}
        />
      </div>
    </section>
  )
}

function buildColumns(options: {
  detail?: ProcessOrderDetailVO
  onEdit: (step: ProcessStep) => void
  onDelete: (stepUuid: string) => void
}): ColumnsType<ProcessStep> {
  return [
    { title: '序', dataIndex: 'stepSort', width: 56 },
    {
      title: '工艺',
      dataIndex: 'stepType',
      width: 130,
      render: (value, step) => (
        <Space size={4}>
          <Tag color={step.stepType === 2 ? 'blue' : 'green'}>{dict(STEP_TYPE, value)}</Tag>
          {step.isMain === 1 && <Tag color="processing">主工艺</Tag>}
        </Space>
      ),
    },
    { title: '名称', dataIndex: 'stepName', width: 120, render: textOrDash },
    { title: '所属母卷', width: 160, render: (_, step) => rollLabel(options.detail, step) },
    { title: '刀数', dataIndex: 'knifeCount', width: 80, align: 'right', render: numberOrDash },
    { title: '吨位', dataIndex: 'processWeight', width: 100, align: 'right', render: weightOrDash },
    { title: '单价', dataIndex: 'unitPrice', width: 100, align: 'right', render: moneyOrDash },
    { title: '金额', dataIndex: 'stepAmount', width: 110, align: 'right', render: moneyOrDash },
    {
      title: '操作',
      width: 110,
      fixed: 'right',
      render: (_, step) => renderActions({ ...options, step }),
    },
  ]
}

function renderActions(options: {
  detail?: ProcessOrderDetailVO
  step: ProcessStep
  onEdit: (step: ProcessStep) => void
  onDelete: (stepUuid: string) => void
}) {
  if (options.detail?.order.orderStatus !== 1 || options.step.isMain === 1) return null

  return (
    <Space size={4}>
      <Button type="text" size="small" icon={<EditOutlined />} onClick={() => options.onEdit(options.step)} />
      <Popconfirm title="确认删除该工序？" onConfirm={() => options.onDelete(options.step.uuid)}>
        <Button danger type="text" size="small" icon={<DeleteOutlined />} />
      </Popconfirm>
    </Space>
  )
}

function rollLabel(detail: ProcessOrderDetailVO | undefined, step: ProcessStep): string {
  const roll = detail?.originalRolls?.find((item) => item.uuid === step.originalUuid)
  return roll?.rollNo || roll?.extraNo || roll?.paperName || step.originalUuid || '-'
}

function textOrDash(value?: string): string {
  return value || '-'
}

function numberOrDash(value?: number): string {
  return value == null ? '-' : String(value)
}

function weightOrDash(value?: number): string {
  return value == null ? '-' : `${formatNumber(value, 3)} t`
}

function moneyOrDash(value?: number): string {
  return value == null ? '-' : formatMoney(value)
}
