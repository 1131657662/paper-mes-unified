import { Alert, Card } from 'antd'
import type { Ref } from 'react'
import type { DeliveryCustomerRevisionPreview } from '../../features/deliveryCustomerSpec/deliveryCustomerSpecTypes'
import type { DeliveryDetailVO } from '../../types/delivery'
import DeliveryPrintOrderSummary from './DeliveryPrintOrderSummary'
import type { DeliveryPrintProjection } from './deliveryPrintProjection'
import DeliveryPrintSheet from './DeliveryPrintSheet'

interface Props {
  ref?: Ref<HTMLDivElement>
  detail: DeliveryDetailVO
  customerSpecs?: DeliveryCustomerRevisionPreview
  projection: DeliveryPrintProjection
}

export default function DeliveryPrintPreviewCard({ ref, detail, customerSpecs, projection }: Props) {
  return (
    <Card
      ref={ref}
      className="document-module-card document-module-card--print"
      title="司机单据预览"
      extra={<DeliveryPrintOrderSummary projection={projection} />}
    >
      {projection.status === 'ready' ? (
        <DeliveryPrintSheet detail={detail} customerSpecs={customerSpecs} projection={projection} />
      ) : (
        <Alert showIcon type="warning" message="司机单据暂不可预览" description={projection.message} />
      )}
    </Card>
  )
}
