import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import OrderRemarkModal from './OrderRemarkModal'
import RollRemarkModal from './RollRemarkModal'
import type { OrderRemarkEditor, RollRemarkEditor } from './useOrderDetailRemarkEditors'

interface Props {
  detail?: ProcessOrderDetailVO
  orderEditor: OrderRemarkEditor
  rollEditor: RollRemarkEditor
}

export default function OrderDetailRemarkModals({ detail, orderEditor, rollEditor }: Props) {
  return (
    <>
      <OrderRemarkModal
        loading={orderEditor.loading}
        open={orderEditor.open}
        order={detail?.order}
        onCancel={orderEditor.close}
        onSubmit={orderEditor.submit}
      />
      <RollRemarkModal
        loading={rollEditor.loading}
        open={Boolean(rollEditor.roll)}
        roll={rollEditor.roll}
        onCancel={rollEditor.close}
        onSubmit={rollEditor.submit}
      />
    </>
  )
}
