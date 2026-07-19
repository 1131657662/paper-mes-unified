import { Button, Tag, Tooltip } from 'antd'
import { CheckOutlined, DeleteOutlined, DownloadOutlined, PrinterOutlined, RollbackOutlined, StopOutlined } from '@ant-design/icons'
import type { DeliveryListActions } from './useDeliveryListActions'

export default function DeliveryListToolbar({ actions }: { actions: DeliveryListActions }) {
  const selected = actions.selected
  return <>
    {actions.selectedCount > 0 && <>
      <Tag color={actions.selectedIgnoredCount > 0 ? 'warning' : 'processing'}>
        已选 {actions.selectedCount} 张 · 可签收 {actions.selectedPendingCount} 张
        {actions.selectedIgnoredCount > 0 ? ` · 忽略 ${actions.selectedIgnoredCount} 张` : ''}
      </Tag>
      <Button icon={<DeleteOutlined />} onClick={actions.clearSelection}>清空选择</Button>
    </>}
    {actions.canManage && <ActionTip title={actions.selectedPendingCount === 0 ? '请至少选择一张待出库单' : undefined}>
      <Button icon={<CheckOutlined />} disabled={actions.selectedPendingCount === 0}
        loading={actions.batchConfirmLoading} onClick={actions.confirmBatch}>批量签收</Button>
    </ActionTip>}
    <ActionTip title={!selected ? '请选择一张出库单后打印' : undefined}>
      <Button icon={<PrinterOutlined />} disabled={!selected} onClick={actions.print}>打印出库单</Button>
    </ActionTip>
    <ActionTip title={!selected ? '请选择一张出库单后导出' : undefined}>
      <Button icon={<DownloadOutlined />} disabled={!selected} loading={actions.exportingSelected}
        onClick={actions.exportSelected}>后台导出</Button>
    </ActionTip>
    <Button icon={<DownloadOutlined />} loading={actions.exportingList} onClick={actions.exportList}>导出对账</Button>
    {actions.canManage && <LifecycleButtons actions={actions} />}
  </>
}

function LifecycleButtons({ actions }: { actions: DeliveryListActions }) {
  const selected = actions.selected
  return <>
    <ActionTip title={!selected || selected.deliveryStatus !== 1 ? '请选择一张待出库单' : undefined}>
      <Button danger icon={<StopOutlined />} disabled={!selected || selected.deliveryStatus !== 1}
        loading={actions.cancelLoading} onClick={actions.cancel}>作废待出库单</Button>
    </ActionTip>
    <ActionTip title={!selected || selected.deliveryStatus !== 2 ? '请选择一张已出库单' : undefined}>
      <Button danger icon={<RollbackOutlined />} disabled={!selected || selected.deliveryStatus !== 2}
        loading={actions.rollingBack} onClick={actions.rollback}>回退出库</Button>
    </ActionTip>
  </>
}

function ActionTip({ children, title }: { children: React.ReactNode; title?: string }) {
  return <Tooltip title={title}><span>{children}</span></Tooltip>
}
