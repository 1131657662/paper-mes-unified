import { Button, Empty, Popconfirm, Space, Table, Tag } from 'antd'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { ProcessOrderDetailVO, ProcessStep } from '../../../types/processOrder'
import { STEP_TYPE } from '../../../constants/processOrder'
import MesTooltip from '../../../components/biz/MesTooltip'
import { dict } from '../../../components/processOrder/shared/detailHelpers'
import { formatMoney, formatNumber } from '../orderDetailUtils'
import type { ProcessRouteConfigTarget } from '../routeConfigTypes'

interface Props {
  canManageOrder: boolean
  detail?: ProcessOrderDetailVO
  onAdd: () => void
  onConfigureRoute: (target: ProcessRouteConfigTarget) => void
  onEdit: (step: ProcessStep) => void
  onDelete: (stepUuid: string) => void
}

export default function StepTableSection({ canManageOrder, detail, onAdd, onConfigureRoute, onEdit, onDelete }: Props) {
  const status = detail?.order.orderStatus
  const canEditPending = canManageOrder && status === 1
  const canAppendRoute = canManageOrder && (status === 1 || status === 3)
  const columns = buildColumns({ canManageOrder, detail, onEdit, onDelete })

  return (
    <section className="order-detail-section">
      <div className="order-detail-section__header">
        <h2 className="order-detail-section__title">工序与费用</h2>
        {(canEditPending || canAppendRoute) && (
          <Space size={8}>
            {canEditPending && (
              <Button size="small" onClick={() => onConfigureRoute({ mode: 'replace' })}>
                重配整卷工艺
              </Button>
            )}
            {canAppendRoute && (
              <Button size="small" type="primary" onClick={() => onConfigureRoute({ mode: 'append' })}>
                选择产物追加工艺
              </Button>
            )}
            {canEditPending && (
              <Button size="small" icon={<PlusOutlined />} onClick={onAdd}>
                新增费用工序
              </Button>
            )}
          </Space>
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
  canManageOrder: boolean
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
    { title: '阶段/输入', width: 110, render: (_, step) => routeStageLabel(step) },
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
  canManageOrder: boolean
  detail?: ProcessOrderDetailVO
  step: ProcessStep
  onEdit: (step: ProcessStep) => void
  onDelete: (stepUuid: string) => void
}) {
  if (!options.canManageOrder || options.detail?.order.orderStatus !== 1 || options.step.isMain === 1) return null

  return (
    <Space size={4}>
      <MesTooltip title="编辑工序">
        <Button
          aria-label="编辑工序"
          type="text"
          size="small"
          icon={<EditOutlined />}
          onClick={() => options.onEdit(options.step)}
        />
      </MesTooltip>
      <Popconfirm title="确认删除该工序？" onConfirm={() => options.onDelete(options.step.uuid)}>
        <MesTooltip title="删除工序">
          <Button danger aria-label="删除工序" type="text" size="small" icon={<DeleteOutlined />} />
        </MesTooltip>
      </Popconfirm>
    </Space>
  )
}

function rollLabel(detail: ProcessOrderDetailVO | undefined, step: ProcessStep): string {
  const roll = detail?.originalRolls?.find((item) => item.uuid === step.originalUuid)
  return roll?.rollNo || roll?.extraNo || roll?.paperName || step.originalUuid || '-'
}

function routeStageLabel(step: ProcessStep): string {
  const stage = step.stageLevel ?? 1
  const input = step.inputType === 2 ? '阶段产出' : '原纸'
  return `第${stage}道 / ${input}`
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
