import { Button, Card, Descriptions, Form, Input, Modal, Space, Spin, Tag, message } from 'antd'
import { StatisticCard } from '@ant-design/pro-components'
import { DeleteOutlined, DownloadOutlined, PrinterOutlined, WalletOutlined } from '@ant-design/icons'
import { useNavigate, useParams } from 'react-router-dom'
import MesPageHeader from '../../components/layout/MesPageHeader'
import DocumentDetailTable from '../../components/biz/DocumentDetailTable'
import { INVOICE_TYPE, SETTLE_STATUS, SETTLE_TYPE } from '../../constants/settle'
import { useCancelReceive } from '../../features/settle/hooks/useCancelReceive'
import { useExportSettle } from '../../features/settle/hooks/useExportSettle'
import { useSettleDetail } from '../../features/settle/hooks/useSettleDetail'
import { useVoidSettle } from '../../features/settle/hooks/useVoidSettle'
import { formatMoney } from '../../features/settle/utils/settleFormatters'
import type { ReceiveRecord, SettleOrder, SettlePrintLine } from '../../types/settle'
import DocumentAuditTimeline from '../../components/biz/DocumentAuditTimeline'
import ReceiveModal from './ReceiveModal'
import SettleGroupedBill from './SettleGroupedBill'
import SettlePrintSheet from './SettlePrintSheet'
import { buildReceiveColumns, buildSettleDetailColumns } from './settleDetailColumns'
import { useRef, useState } from 'react'
import '../documentModule.css'

export default function SettleDetailPage() {
  const { uuid } = useParams()
  const navigate = useNavigate()
  const [actionForm] = Form.useForm<{ reason: string }>()
  const [actionTarget, setActionTarget] = useState<SettleActionTarget | null>(null)
  const [receiveOpen, setReceiveOpen] = useState(false)
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
    onCancelReceive: openCancelReceive,
  })

  const handleExport = async () => {
    if (uuid) await exportMutation.mutateAsync(uuid)
  }

  const handlePrint = () => {
    printPreviewRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    window.setTimeout(() => window.print(), 220)
  }

  async function handleConfirmAction() {
    const values = await actionForm.validateFields()
    if (!uuid || !actionTarget) return
    if (actionTarget.type === 'cancelReceive') {
      await cancelReceiveMutation.mutateAsync({
        uuid,
        receiveUuid: actionTarget.record.uuid,
        data: values,
      })
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
        description="面向客户的结算单详情，包含加工单费用组成、原纸粒度结算明细、收款记录、Excel 导出和打印预览。"
        onBack={() => navigate('/settle-orders')}
        tags={order && <SettleStatusTag status={order.settleStatus} />}
        actions={order && (
          <Space wrap>
            <Button icon={<PrinterOutlined />} onClick={handlePrint}>打印预览</Button>
            <Button icon={<DownloadOutlined />} loading={exportMutation.isPending} onClick={handleExport}>
              导出 Excel
            </Button>
            {!hasActiveReceive && (
              <Button danger icon={<DeleteOutlined />} onClick={openVoidSettle}>
                作废结算单
              </Button>
            )}
            {order.settleStatus !== 3 && (
              <Button type="primary" icon={<WalletOutlined />} onClick={() => setReceiveOpen(true)}>
                登记收款
              </Button>
            )}
          </Space>
        )}
      />

      <Spin className="mes-spin-fill" spinning={detailQuery.isLoading || detailQuery.isFetching}>
        {detail && (
          <>
            <SettleAmountOverview order={detail.order} />

            <Card className="document-module-card" title="结算基础信息">
              <Descriptions bordered size="small" column={{ xs: 1, md: 2, xl: 3 }}>
                <Descriptions.Item label="结算单号">{detail.order.settleNo}</Descriptions.Item>
                <Descriptions.Item label="客户">{detail.order.customerName}</Descriptions.Item>
                <Descriptions.Item label="结算类型">{SETTLE_TYPE[detail.order.settleType] || '-'}</Descriptions.Item>
                <Descriptions.Item label="结算日期">{detail.order.settleDate}</Descriptions.Item>
                <Descriptions.Item label="账期">
                  {detail.order.periodStart || '-'} ~ {detail.order.periodEnd || '-'}
                </Descriptions.Item>
                <Descriptions.Item label="是否开票">{INVOICE_TYPE[detail.order.isInvoice] || '-'}</Descriptions.Item>
                <Descriptions.Item label="未税金额">{formatMoney(detail.order.amountNoTax)}</Descriptions.Item>
                <Descriptions.Item label="税点加价">{formatMoney(detail.order.taxAmount)}</Descriptions.Item>
                <Descriptions.Item label="已收金额">{formatMoney(detail.order.receivedAmount)}</Descriptions.Item>
                <Descriptions.Item label="未收金额">{formatMoney(detail.order.unreceivedAmount)}</Descriptions.Item>
                <Descriptions.Item label="备注" span="filled">{detail.order.remark || '-'}</Descriptions.Item>
              </Descriptions>
            </Card>

            <Card className="document-module-card" title="加工单费用组成">
              <div className="document-module-table">
                <DocumentDetailTable
                  storageKey="settle-detail-fee-items"
                  rowKey="uuid"
                  columns={buildSettleDetailColumns(extraFeeByOrder)}
                  dataSource={detail.details}
                  onReload={() => detailQuery.refetch()}
                  pagination={false}
                  scroll={{ x: 680 }}
                />
              </div>
            </Card>

            <Card className="document-module-card" title="客户结算明细">
              <div className="document-module-table">
                <SettleGroupedBill lines={detail.printLines ?? []} />
              </div>
            </Card>

            <Card className="document-module-card" title="收款记录">
              <div className="document-module-table">
                <DocumentDetailTable
                  storageKey="settle-detail-receive-records"
                  rowKey="uuid"
                  columns={receiveTableColumns}
                  dataSource={detail.receives}
                  onReload={() => detailQuery.refetch()}
                  pagination={false}
                  scroll={{ x: 1060 }}
                />
              </div>
            </Card>

            <Card className="document-module-card" title="业务追踪">
              <DocumentAuditTimeline logs={detail.operationLogs ?? []} />
            </Card>

            <Card ref={printPreviewRef} className="document-module-card document-module-card--print" title="客户单据预览">
              <SettlePrintSheet detail={detail} />
            </Card>
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
      <Modal
        title={actionTarget?.type === 'cancelReceive' ? '撤销收款' : '作废结算单'}
        open={!!actionTarget}
        okText="确认"
        cancelText="取消"
        okButtonProps={{
          danger: true,
          loading: cancelReceiveMutation.isPending || voidSettleMutation.isPending,
        }}
        onCancel={() => setActionTarget(null)}
        onOk={handleConfirmAction}
      >
        <p className="document-action-warning">
          {actionTarget?.type === 'cancelReceive'
            ? '撤销后系统会重新计算结算单的已收、未收和状态，原收款流水会保留为已撤销。'
            : '作废后关联加工单会退回已完成可结算状态，已有有效收款的结算单不能作废。'}
        </p>
        <Form form={actionForm} layout="vertical">
          <Form.Item
            name="reason"
            label="原因"
            rules={[{ required: true, message: '请输入原因' }]}
          >
            <Input.TextArea rows={3} maxLength={255} showCount placeholder="请输入原因，便于业务追溯" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

type SettleActionTarget =
  | { type: 'cancelReceive'; record: ReceiveRecord }
  | { type: 'voidSettle' }

function SettleStatusTag({ status }: { status?: number }) {
  const item = status ? SETTLE_STATUS[status] : undefined
  return item ? <Tag className="mes-status-tag" color={item.color}>{item.text}</Tag> : <>-</>
}

function SettleAmountOverview({ order }: { order: SettleOrder }) {
  const items: SettleOverviewItem[] = [
    {
      label: '应收总额',
      tone: 'primary',
      value: formatMoney(order.totalAmount),
    },
    {
      label: '已收金额',
      tone: 'success',
      value: formatMoney(order.receivedAmount),
    },
    {
      label: '未收金额',
      tone: 'warning',
      value: formatMoney(order.unreceivedAmount),
    },
    {
      hint: '锯纸费 / 复卷费 / 额外费',
      label: '费用构成',
      value: `${formatMoney(order.sawAmount)} / ${formatMoney(order.rewindAmount)} / ${formatMoney(order.extraAmount)}`,
    },
  ]

  return (
    <StatisticCard.Group className="document-amount-overview" gutter={[12, 12]} ghost>
      {items.map((item) => (
        <StatisticCard
          className={`document-amount-card ${item.tone ? `document-amount-card--${item.tone}` : ''}`}
          colSpan={{ xs: 24, md: 12, xl: 6 }}
          key={item.label}
          statistic={{
            description: item.hint,
            title: item.label,
            value: item.value,
          }}
        />
      ))}
    </StatisticCard.Group>
  )
}

interface SettleOverviewItem {
  hint?: string
  label: string
  tone?: 'primary' | 'success' | 'warning'
  value: string
}

function buildExtraFeeByOrder(lines: SettlePrintLine[]) {
  const map: Record<string, string> = {}
  for (const line of lines ?? []) {
    if (line.orderUuid && line.extraFeeSummary && !map[line.orderUuid]) {
      map[line.orderUuid] = line.extraFeeSummary
    }
  }
  return map
}
