import { Button, Space } from 'antd'
import {
  CheckOutlined,
  DownloadOutlined,
  EditOutlined,
  PlusOutlined,
  PrinterOutlined,
  RollbackOutlined,
  StopOutlined,
} from '@ant-design/icons'
import MesPageHeader from '../../components/layout/MesPageHeader'
import type { DeliveryOrder } from '../../types/delivery'
import { DeliveryStatusTag } from './DeliveryDetailSummary'

export interface DeliveryDetailHeaderActions {
  canConfirm: boolean
  canManage: boolean
  cancelling: boolean
  confirming: boolean
  exporting: boolean
  rollingBack: boolean
  onAppend: () => void
  onCancel: () => void
  onConfirm: () => void
  onEdit: () => void
  onExport: () => void
  onPrint: () => void
  onRollback: () => void
}

interface Props {
  actions: DeliveryDetailHeaderActions
  onBack: () => void
  order?: DeliveryOrder
}

export default function DeliveryDetailHeader({ actions, onBack, order }: Props) {
  return (
    <MesPageHeader
      title={order?.deliveryNo ?? '出库单详情'}
      description={order ? `${order.customerName || '-'} · ${order.deliveryDate || '-'}` : undefined}
      onBack={onBack}
      tags={order && <DeliveryStatusTag status={order.deliveryStatus} />}
      actions={order && <HeaderActions actions={actions} status={order.deliveryStatus} />}
    />
  )
}

function HeaderActions({ actions, status }: { actions: DeliveryDetailHeaderActions; status: number }) {
  return (
    <Space wrap>
      {actions.canConfirm && status === 1 && <Button type="primary" icon={<CheckOutlined />}
        loading={actions.confirming} onClick={actions.onConfirm}>确认签收</Button>}
      {actions.canManage && status === 1 && <Button icon={<EditOutlined />}
        onClick={actions.onEdit}>编辑出库信息</Button>}
      {actions.canManage && status === 1 && <Button icon={<PlusOutlined />}
        onClick={actions.onAppend}>添加出库卷</Button>}
      <Button icon={<PrinterOutlined />} onClick={actions.onPrint}>打印预览</Button>
      <Button icon={<DownloadOutlined />} loading={actions.exporting}
        onClick={actions.onExport}>后台导出</Button>
      {actions.canManage && status === 1 && <Button danger icon={<StopOutlined />}
        loading={actions.cancelling} onClick={actions.onCancel}>作废待出库单</Button>}
      {actions.canManage && status === 2 && <Button danger icon={<RollbackOutlined />}
        loading={actions.rollingBack} onClick={actions.onRollback}>回退出库</Button>}
    </Space>
  )
}
