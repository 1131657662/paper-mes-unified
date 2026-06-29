import { Drawer } from 'antd'
import BackRecordWorkspace from './backRecord/BackRecordWorkspace'

interface Props {
  uuid: string | null
  open: boolean
  onClose: () => void
  onSuccess: () => void
}

export default function BackRecordDrawer({ uuid, open, onClose, onSuccess }: Props) {
  return (
    <Drawer
      title="回录工作台"
      width="92vw"
      open={open}
      onClose={onClose}
      destroyOnHidden
      className="mes-detail-drawer"
    >
      <BackRecordWorkspace
        uuid={uuid}
        enabled={open}
        mode="drawer"
        onClose={onClose}
        onSuccess={onSuccess}
      />
    </Drawer>
  )
}
