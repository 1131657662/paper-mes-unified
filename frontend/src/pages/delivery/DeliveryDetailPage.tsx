import { useEffect, useRef, useState } from 'react'
import { Button, Card, Space, Spin, message } from 'antd'
import { CheckOutlined, DownloadOutlined, PlusOutlined, PrinterOutlined, RollbackOutlined, StopOutlined } from '@ant-design/icons'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { PERMISSIONS } from '../../constants/permissions'
import DocumentAuditTimeline from '../../components/biz/DocumentAuditTimeline'
import DocumentDetailTable from '../../components/biz/DocumentDetailTable'
import MesPageHeader from '../../components/layout/MesPageHeader'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import { useDeliveryDetail } from '../../features/delivery/hooks/useDeliveryDetail'
import { useConfirmDelivery } from '../../features/delivery/hooks/useConfirmDelivery'
import { useCancelPendingDelivery } from '../../features/delivery/hooks/useCancelPendingDelivery'
import { useCreateDeliveryOrderExportTask } from '../../features/exportTask/hooks/useCreateDeliveryOrderExportTask'
import { useRemoveDeliveryDetail } from '../../features/delivery/hooks/useRemoveDeliveryDetail'
import { useRollbackDelivery } from '../../features/delivery/hooks/useRollbackDelivery'
import type { DeliveryDetail } from '../../types/delivery'
import DeliveryAppendItemsModal from './DeliveryAppendItemsModal'
import DeliveryPrintSheet from './DeliveryPrintSheet'
import DeliveryRollbackSnapshotCard from './DeliveryRollbackSnapshotCard'
import { buildDeliveryDetailColumns } from './deliveryDetailColumns'
import { DeliveryOverview, DeliveryPickupInfo, DeliveryStatusTag } from './DeliveryDetailSummary'
import {
  askDeliveryCancelReason,
  askDeliveryRollbackReason,
  askDeliverySignUser,
  confirmRemoveDeliveryDetail,
} from './deliveryDetailDialogs'
import { useHasPermission } from '../../stores/authStore'
import '../documentModule.css'

export default function DeliveryDetailPage() {
  const { uuid } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const [appendOpen, setAppendOpen] = useState(false)
  const canManageDelivery = useHasPermission(PERMISSIONS.deliveryManage)
  const autoPrintDoneRef = useRef(false)
  const printPreviewRef = useRef<HTMLDivElement>(null)
  const detailQuery = useDeliveryDetail(uuid)
  const confirmMutation = useConfirmDelivery()
  const cancelMutation = useCancelPendingDelivery()
  const exportMutation = useCreateDeliveryOrderExportTask()
  const rollbackMutation = useRollbackDelivery()
  const removeDetailMutation = useRemoveDeliveryDetail()
  const detail = detailQuery.data
  const order = detail?.order
  const shouldAutoPrint = new URLSearchParams(location.search).get('print') === '1'

  useEffect(() => {
    if (!detail || !shouldAutoPrint || autoPrintDoneRef.current) return
    autoPrintDoneRef.current = true
    printPreviewRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    window.setTimeout(() => window.print(), 220)
  }, [detail, shouldAutoPrint])

  const handleExport = async () => {
    if (exportMutation.isPending) return
    if (uuid) {
      await exportMutation.mutateAsync({ uuid })
      message.success('已加入导出任务，可在右上角下载任务中心查看')
    }
  }

  const handlePrint = () => {
    printPreviewRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    window.setTimeout(() => window.print(), 220)
  }

  const handleConfirm = async () => {
    if (confirmMutation.isPending || !canManageDelivery || !uuid || !order) return
    const signUser = await askDeliverySignUser(order.deliveryNo)
    if (signUser === null) return
    await confirmMutation.mutateAsync({ uuid, data: signUser ? { signUser } : undefined })
    message.success('出库签收完成')
    detailQuery.refetch()
  }

  const handleRollback = async () => {
    if (rollbackMutation.isPending || !canManageDelivery) return
    if (!uuid || !order) return
    const reason = await askDeliveryRollbackReason(order.deliveryNo).catch(() => null)
    if (!reason) return
    await rollbackMutation.mutateAsync({ uuid, data: { reason } })
    message.success('已回退为待出库，可继续改单')
    detailQuery.refetch()
  }

  const handleCancel = async () => {
    if (cancelMutation.isPending || !canManageDelivery || !uuid || !order) return
    const reason = await askDeliveryCancelReason(order.deliveryNo)
    if (!reason) return
    await cancelMutation.mutateAsync({ uuid, data: { reason } })
    message.success('待出库单已作废，成品库存已释放')
    navigate('/delivery-orders')
  }

  const handleRemove = async (record: DeliveryDetail) => {
    if (removeDetailMutation.isPending || !canManageDelivery) return
    if (!uuid) return
    const confirmed = await confirmRemoveDeliveryDetail(record.finishRollNo)
    if (!confirmed) return
    await removeDetailMutation.mutateAsync({ uuid, detailUuid: record.uuid })
    message.success('已从本张出库单移出')
    detailQuery.refetch()
  }

  return (
    <div className="document-module-page">
      <MesPageHeader
        title={order?.deliveryNo ?? '出库单详情'}
        description={order ? `${order.customerName || '-'} · ${order.deliveryDate || '-'}` : undefined}
        onBack={() => navigate('/delivery-orders')}
        tags={order && <DeliveryStatusTag status={order.deliveryStatus} />}
        actions={order && (
          <Space wrap>
            {canManageDelivery && order.deliveryStatus === 1 && (
              <Button type="primary" icon={<CheckOutlined />} loading={confirmMutation.isPending} onClick={handleConfirm}>
                确认签收
              </Button>
            )}
            {canManageDelivery && order.deliveryStatus === 1 && (
              <Button icon={<PlusOutlined />} onClick={() => setAppendOpen(true)}>
                添加出库卷
              </Button>
            )}
            <Button icon={<PrinterOutlined />} onClick={handlePrint}>打印预览</Button>
            <Button icon={<DownloadOutlined />} loading={exportMutation.isPending} onClick={handleExport}>
              后台导出
            </Button>
            {canManageDelivery && order.deliveryStatus === 1 && (
              <Button danger icon={<StopOutlined />} loading={cancelMutation.isPending} onClick={handleCancel}>
                作废待出库单
              </Button>
            )}
            {canManageDelivery && order.deliveryStatus === 2 && (
              <Button danger icon={<RollbackOutlined />} loading={rollbackMutation.isPending} onClick={handleRollback}>
                回退出库
              </Button>
            )}
          </Space>
        )}
      />

      {detailQuery.isError && (
        <QueryLoadErrorAlert
          message="出库单详情加载失败"
          description="当前空白不代表单据不存在，请重新加载后再执行出库操作。"
          onRetry={() => void detailQuery.refetch()}
        />
      )}

      <Spin className="mes-spin-fill" spinning={detailQuery.isLoading || detailQuery.isFetching}>
        {detail && (
          <>
            <DeliveryOverview order={detail.order} />

            <DeliveryPickupInfo order={detail.order} />

            <Card className="document-module-card" title="出库明细">
              <div className="document-module-table">
                <DocumentDetailTable
                  storageKey="delivery-detail-items"
                  rowKey="uuid"
                  columns={buildDeliveryDetailColumns({
                    canRemove: canManageDelivery,
                    deliveryStatus: detail.order.deliveryStatus,
                    onRemove: handleRemove,
                  })}
                  dataSource={detail.details}
                  onReload={() => detailQuery.refetch()}
                  pagination={false}
                  scroll={{ x: 1280 }}
                />
              </div>
            </Card>

            {detail.rollbackSnapshot && (
              <DeliveryRollbackSnapshotCard snapshot={detail.rollbackSnapshot} />
            )}

            <Card className="document-module-card" title="业务追踪">
              <DocumentAuditTimeline logs={detail.operationLogs ?? []} />
            </Card>

            <Card ref={printPreviewRef} className="document-module-card document-module-card--print" title="司机单据预览">
              <DeliveryPrintSheet detail={detail} />
            </Card>

            <DeliveryAppendItemsModal
              customerName={detail.order.customerName}
              customerUuid={detail.order.customerUuid}
              warehouseUuid={detail.order.warehouseUuid}
              deliveryUuid={detail.order.uuid}
              open={appendOpen}
              onClose={() => setAppendOpen(false)}
              onSuccess={() => detailQuery.refetch()}
            />
          </>
        )}
      </Spin>
    </div>
  )
}
