import type {
  ProcessOrderDetailVO,
  ProcessStep,
} from '../../../types/processOrder'
import { useExportProcessOrder } from '../hooks/useExportProcessOrder'
import type { ProcessRouteConfigTarget } from '../routeConfigTypes'
import OrderDetailContent from './OrderDetailContent'
import OrderDetailRemarkModals from './OrderDetailRemarkModals'
import { useOrderRemarkEditor, useRollRemarkEditor } from './useOrderDetailRemarkEditors'
import './OrderDetailView.css'
import './OrderDetailOverview.css'

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
  const exportMutation = useExportProcessOrder()
  const orderRemark = useOrderRemarkEditor(detail)
  const rollRemark = useRollRemarkEditor(detail)

  const handleExport = async () => {
    if (!detail?.order.uuid) return
    await exportMutation.mutateAsync({
      documentNo: detail.order.orderNo,
      uuid: detail.order.uuid,
    })
  }

  return (
    <div className={`order-detail-scroll order-detail-scroll--${mode}`}>
      <OrderDetailContent
        detail={detail}
        mode={mode}
        actions={{
          exporting: exportMutation.isPending, onAddStep, onBack, onConfigureRoute, onDeleteStep,
          onEditOrderRemark: orderRemark.show,
          onEditRollRemark: rollRemark.show,
          onEditStep, onExport: handleExport,
        }}
      />
      <OrderDetailRemarkModals detail={detail} orderEditor={orderRemark} rollEditor={rollRemark} />
    </div>
  )
}
