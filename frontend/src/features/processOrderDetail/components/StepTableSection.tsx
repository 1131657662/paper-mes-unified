import { useState, type MouseEvent } from 'react'
import { Button, Empty, Space, Table, Typography } from 'antd'
import { CalculatorOutlined, PlusOutlined } from '@ant-design/icons'
import MesTooltip from '../../../components/biz/MesTooltip'
import type { ProcessOrderDetailVO, ProcessStep } from '../../../types/processOrder'
import type { ProcessRouteConfigTarget } from '../routeConfigTypes'
import { buildStepTableColumns } from './StepTableColumns'
import ProcessStepPricingBatchDrawer from './ProcessStepPricingBatchDrawer'
import { pricingSteps, type PricingStep } from '../pricingBatchModel'
import './StepTableSection.css'

interface Props {
  canManageOrder: boolean
  canAdjustPricing: boolean
  detail?: ProcessOrderDetailVO
  onAdd: () => void
  onConfigureRoute: (target: ProcessRouteConfigTarget) => void
  onEdit: (step: ProcessStep) => void
  onDelete: (stepUuid: string) => void
  onAdjustPricing: (step: ProcessStep) => void
}

export default function StepTableSection({ canManageOrder, canAdjustPricing, detail, onAdd, onConfigureRoute, onEdit, onDelete, onAdjustPricing }: Props) {
  const [selectedKeys, setSelectedKeys] = useState<React.Key[]>([])
  const [pricingOpen, setPricingOpen] = useState(false)
  const status = detail?.order.orderStatus
  const canEditPending = canManageOrder && status === 1
  const canAppendRoute = canManageOrder && (status === 1 || status === 3)
  const canPrice = canAdjustPricing && (status === 3 || status === 4)
  const availableSteps = pricingSteps(detail?.steps)
  const selectedSteps = availableSteps.filter((step) => selectedKeys.includes(step.uuid))
  const columns = buildStepTableColumns({
    canManageOrder, canAdjustPricing, detail, onEdit, onDelete, onAdjustPricing, onConfigureRoute,
  })

  return (
    <section className="order-detail-section">
      <div className="order-detail-section__header">
        <h2 className="order-detail-section__title">工序与费用</h2>
        {(canEditPending || canAppendRoute || canPrice) && (
          <Space size={8}>
            {canPrice && <PricingActions availableCount={availableSteps.length} selectedSteps={selectedSteps}
              onOpenAll={() => { setSelectedKeys(availableSteps.map((step) => step.uuid)); setPricingOpen(true) }}
              onOpenSelected={() => setPricingOpen(true)} />}
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
          className="order-detail-step-table"
          rowKey="uuid"
          size="small"
          columns={columns}
          dataSource={detail?.steps ?? []}
          pagination={false}
          rowSelection={canPrice ? {
            fixed: true,
            selectedRowKeys: selectedKeys,
            onChange: setSelectedKeys,
          } : undefined}
          onRow={canPrice ? (step) => ({ onClick: (event) => toggleRowSelection(event, step, selectedKeys, setSelectedKeys) }) : undefined}
          scroll={{ x: 'max-content' }}
          locale={{ emptyText: <Empty description="暂无工序" /> }}
        />
      </div>
      {detail && <ProcessStepPricingBatchDrawer key={selectedSteps.map((step) => step.uuid).join(':')}
        detail={detail} open={pricingOpen} selectedSteps={selectedSteps}
        onApplied={() => { setPricingOpen(false); setSelectedKeys([]) }} onClose={() => setPricingOpen(false)} />}
    </section>
  )
}

function PricingActions({ availableCount, selectedSteps, onOpenAll, onOpenSelected }: {
  availableCount: number
  selectedSteps: PricingStep[]
  onOpenAll: () => void
  onOpenSelected: () => void
}) {
  const allReason = availableCount ? undefined : '当前状态下暂无可核定工序'
  const selectedReason = allReason ?? (selectedSteps.length ? undefined : '请先在表格中勾选需要核定的工序')
  return <Space size={8} className="step-pricing-actions">
    {selectedSteps.length > 0 && <Typography.Text type="secondary">已选 {selectedSteps.length} 道</Typography.Text>}
    <MesTooltip title={allReason}>
      <span className="step-pricing-action-tooltip" title={allReason}>
        <Button size="small" icon={<CalculatorOutlined />} disabled={Boolean(allReason)} onClick={onOpenAll}>
          核定全部费用
        </Button>
      </span>
    </MesTooltip>
    <MesTooltip title={selectedReason}>
      <span className="step-pricing-action-tooltip" title={selectedReason}>
        <Button size="small" type="primary" disabled={Boolean(selectedReason)} onClick={onOpenSelected}>
          批量核定{selectedSteps.length ? `（${selectedSteps.length}）` : ''}
        </Button>
      </span>
    </MesTooltip>
  </Space>
}

function toggleRowSelection(event: MouseEvent<HTMLElement>, step: ProcessStep, keys: React.Key[],
                            setKeys: (keys: React.Key[]) => void) {
  if (isInteractiveTarget(event.target)) return
  setKeys(keys.includes(step.uuid) ? keys.filter((key) => key !== step.uuid) : [...keys, step.uuid])
}

function isInteractiveTarget(target: EventTarget | null): boolean {
  return target instanceof HTMLElement && Boolean(target.closest('button, input, a, [role="checkbox"], .ant-table-filter-trigger'))
}
