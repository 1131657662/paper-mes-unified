import { PERMISSIONS } from '../../../constants/permissions'
import { useHasPermission } from '../../../stores/authStore'
import type { ProcessOrderDetailVO, ProcessStep, RollProductionVO } from '../../../types/processOrder'
import type { ProcessRouteConfigTarget } from '../routeConfigTypes'
import OrderDetailHeader from './OrderDetailHeader'
import OrderExecutionHost from './OrderExecutionHost'
import OrderInfoSection from './OrderInfoSection'
import ProductionTree from './ProductionTree'
import StepTableSection from './StepTableSection'

export interface OrderDetailContentActions {
  exporting: boolean
  onAddStep: () => void
  onBack?: () => void
  onConfigureRoute: (target: ProcessRouteConfigTarget) => void
  onDeleteStep: (stepUuid: string) => void
  onEditOrderRemark: () => void
  onEditRollRemark: (roll: RollProductionVO) => void
  onEditStep: (step: ProcessStep) => void
  onAdjustPricing: (step: ProcessStep) => void
  onExport: () => void
}

interface Props {
  actions: OrderDetailContentActions
  detail?: ProcessOrderDetailVO
  mode: 'page' | 'drawer'
}

export default function OrderDetailContent({ actions, detail, mode }: Props) {
  const canManageOrder = useHasPermission(PERMISSIONS.orderManage)
  const canAdjustPricing = useHasPermission(PERMISSIONS.orderPricing)
  const canEditRemark = canManageOrder
    && (detail?.order.orderStatus == null || ![4, 5, 6].includes(detail.order.orderStatus))

  return (
    <div className={`order-detail-view order-detail-view--${mode}`}>
      <OrderDetailHeader exporting={actions.exporting} order={detail?.order} onBack={actions.onBack} onExport={actions.onExport} />
      <OrderExecutionHost detail={detail} />
      <OrderInfoSection canEditRemark={canEditRemark} detail={detail} onEditRemark={actions.onEditOrderRemark} />
      <ProductionTree
        canManageOrder={canManageOrder}
        canEditRemark={canEditRemark}
        orderStatus={detail?.order.orderStatus}
        productions={detail?.rollProductions}
        onConfigureRoute={actions.onConfigureRoute}
        onEditRollRemark={actions.onEditRollRemark}
      />
      <StepTableSection
        canManageOrder={canManageOrder}
        canAdjustPricing={canAdjustPricing}
        detail={detail}
        onAdd={actions.onAddStep}
        onConfigureRoute={actions.onConfigureRoute}
        onEdit={actions.onEditStep}
        onDelete={actions.onDeleteStep}
        onAdjustPricing={actions.onAdjustPricing}
      />
    </div>
  )
}
