import { Button, Card, Form, Space, Spin, message } from 'antd'
import { DeleteOutlined, DownloadOutlined, PrinterOutlined, WalletOutlined } from '@ant-design/icons'
import { useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import MesPageHeader from '../../components/layout/MesPageHeader'
import DocumentAuditTimeline from '../../components/biz/DocumentAuditTimeline'
import DocumentDetailTable from '../../components/biz/DocumentDetailTable'
import { PERMISSIONS } from '../../constants/permissions'
import { useCancelReceive } from '../../features/settle/hooks/useCancelReceive'
import { useExportSettle } from '../../features/settle/hooks/useExportSettle'
import { useSettleDetail } from '../../features/settle/hooks/useSettleDetail'
import { useVoidSettle } from '../../features/settle/hooks/useVoidSettle'
import { useHasPermission } from '../../stores/authStore'
import type { ReceiveRecord } from '../../types/settle'
import ReceiveModal from './ReceiveModal'
import SettleActionReasonModal, { type SettleActionTarget } from './SettleActionReasonModal'
import SettleAmountOverview from './SettleAmountOverview'
import SettleGroupedBill from './SettleGroupedBill'
import SettlePrintSheet from './SettlePrintSheet'
import SettlementInfoCard from './SettlementInfoCard'
import { buildReceiveColumns, buildSettleDetailColumns } from './settleDetailColumns'
import SettleDetailHeader from './SettleDetailHeader'
import { buildExtraFeeByOrder } from './settleExtraFeeMap'
import '../documentModule.css'

export default function SettleDetailPage() {
  const { uuid } = useParams()
  const navigate = useNavigate()
  const [actionForm] = Form.useForm<{ reason: string }>()
  const [actionTarget, setActionTarget] = useState<SettleActionTarget | null>(null)
  const [receiveOpen, setReceiveOpen] = useState(false)
  const canManageSettle = useHasPermission(PERMISSIONS.settleManage)
  const canReceiveSettle = useHasPermission(PERMISSIONS.settleReceive)
  const printPreviewRef = useRef<HTMLDivElement>(null)
  const detailQuery = useSettleDetail(uuid)
  const cancelReceiveMutation = useCancelReceive()
  const exportMutation = useExportSettle()
  const voidSettleMutation = useVoidSettle()
  const detail = detailQuery.data
  const order = detail?.order
  const extraFeeByOrder = buildExtraFeeByOrder(detail?.printLines ?? [])
  const hasActiveReceive = (detail?.receives ?? []).some((record) => record.recordStatus !== 2)
  const receiveTableColumns = buildReceiveColumns({
    cancelLoading: cancelReceiveMutation.isPending,
    onCancelReceive: canReceiveSettle ? openCancelReceive : undefined,
  })

  const handleExport = async () => {
    if (uuid) {
      await exportMutation.mutateAsync({ documentNo: order?.settleNo, uuid })
    }
  }

  const handlePrint = () => {
    printPreviewRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    window.setTimeout(() => window.print(), 220)
  }

  async function handleConfirmAction() {
    const values = await actionForm.validateFields()
    if (!uuid || !actionTarget) return
    if (actionTarget.type === 'cancelReceive') {
      await cancelReceiveMutation.mutateAsync({ uuid, receiveUuid: actionTarget.record.uuid, data: values })
      message.success('收款已撤销')
      detailQuery.refetch()
    } else {
      await voidSettleMutation.mutateAsync({ uuid, data: values })
      message.success('结算单已作废')
      navigate('/settle-orders')
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
        description={order ? `${order.customerName || '-'} · ${order.settleDate || '-'}` : undefined}
        onBack={() => navigate('/settle-orders')}
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
              导出 Excel
            </Button>
            {canManageSettle && order.settleStatus !== 4 && !hasActiveReceive && (
              <Button danger icon={<DeleteOutlined />} onClick={openVoidSettle}>作废结算单</Button>
            )}
          </Space>
        )}
      />

      <Spin className="mes-spin-fill" spinning={detailQuery.isLoading || detailQuery.isFetching}>
        {detail && (
          <>
            <SettleAmountOverview order={detail.order} />
            <SettlementInfoCard detail={detail} />
            <Card className="document-module-card" title="加工单费用组成">
              <DocumentDetailTable
                storageKey="settle-detail-fee-items"
                rowKey="uuid"
                columns={buildSettleDetailColumns(extraFeeByOrder)}
                dataSource={detail.details}
                onReload={() => detailQuery.refetch()}
                pagination={false}
                scroll={{ x: 680 }}
              />
            </Card>
            <Card className="document-module-card" title="客户结算明细">
              <SettleGroupedBill lines={detail.printLines ?? []} />
            </Card>
            <Card className="document-module-card" title="收款记录">
              <DocumentDetailTable
                storageKey="settle-detail-receive-records"
                rowKey="uuid"
                columns={receiveTableColumns}
                dataSource={detail.receives}
                onReload={() => detailQuery.refetch()}
                pagination={false}
                scroll={{ x: 1050 }}
              />
            </Card>
            <Card className="document-module-card" title="业务追踪">
              <DocumentAuditTimeline logs={detail.operationLogs ?? []} />
            </Card>
            <div ref={printPreviewRef}>
              <Card className="document-module-card document-module-card--print" title="客户单据预览">
                <SettlePrintSheet detail={detail} />
              </Card>
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
          detailQuery.refetch()
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
