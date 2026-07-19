import { Button, Form, Space, Spin, message } from 'antd'
import { DeleteOutlined, DownloadOutlined, PrinterOutlined, WalletOutlined } from '@ant-design/icons'
import { useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import MesPageHeader from '../../components/layout/MesPageHeader'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import { PERMISSIONS } from '../../constants/permissions'
import { useCancelReceive } from '../../features/settle/hooks/useCancelReceive'
import { useCreateSettleExportTask } from '../../features/exportTask/hooks/useExportTaskMutations'
import { useSettleDetails } from '../../features/settle/hooks/useSettleDetails'
import { useSettleOperationLogs } from '../../features/settle/hooks/useSettleOperationLogs'
import { useSettleOrderHeader } from '../../features/settle/hooks/useSettleOrderHeader'
import { useSettlePrintLines } from '../../features/settle/hooks/useSettlePrintLines'
import { useSettleReceives } from '../../features/settle/hooks/useSettleReceives'
import { useVoidSettle } from '../../features/settle/hooks/useVoidSettle'
import { useHasPermission } from '../../stores/authStore'
import type { ReceiveRecord, SettleDetailVO } from '../../types/settle'
import ReceiveModal from './ReceiveModal'
import SettleActionReasonModal, { type SettleActionTarget } from './SettleActionReasonModal'
import SettleAmountOverview from './SettleAmountOverview'
import { buildReceiveColumns } from './settleDetailColumns'
import SettleDetailHeader from './SettleDetailHeader'
import { buildExtraFeeByOrder } from './settleExtraFeeMap'
import '../documentModule.css'
import './SettleDetailPage.css'
import SettleDetailTabNav, { type DetailTab } from './SettleDetailTabNav'
import SettleDetailTabContent from './SettleDetailTabContent'
import { settleListReturnTarget } from './settleListNavigation'

export default function SettleDetailPage() {
  const { uuid } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const returnTo = settleListReturnTarget(location.state)
  const [actionForm] = Form.useForm<{ reason: string }>()
  const [actionTarget, setActionTarget] = useState<SettleActionTarget | null>(null)
  const [receiveOpen, setReceiveOpen] = useState(false)
  const [activeTab, setActiveTab] = useState<DetailTab>('overview')
  const canManageSettle = useHasPermission(PERMISSIONS.settleManage)
  const canReceiveSettle = useHasPermission(PERMISSIONS.settleReceive)
  const orderQuery = useSettleOrderHeader(uuid)
  const overviewEnabled = activeTab === 'overview' || activeTab === 'print'
  const detailsQuery = useSettleDetails(uuid, overviewEnabled)
  const printLinesQuery = useSettlePrintLines(uuid, overviewEnabled)
  const receivesQuery = useSettleReceives(uuid)
  const operationLogsQuery = useSettleOperationLogs(uuid, activeTab === 'audit')
  const cancelReceiveMutation = useCancelReceive()
  const exportMutation = useCreateSettleExportTask()
  const voidSettleMutation = useVoidSettle()
  const order = orderQuery.data
  const detail: SettleDetailVO | undefined = order ? {
    order,
    details: detailsQuery.data ?? [],
    receives: receivesQuery.data ?? [],
    printLines: printLinesQuery.data ?? [],
    operationLogs: operationLogsQuery.data ?? [],
  } : undefined
  const extraFeeByOrder = buildExtraFeeByOrder(printLinesQuery.data ?? [])
  const hasActiveReceive = (receivesQuery.data ?? []).some((record) => record.recordStatus !== 2)
  const sectionLoading = activeTab === 'audit'
    ? operationLogsQuery.isLoading
    : activeTab === 'receives'
      ? receivesQuery.isLoading
      : overviewEnabled && (detailsQuery.isLoading || printLinesQuery.isLoading)
  const sectionError = activeTab === 'audit'
    ? operationLogsQuery.isError
    : activeTab === 'receives'
      ? receivesQuery.isError
      : overviewEnabled && (detailsQuery.isError || printLinesQuery.isError)
  const receiveTableColumns = buildReceiveColumns({
    cancelLoading: cancelReceiveMutation.isPending,
    onCancelReceive: canReceiveSettle ? openCancelReceive : undefined,
  })

  const handleExport = async () => {
    if (uuid) {
      await exportMutation.mutateAsync({ uuid, requestId: crypto.randomUUID() })
      message.success('已加入导出任务，可在右上角下载任务中心查看')
    }
  }

  const handlePrint = async () => {
    setActiveTab('print')
    if (uuid) {
      await printLinesQuery.refetch()
    }
    window.setTimeout(() => {
      document.querySelector<HTMLElement>('.document-module-card--print')
        ?.scrollIntoView({ behavior: 'smooth', block: 'start' })
      window.print()
    }, 220)
  }

  const retrySection = () => {
    if (activeTab === 'audit') return void operationLogsQuery.refetch()
    if (activeTab === 'receives') return void receivesQuery.refetch()
    void Promise.all([detailsQuery.refetch(), printLinesQuery.refetch()])
  }

  async function handleConfirmAction() {
    const values = await actionForm.validateFields()
    if (!uuid || !actionTarget) return
    if (actionTarget.type === 'cancelReceive') {
      await cancelReceiveMutation.mutateAsync({ uuid, receiveUuid: actionTarget.record.uuid, data: values })
      message.success('收款已撤销')
      await Promise.all([orderQuery.refetch(), receivesQuery.refetch()])
    } else {
      await voidSettleMutation.mutateAsync({ uuid, data: values })
      message.success('结算单已作废')
      navigate(returnTo)
    }
    setActionTarget(null)
    actionForm.resetFields()
  }

  function openCancelReceive(record: ReceiveRecord) {
    actionForm.resetFields()
    setActionTarget({ type: 'cancelReceive', record })
  }

  function openVoidSettle() {
    actionForm.resetFields()
    setActionTarget({ type: 'voidSettle' })
  }

  return (
    <div className="document-module-page">
      <MesPageHeader
        title={order?.settleNo ?? '结算单详情'}
        description={order ? `${order.customerName || '-'} · 结算 ${order.settleDate || '-'} · 到期 ${order.dueDate || '-'}` : undefined}
        onBack={() => navigate(returnTo)}
        tags={order && <SettleDetailHeader order={order} />}
        actions={order && (
          <Space wrap>
            {canReceiveSettle && [1, 2].includes(order.settleStatus) && (
              <Button type="primary" icon={<WalletOutlined />} onClick={() => setReceiveOpen(true)}>
                登记收款
              </Button>
            )}
            <Button icon={<PrinterOutlined />} onClick={handlePrint}>打印预览</Button>
            <Button icon={<DownloadOutlined />} loading={exportMutation.isPending} onClick={handleExport}>
              后台导出
            </Button>
            {canManageSettle && order.settleStatus !== 4 && !hasActiveReceive && (
              <Button danger icon={<DeleteOutlined />} onClick={openVoidSettle}>作废结算单</Button>
            )}
          </Space>
        )}
      />

      {orderQuery.isError && (
        <QueryLoadErrorAlert
          message="结算单详情加载失败"
          description="当前结算单可能已被更新或暂时不可用，请重试后再进行收款、作废或导出。"
          onRetry={() => void orderQuery.refetch()}
        />
      )}
      {sectionError && (
        <QueryLoadErrorAlert
          message="当前分区加载失败"
          description="可以重试当前分区，已加载的结算单头部信息不会丢失。"
          onRetry={retrySection}
        />
      )}
      <Spin className="mes-spin-fill" spinning={orderQuery.isLoading || orderQuery.isFetching || sectionLoading}>
        {detail && (
          <>
            <div className="settle-detail-kpi">
              <SettleAmountOverview details={detail.details} order={detail.order} />
            </div>
            <div className="settle-detail-layout">
              <SettleDetailTabNav activeTab={activeTab} onChange={setActiveTab} />
              <div className="settle-detail-content">
                <SettleDetailTabContent
                  activeTab={activeTab}
                  detail={detail}
                  extraFeeByOrder={extraFeeByOrder}
                  receiveTableColumns={receiveTableColumns}
                  onReloadDetails={() => void detailsQuery.refetch()}
                  onReloadReceives={() => void receivesQuery.refetch()}
                />
              </div>
            </div>
          </>
        )}
      </Spin>

      <ReceiveModal
        settleUuid={uuid ?? null}
        unreceivedAmount={order?.unreceivedAmount ?? 0}
        open={receiveOpen}
        onClose={() => setReceiveOpen(false)}
        onSuccess={() => {
          setReceiveOpen(false)
          void Promise.all([orderQuery.refetch(), receivesQuery.refetch()])
        }}
      />
      <SettleActionReasonModal
        actionTarget={actionTarget}
        form={actionForm}
        loading={cancelReceiveMutation.isPending || voidSettleMutation.isPending}
        onCancel={() => setActionTarget(null)}
        onOk={handleConfirmAction}
      />
    </div>
  )
}
