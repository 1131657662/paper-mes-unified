import { useState } from 'react'
import { Card, Spin, message } from 'antd'
import { useLocation, useNavigate, useParams } from 'react-router'
import { notifyErrorOnce } from '../../api/request'
import { PERMISSIONS } from '../../constants/permissions'
import DocumentAuditTimeline from '../../components/biz/DocumentAuditTimeline'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import { useDeliveryDetail } from '../../features/delivery/hooks/useDeliveryDetail'
import { useConfirmDelivery } from '../../features/delivery/hooks/useConfirmDelivery'
import { useCancelPendingDelivery } from '../../features/delivery/hooks/useCancelPendingDelivery'
import { useCreateDeliveryOrderExportTask } from '../../features/exportTask/hooks/useCreateDeliveryOrderExportTask'
import { useRemoveDeliveryDetail } from '../../features/delivery/hooks/useRemoveDeliveryDetail'
import { useRollbackDelivery } from '../../features/delivery/hooks/useRollbackDelivery'
import { useDeliveryCustomerSpecs } from '../../features/deliveryCustomerSpec/useDeliveryCustomerSpecs'
import type { DeliveryDocumentView } from '../../features/deliveryCustomerSpec/deliveryCustomerSpecTypes'
import type { DeliveryDetail } from '../../types/delivery'
import type { DeliveryExportSortChains } from '../../types/deliverySort'
import DeliveryAppendItemsModal from './DeliveryAppendItemsModal'
import DeliveryPrintPreviewCard from './DeliveryPrintPreviewCard'
import DeliveryRollbackSnapshotCard from './DeliveryRollbackSnapshotCard'
import DeliveryCustomerDocumentSection from './DeliveryCustomerDocumentSection'
import DeliveryDetailHeader, { type DeliveryDetailHeaderActions } from './DeliveryDetailHeader'
import DeliveryPendingEditModal from './DeliveryPendingEditModal'
import { DeliveryOverview, DeliveryPickupInfo } from './DeliveryDetailSummary'
import { useDeliveryPrintActions } from './useDeliveryPrintActions'
import { useDeliveryDetailTableState } from './useDeliveryDetailTableState'
import { buildDeliveryPrintProjection } from './deliveryPrintProjection'
import { authorizeDeliveryConfirmation } from './deliveryConfirmAuthorization'
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
  const [editOpen, setEditOpen] = useState(false)
  const [documentView, setDocumentView] = useState<DeliveryDocumentView>('customer')
  const canManageDelivery = useHasPermission(PERMISSIONS.deliveryManage)
  const canReleaseDelivery = useHasPermission(PERMISSIONS.deliveryRelease)
  const canConfirmDelivery = canManageDelivery || canReleaseDelivery
  const detailQuery = useDeliveryDetail(uuid)
  const confirmMutation = useConfirmDelivery()
  const cancelMutation = useCancelPendingDelivery()
  const exportMutation = useCreateDeliveryOrderExportTask()
  const rollbackMutation = useRollbackDelivery()
  const removeDetailMutation = useRemoveDeliveryDetail()
  const detail = detailQuery.data
  const order = detail?.order
  const detailTableState = useDeliveryDetailTableState()
  const customerSpecQuery = useDeliveryCustomerSpecs(uuid)
  const sortChains = {
    physical: detailTableState.physical.sortChain,
    customer: detailTableState.customer.sortChain,
    trace: detailTableState.trace.sortChain,
  } satisfies DeliveryExportSortChains
  const printProjection = detail ? buildDeliveryPrintProjection({
    detail, customerSpecs: customerSpecQuery.data, variant: documentView, sortChains,
  }) : undefined
  const shouldAutoPrint = new URLSearchParams(location.search).get('print') === '1'
  const { printPreviewRef, requestPrint } = useDeliveryPrintActions({
    detailReady: Boolean(detail),
    documentReady: printProjection?.status === 'ready',
    documentError: printProjection?.status === 'invalid' ? printProjection.message : undefined,
    shouldAutoPrint,
  })

  const refreshDetailSafely = async (failureMessage: string) => {
    try {
      const refreshed = await detailQuery.refetch()
      if (refreshed.isError) throw refreshed.error
    } catch (error) {
      notifyErrorOnce(error, failureMessage)
    }
  }

  const handleExport = async () => {
    if (exportMutation.isPending) return
    if (uuid) {
      const completed = await exportMutation.mutateAsync({
        uuid,
        customerRevisionNo: customerSpecQuery.data?.currentRevisionNo ?? 0,
        documentView,
        sortChains,
      })
        .then(() => true)
        .catch((error) => {
          notifyErrorOnce(error, '出库单导出失败，请刷新后重试')
          return false
        })
      if (!completed) return
      message.success('已加入导出任务，可在右上角下载任务中心查看')
    }
  }

  const handleConfirm = async () => {
    if (confirmMutation.isPending || !canConfirmDelivery || !uuid || !order) return
    const signUser = await askDeliverySignUser(order.deliveryNo)
    if (signUser === null) return
    try {
      const completed = await authorizeDeliveryConfirmation((forceRelease) => (
        confirmMutation.mutateAsync({
          uuid,
          data: { ...(signUser ? { signUser } : {}), forceRelease },
        })
      ), 1, canReleaseDelivery)
      if (!completed) return
      message.success('出库签收完成')
      await refreshDetailSafely('出库已签收，但详情刷新失败，请手动刷新页面')
    } catch (error) {
      notifyErrorOnce(error, '出库签收失败，请刷新后重试')
      await refreshDetailSafely('出库详情刷新失败，请手动刷新页面')
    }
  }

  const handleRollback = async () => {
    if (rollbackMutation.isPending || !canManageDelivery) return
    if (!uuid || !order) return
    const reason = await askDeliveryRollbackReason(order.deliveryNo).catch(() => null)
    if (!reason) return
    const completed = await rollbackMutation.mutateAsync({ uuid, data: { reason } })
      .then(() => true)
      .catch((error) => {
        notifyErrorOnce(error, '出库单回退失败，请刷新后重试')
        return false
      })
    if (!completed) return
    message.success('已回退为待出库，可继续改单')
    await refreshDetailSafely('出库单已回退，但详情刷新失败，请手动刷新页面')
  }

  const handleCancel = async () => {
    if (cancelMutation.isPending || !canManageDelivery || !uuid || !order) return
    const reason = await askDeliveryCancelReason(order.deliveryNo)
    if (!reason) return
    const completed = await cancelMutation.mutateAsync({ uuid, data: { reason } })
      .then(() => true)
      .catch((error) => {
        notifyErrorOnce(error, '取消出库单失败，请刷新后重试')
        return false
      })
    if (!completed) return
    message.success('待出库单已作废，成品库存已释放')
    navigate('/delivery-orders')
  }

  const handleRemove = async (record: DeliveryDetail) => {
    if (removeDetailMutation.isPending || !canManageDelivery) return
    if (!uuid) return
    const confirmed = await confirmRemoveDeliveryDetail(record.finishRollNo)
    if (!confirmed) return
    const completed = await removeDetailMutation.mutateAsync({ uuid, detailUuid: record.uuid })
      .then(() => true)
      .catch((error) => {
        notifyErrorOnce(error, '移除出库明细失败，请刷新后重试')
        return false
      })
    if (!completed) return
    message.success('已从本张出库单移出')
    await refreshDetailSafely('明细已移除，但出库单刷新失败，请手动刷新页面')
  }

  const headerActions = {
    canConfirm: canConfirmDelivery,
    canManage: canManageDelivery,
    cancelling: cancelMutation.isPending,
    confirming: confirmMutation.isPending,
    exporting: exportMutation.isPending,
    rollingBack: rollbackMutation.isPending,
    onAppend: () => setAppendOpen(true),
    onCancel: () => void handleCancel(),
    onConfirm: () => void handleConfirm(),
    onEdit: () => setEditOpen(true),
    onExport: () => void handleExport(),
    onPrint: requestPrint,
    onRollback: () => void handleRollback(),
  } satisfies DeliveryDetailHeaderActions

  return (
    <div className="document-module-page">
      <DeliveryDetailHeader actions={headerActions} order={order}
        onBack={() => navigate('/delivery-orders')} />

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

            {customerSpecQuery.isError && (
              <QueryLoadErrorAlert message="客户单据口径加载失败"
                description="为避免打印错误口径，客户单据与追溯视图暂不可打印；仓库实物视图不受影响。"
                onRetry={() => void customerSpecQuery.refetch()} />
            )}

            <DeliveryCustomerDocumentSection
              canManage={canManageDelivery}
              customerSpecs={customerSpecQuery.data}
              detail={detail}
              loading={customerSpecQuery.isLoading}
              view={documentView}
              sortState={detailTableState}
              onReload={() => void refreshDetailSafely('出库单详情刷新失败，请手动刷新页面')}
              onRemove={handleRemove}
              onViewChange={setDocumentView}
            />

            {detail.rollbackSnapshot && (
              <DeliveryRollbackSnapshotCard snapshot={detail.rollbackSnapshot} />
            )}

            <Card className="document-module-card" title="业务追踪">
              <DocumentAuditTimeline logs={detail.operationLogs ?? []} />
            </Card>

            {printProjection && <DeliveryPrintPreviewCard
              ref={printPreviewRef}
              detail={detail}
              customerSpecs={customerSpecQuery.data}
              projection={printProjection}
            />}

            <DeliveryAppendItemsModal
              customerName={detail.order.customerName}
              customerUuid={detail.order.customerUuid}
              warehouseUuid={detail.order.warehouseUuid}
              deliveryUuid={detail.order.uuid}
              open={appendOpen}
              onClose={() => setAppendOpen(false)}
              onSuccess={() => void refreshDetailSafely('明细已追加，但出库单刷新失败，请手动刷新页面')}
            />

            {editOpen && (
              <DeliveryPendingEditModal
                open
                order={detail.order}
                onCancel={() => setEditOpen(false)}
                onSaved={() => setEditOpen(false)}
              />
            )}
          </>
        )}
      </Spin>
    </div>
  )
}
