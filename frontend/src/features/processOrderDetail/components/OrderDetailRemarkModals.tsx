import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import OrderRemarkModal from './OrderRemarkModal'
import PostProductionNoteModal from './PostProductionNoteModal'
import RollRemarkModal from './RollRemarkModal'
import type { OrderRemarkEditor, PostProductionNoteEditor, RollRemarkEditor } from './useOrderDetailRemarkEditors'

interface Props {
  detail?: ProcessOrderDetailVO
  orderEditor: OrderRemarkEditor
  postProductionEditor: PostProductionNoteEditor
  rollEditor: RollRemarkEditor
}

export default function OrderDetailRemarkModals({ detail, orderEditor, postProductionEditor, rollEditor }: Props) {
  return (
    <>
      <OrderRemarkModal
        loading={orderEditor.loading}
        open={orderEditor.open}
        order={detail?.order}
        onCancel={orderEditor.close}
        onSubmit={orderEditor.submit}
      />
      <PostProductionNoteModal
        loading={postProductionEditor.loading}
        open={postProductionEditor.open}
        order={detail?.order}
        onCancel={postProductionEditor.close}
        onSubmit={postProductionEditor.submit}
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
