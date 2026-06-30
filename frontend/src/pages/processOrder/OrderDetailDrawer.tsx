import { Drawer } from 'antd'
import OrderDetailPanel from '../../features/processOrderDetail/components/OrderDetailPanel'

interface Props {
  uuid: string | null
  open: boolean
  onClose: () => void
}

export default function OrderDetailDrawer({ uuid, open, onClose }: Props) {
  return (
    <Drawer
      title="加工单详情"
      width="86vw"
      open={open}
      onClose={onClose}
      destroyOnHidden
      className="mes-detail-drawer"
    >
      <OrderDetailPanel uuid={uuid} mode="drawer" enabled={open && !!uuid} />
    </Drawer>
  )
}
