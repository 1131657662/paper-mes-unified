import type { ProcessOrderDetailVO, ProcessStep } from '../../../types/processOrder'
import OrderDetailHeader from './OrderDetailHeader'
import OrderExecutionHost from './OrderExecutionHost'
import OrderInfoSection from './OrderInfoSection'
import OrderMetricStrip from './OrderMetricStrip'
import OrderStatusProgress from './OrderStatusProgress'
import ProductionTree from './ProductionTree'
import StepTableSection from './StepTableSection'
import './OrderDetailView.css'

interface Props {
  detail?: ProcessOrderDetailVO
  mode?: 'page' | 'drawer'
  onBack?: () => void
  onAddStep: () => void
  onEditStep: (step: ProcessStep) => void
  onDeleteStep: (stepUuid: string) => void
}

export default function OrderDetailView({
  detail,
  mode = 'page',
  onBack,
  onAddStep,
  onEditStep,
  onDeleteStep,
}: Props) {
  return (
    <div className={`order-detail-scroll order-detail-scroll--${mode}`}>
      <div className={`order-detail-view order-detail-view--${mode}`}>
        <OrderDetailHeader order={detail?.order} onBack={onBack} />
        <OrderStatusProgress order={detail?.order} />
        <OrderExecutionHost detail={detail} />
        <OrderMetricStrip detail={detail} />
        <OrderInfoSection detail={detail} />
        <ProductionTree productions={detail?.rollProductions} />
        <StepTableSection
          detail={detail}
          onAdd={onAddStep}
          onEdit={onEditStep}
          onDelete={onDeleteStep}
        />
      </div>
    </div>
  )
}
