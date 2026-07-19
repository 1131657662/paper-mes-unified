import type {
  ProcessOrderDetailVO,
  ProcessStep,
} from '../../../types/processOrder'
import { message } from 'antd'
import { useCreateProcessOrderExportTask } from '../../exportTask/hooks/useCreateProcessOrderExportTask'
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
  onAdjustPricing: (step: ProcessStep) => void
}

export default function OrderDetailView({
  detail,
  mode = 'page',
  onBack,
  onAddStep,
  onConfigureRoute,
  onEditStep,
  onDeleteStep,
  onAdjustPricing,
}: Props) {
  const exportMutation = useCreateProcessOrderExportTask()
  const orderRemark = useOrderRemarkEditor(detail)
  const rollRemark = useRollRemarkEditor(detail)

  const handleExport = async () => {
    if (!detail?.order.uuid) return
    await exportMutation.mutateAsync({ uuid: detail.order.uuid })
    message.success('已加入导出任务，可在右上角下载任务中心查看')
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
          onAdjustPricing,
        }}
      />
      <OrderDetailRemarkModals detail={detail} orderEditor={orderRemark} rollEditor={rollRemark} />
    </div>
  )
}
