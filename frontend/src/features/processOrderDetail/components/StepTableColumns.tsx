import { CalculatorOutlined, DeleteOutlined, EditOutlined, SettingOutlined } from '@ant-design/icons'
import { Button, Popconfirm, Space, Tag, Typography } from 'antd'
import type { ColumnsType, ColumnType } from 'antd/es/table'
import MesTooltip from '../../../components/biz/MesTooltip'
import { dict } from '../../../components/processOrder/shared/detailHelpers'
import { STEP_TYPE } from '../../../constants/processOrder'
import type { ProcessOrderDetailVO, ProcessStep } from '../../../types/processOrder'
import type { ProcessRouteConfigTarget } from '../routeConfigTypes'
import { renderAdjustmentCell, renderAmountCell, renderPricingBasisCell, renderProcessingQuantity, renderRollCell, renderUnitPriceCell } from './StepTableCells'
import RollFilterDropdown from './RollFilterDropdown'

interface StepTableColumnOptions {
  canManageOrder: boolean
  canAdjustPricing: boolean
  detail?: ProcessOrderDetailVO
  onEdit: (step: ProcessStep) => void
  onDelete: (stepUuid: string) => void
  onAdjustPricing: (step: ProcessStep) => void
  onConfigureRoute: (target: ProcessRouteConfigTarget) => void
}

export function buildStepTableColumns(options: StepTableColumnOptions): ColumnsType<ProcessStep> {
  return [
    stepColumn(),
    rollColumn(options.detail),
    { title: '加工量', width: 88, align: 'right', render: (_, step) => renderProcessingQuantity(step) },
    { title: '计价方式 / 数量', width: 150, render: (_, step) => renderPricingBasisCell(step) },
    { title: '单价', dataIndex: 'unitPrice', width: 104, align: 'right', render: (_, step) => renderUnitPriceCell(step) },
    { title: '计价调整', dataIndex: 'pricingAdjustmentAmount', width: 112, align: 'right', render: (_, step) => renderAdjustmentCell(step) },
    { title: '费用', dataIndex: 'stepAmount', width: 118, align: 'right', render: (_, step) => renderAmountCell(step) },
    actionColumn(options),
  ]
}

export function stepMatchesRollFilter(value: boolean | React.Key, step: ProcessStep): boolean {
  return step.originalUuid === String(value)
}

function stepColumn(): ColumnType<ProcessStep> {
  return {
    title: '工序', dataIndex: 'stepType', width: 144,
    render: (value, step) => <div className="order-detail-step-name-cell">
      <strong>{step.stepName || dict(STEP_TYPE, value)}</strong>
      <Space size={4}>
        <Tag color={step.stepType === 2 ? 'blue' : 'green'}>{dict(STEP_TYPE, value)}</Tag>
        {step.isMain === 1 && <Tag color="processing">主工艺</Tag>}
      </Space>
      <span className="order-detail-step-stage">{routeStageLabel(step)}</span>
    </div>,
  }
}

function rollColumn(detail?: ProcessOrderDetailVO): ColumnType<ProcessStep> {
  return {
    title: '所属母卷',
    width: 220,
    filterDropdown: (props) => <RollFilterDropdown {...props} rolls={detail?.originalRolls ?? []} />,
    filterMultiple: true,
    onFilter: stepMatchesRollFilter,
    render: (_, step) => renderRollCell(detail, step),
  }
}

function actionColumn(options: StepTableColumnOptions): ColumnType<ProcessStep> {
  return {
    title: '操作',
    width: 64,
    minWidth: 64,
    className: 'order-detail-step-actions-cell',
    fixed: 'right',
    render: (_, step) => renderActionCell(options, step),
  }
}

function renderActionCell(options: StepTableColumnOptions, step: ProcessStep) {
  const canEdit = options.canManageOrder && options.detail?.order.orderStatus === 1
  if (canEdit && step.isMain === 1) return renderRouteAction(options, step)
  const editActions = renderEditActions(options, step, canEdit)
  if (editActions) return editActions
  const canAdjust = options.canAdjustPricing && [3, 4].includes(options.detail?.order.orderStatus ?? 0)
  if (!canAdjust) return <Typography.Text type="secondary">-</Typography.Text>
  return (
    <MesTooltip title="计价核定">
      <Button aria-label="计价核定" type="text" size="small" icon={<CalculatorOutlined />}
        onClick={() => options.onAdjustPricing(step)} />
    </MesTooltip>
  )
}

function renderRouteAction(options: StepTableColumnOptions, step: ProcessStep) {
  return (
    <MesTooltip title="调整本卷工艺">
      <Button aria-label="调整本卷工艺" type="text" size="small" icon={<SettingOutlined />}
        onClick={() => options.onConfigureRoute({ mode: 'replace', originalUuid: step.originalUuid })} />
    </MesTooltip>
  )
}

function renderEditActions(options: StepTableColumnOptions, step: ProcessStep, canEdit: boolean) {
  if (!canEdit || step.isMain === 1) return null
  return (
    <Space size={4}>
      <MesTooltip title="编辑工序">
        <Button aria-label="编辑工序" type="text" size="small" icon={<EditOutlined />}
          onClick={() => options.onEdit(step)} />
      </MesTooltip>
      <Popconfirm title="确认删除该工序？" onConfirm={() => options.onDelete(step.uuid)}>
        <MesTooltip title="删除工序">
          <Button danger aria-label="删除工序" type="text" size="small" icon={<DeleteOutlined />} />
        </MesTooltip>
      </Popconfirm>
    </Space>
  )
}

function routeStageLabel(step: ProcessStep): string {
  const stage = step.stageLevel ?? 1
  const input = step.inputType === 2 ? '阶段产出' : '原纸'
  return `第${stage}道 / ${input}`
}
