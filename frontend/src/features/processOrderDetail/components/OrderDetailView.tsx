import { message } from 'antd'
import { useState } from 'react'
import type {
  OriginalRollRemarkDTO,
  ProcessOrderDetailVO,
  ProcessOrderRemarkDTO,
  ProcessStep,
  RollProductionVO,
} from '../../../types/processOrder'
import { useExportProcessOrder } from '../hooks/useExportProcessOrder'
import { useUpdateOrderRemark } from '../hooks/useUpdateOrderRemark'
import { useUpdateRollRemark } from '../hooks/useUpdateRollRemark'
import type { ProcessRouteConfigTarget } from '../routeConfigTypes'
import OrderDetailHeader from './OrderDetailHeader'
import OrderExecutionHost from './OrderExecutionHost'
import OrderInfoSection from './OrderInfoSection'
import OrderRemarkModal from './OrderRemarkModal'
import OrderMetricStrip from './OrderMetricStrip'
import OrderStatusProgress from './OrderStatusProgress'
import ProductionTree from './ProductionTree'
import RollRemarkModal from './RollRemarkModal'
import StepTableSection from './StepTableSection'
import './OrderDetailView.css'

interface Props {
  detail?: ProcessOrderDetailVO
  mode?: 'page' | 'drawer'
  onBack?: () => void
  onAddStep: () => void
  onConfigureRoute: (target: ProcessRouteConfigTarget) => void
  onEditStep: (step: ProcessStep) => void
  onDeleteStep: (stepUuid: string) => void
}

export default function OrderDetailView({
  detail,
  mode = 'page',
  onBack,
  onAddStep,
  onConfigureRoute,
  onEditStep,
  onDeleteStep,
}: Props) {
  const [orderRemarkOpen, setOrderRemarkOpen] = useState(false)
  const [editingRollRemark, setEditingRollRemark] = useState<RollProductionVO>()
  const exportMutation = useExportProcessOrder()
  const updateOrderRemarkMutation = useUpdateOrderRemark()
  const updateRollRemarkMutation = useUpdateRollRemark()
  const canEditRemark = canEditRemarks(detail?.order.orderStatus)

  const handleExport = async () => {
    if (!detail?.order.uuid) return
    await exportMutation.mutateAsync({
      documentNo: detail.order.orderNo,
      uuid: detail.order.uuid,
    })
  }

  const handleOrderRemarkSubmit = async (values: ProcessOrderRemarkDTO) => {
    if (!detail?.order.uuid) return
    await updateOrderRemarkMutation.mutateAsync({ orderUuid: detail.order.uuid, values })
    setOrderRemarkOpen(false)
    message.success('主单备注已更新')
  }

  const handleRollRemarkSubmit = async (values: OriginalRollRemarkDTO) => {
    if (!detail?.order.uuid || !editingRollRemark?.originalUuid) return
    await updateRollRemarkMutation.mutateAsync({
      orderUuid: detail.order.uuid,
      rollUuid: editingRollRemark.originalUuid,
      values,
    })
    setEditingRollRemark(undefined)
    message.success('原纸备注已更新')
  }

  return (
    <div className={`order-detail-scroll order-detail-scroll--${mode}`}>
      <div className={`order-detail-view order-detail-view--${mode}`}>
        <OrderDetailHeader
          exporting={exportMutation.isPending}
          order={detail?.order}
          onBack={onBack}
          onExport={handleExport}
        />
        <OrderStatusProgress order={detail?.order} />
        <OrderExecutionHost detail={detail} />
        <OrderMetricStrip detail={detail} />
        <OrderInfoSection
          canEditRemark={canEditRemark}
          detail={detail}
          onEditRemark={() => setOrderRemarkOpen(true)}
        />
        <ProductionTree
          canEditRemark={canEditRemark}
          orderStatus={detail?.order.orderStatus}
          productions={detail?.rollProductions}
          onConfigureRoute={onConfigureRoute}
          onEditRollRemark={setEditingRollRemark}
        />
        <StepTableSection
          detail={detail}
          onAdd={onAddStep}
          onConfigureRoute={onConfigureRoute}
          onEdit={onEditStep}
          onDelete={onDeleteStep}
        />
      </div>
      <OrderRemarkModal
        loading={updateOrderRemarkMutation.isPending}
        open={orderRemarkOpen}
        order={detail?.order}
        onCancel={() => setOrderRemarkOpen(false)}
        onSubmit={handleOrderRemarkSubmit}
      />
      <RollRemarkModal
        loading={updateRollRemarkMutation.isPending}
        open={Boolean(editingRollRemark)}
        roll={editingRollRemark}
        onCancel={() => setEditingRollRemark(undefined)}
        onSubmit={handleRollRemarkSubmit}
      />
    </div>
  )
}

function canEditRemarks(status?: number): boolean {
  return status == null || ![4, 5, 6].includes(status)
}
