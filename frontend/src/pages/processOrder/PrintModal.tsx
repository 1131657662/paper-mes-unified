import { Drawer, Spin } from 'antd'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import { useProcessOrderDetail } from '../../features/processOrderDetail/hooks/useProcessOrderDetail'
import PrintIssueDrawer from '../../features/processOrderDetail/components/PrintIssueDrawer'

interface Props {
  uuid: string | null
  orderNo?: string
  printCount?: number
  open: boolean
  onClose: () => void
  onSuccess: () => void
}

/**
 * 列表页打印入口的兼容壳。实际预览、首次下发、补打和浏览器打印
 * 统一由详情页的 PrintIssueDrawer 处理，避免列表入口只改状态不打印。
 */
export default function PrintModal({ uuid, orderNo, open, onClose, onSuccess }: Props) {
  const detailQuery = useProcessOrderDetail(uuid ?? undefined, { enabled: open })
  const drawerTitle = orderNo ? `加工单打印：${orderNo}` : '加工单打印'

  if (!open || !uuid) return null
  if (detailQuery.isLoading) {
    return (
      <Drawer title={drawerTitle} open onClose={onClose} width="min(1180px, calc(100vw - 32px))">
        <Spin spinning><div className="print-modal-loading" aria-label="加工单打印信息加载中" /></Spin>
      </Drawer>
    )
  }
  if (detailQuery.isError || !detailQuery.data) {
    return (
      <Drawer title={drawerTitle} open onClose={onClose} width="min(1180px, calc(100vw - 32px))">
        <QueryLoadErrorAlert
          message="加工单详情加载失败"
          description="当前不会执行打印或下发，请重新加载后继续。"
          onRetry={() => void detailQuery.refetch()}
        />
      </Drawer>
    )
  }

  return <PrintIssueDrawer detail={detailQuery.data} open onClose={onClose} onPrinted={onSuccess} />
}
