import { useEffect, useRef, useState } from 'react'
import { Button, Card, Descriptions, Input, Modal, Space, Spin, Tag, message } from 'antd'
import { StatisticCard } from '@ant-design/pro-components'
import { DownloadOutlined, PlusOutlined, PrinterOutlined, RollbackOutlined } from '@ant-design/icons'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { DELIVERY_STATUS, SETTLE_BLOCK_ACTION } from '../../constants/delivery'
import DocumentAuditTimeline from '../../components/biz/DocumentAuditTimeline'
import DocumentDetailTable from '../../components/biz/DocumentDetailTable'
import MesPageHeader from '../../components/layout/MesPageHeader'
import { useDeliveryDetail } from '../../features/delivery/hooks/useDeliveryDetail'
import { useExportDelivery } from '../../features/delivery/hooks/useExportDelivery'
import { useRemoveDeliveryDetail } from '../../features/delivery/hooks/useRemoveDeliveryDetail'
import { useRollbackDelivery } from '../../features/delivery/hooks/useRollbackDelivery'
import { formatKg } from '../../features/delivery/utils/deliveryFormatters'
import type { DeliveryDetail, DeliveryOrder } from '../../types/delivery'
import DeliveryAppendItemsModal from './DeliveryAppendItemsModal'
import DeliveryPrintSheet from './DeliveryPrintSheet'
import { buildDeliveryDetailColumns } from './deliveryDetailColumns'
import '../documentModule.css'

export default function DeliveryDetailPage() {
  const { uuid } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const [appendOpen, setAppendOpen] = useState(false)
  const autoPrintDoneRef = useRef(false)
  const printPreviewRef = useRef<HTMLDivElement>(null)
  const detailQuery = useDeliveryDetail(uuid)
  const exportMutation = useExportDelivery()
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
    if (uuid) await exportMutation.mutateAsync(uuid)
  }

  const handlePrint = () => {
    printPreviewRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    window.setTimeout(() => window.print(), 220)
  }

  const handleRollback = async () => {
    if (!uuid || !order) return
    const reason = await askRollbackReason(order.deliveryNo)
    await rollbackMutation.mutateAsync({ uuid, data: { reason } })
    message.success('已回退为待出库，可继续改单')
    detailQuery.refetch()
  }

  const handleRemove = async (record: DeliveryDetail) => {
    if (!uuid) return
    await confirmRemove(record.finishRollNo)
    await removeDetailMutation.mutateAsync({ uuid, detailUuid: record.uuid })
    message.success('已从本张出库单移出')
    detailQuery.refetch()
  }

  return (
    <div className="document-module-page">
      <MesPageHeader
        title={order?.deliveryNo ?? '出库单详情'}
        description="查看司机签收单据、出库明细，支持 Excel 导出、打印预览、签收回退和待出库改单。"
        onBack={() => navigate('/delivery-orders')}
        tags={order && <DeliveryStatusTag status={order.deliveryStatus} />}
        actions={order && (
          <Space wrap>
            {order.deliveryStatus === 1 && (
              <Button icon={<PlusOutlined />} onClick={() => setAppendOpen(true)}>
                添加出库卷
              </Button>
            )}
            <Button icon={<PrinterOutlined />} onClick={handlePrint}>打印预览</Button>
            <Button icon={<DownloadOutlined />} loading={exportMutation.isPending} onClick={handleExport}>
              导出 Excel
            </Button>
            {order.deliveryStatus === 2 && (
              <Button danger icon={<RollbackOutlined />} loading={rollbackMutation.isPending} onClick={handleRollback}>
                回退出库
              </Button>
            )}
          </Space>
        )}
      />

      <Spin className="mes-spin-fill" spinning={detailQuery.isLoading || detailQuery.isFetching}>
        {detail && (
          <>
            <DeliveryOverview order={detail.order} />

            <Card className="document-module-card" title="出库信息">
              <Descriptions bordered size="small" column={{ xs: 1, md: 2, xl: 3 }}>
                <Descriptions.Item label="出库单号">{detail.order.deliveryNo}</Descriptions.Item>
                <Descriptions.Item label="客户">{detail.order.customerName}</Descriptions.Item>
                <Descriptions.Item label="出库日期">{detail.order.deliveryDate}</Descriptions.Item>
                <Descriptions.Item label="出库统计">
                  {detail.order.totalCount} 卷 / {formatKg(detail.order.totalWeight)}
                </Descriptions.Item>
                <Descriptions.Item label="提货人">{detail.order.pickerName || '-'}</Descriptions.Item>
                <Descriptions.Item label="车牌号">{detail.order.carNo || '-'}</Descriptions.Item>
                <Descriptions.Item label="柜号">{detail.order.containerNo || '-'}</Descriptions.Item>
                <Descriptions.Item label="签收人">{detail.order.signUser || '-'}</Descriptions.Item>
                <Descriptions.Item label="签收时间">{detail.order.signTime || '-'}</Descriptions.Item>
                <Descriptions.Item label="结算拦截">
                  {detail.order.settleBlockAction
                    ? <Tag className="mes-status-tag" color="orange">{SETTLE_BLOCK_ACTION[detail.order.settleBlockAction]}</Tag>
                    : '-'}
                </Descriptions.Item>
                <Descriptions.Item label="备注" span="filled">{detail.order.remark || '-'}</Descriptions.Item>
              </Descriptions>
            </Card>

            <Card className="document-module-card" title="出库明细">
              <div className="document-module-table">
                <DocumentDetailTable
                  storageKey="delivery-detail-items"
                  rowKey="uuid"
                  columns={buildDeliveryDetailColumns({
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

            <Card className="document-module-card" title="业务追踪">
              <DocumentAuditTimeline logs={detail.operationLogs ?? []} />
            </Card>

            <Card ref={printPreviewRef} className="document-module-card document-module-card--print" title="司机单据预览">
              <DeliveryPrintSheet detail={detail} />
            </Card>

            <DeliveryAppendItemsModal
              customerName={detail.order.customerName}
              customerUuid={detail.order.customerUuid}
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

function DeliveryStatusTag({ status }: { status?: number }) {
  const item = status ? DELIVERY_STATUS[status] : undefined
  return item ? <Tag className="mes-status-tag" color={item.color}>{item.text}</Tag> : <>-</>
}

function DeliveryOverview({ order }: { order: DeliveryOrder }) {
  const items: DeliveryOverviewItem[] = [
    {
      label: '出库卷数',
      tone: 'primary',
      value: `${order.totalCount ?? 0} 卷`,
    },
    {
      label: '出库重量',
      value: formatKg(order.totalWeight),
    },
    {
      hint: order.signTime || '司机签收后扣减库存',
      label: '签收状态',
      tone: order.deliveryStatus === 2 ? 'success' : 'warning',
      value: order.deliveryStatus === 2 ? '已签收' : '待签收',
    },
    {
      hint: order.settleBlockAction ? SETTLE_BLOCK_ACTION[order.settleBlockAction] : '无结算拦截',
      label: '改单状态',
      value: order.deliveryStatus === 1 ? '可调整' : '需先回退',
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

interface DeliveryOverviewItem {
  hint?: string
  label: string
  tone?: 'primary' | 'success' | 'warning'
  value: string
}

function askRollbackReason(deliveryNo: string) {
  let reason = ''
  return new Promise<string>((resolve, reject) => {
    Modal.confirm({
      title: `回退出库 ${deliveryNo}`,
      content: (
        <div className="delivery-sign-modal">
          <p>回退后成品卷恢复为已入库，出库单回到待出库状态，可移出装不下的明细后重新签收。</p>
          <Input.TextArea
            rows={3}
            placeholder="请输入回退原因，例如：车辆装不下，需要减少本次装车卷数"
            onChange={(event) => {
              reason = event.target.value
            }}
          />
        </div>
      ),
      okText: '确认回退',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => {
        const value = reason.trim()
        if (!value) {
          message.warning('请填写回退原因')
          return Promise.reject(new Error('reason required'))
        }
        resolve(value)
      },
      onCancel: () => reject(new Error('cancel')),
    })
  })
}

function confirmRemove(finishRollNo: string) {
  return new Promise<void>((resolve, reject) => {
    Modal.confirm({
      title: '移出出库明细',
      content: `确认将 ${finishRollNo || '该成品卷'} 从本张待出库单中移出？移出后可重新勾选出库。`,
      okText: '移出',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => resolve(),
      onCancel: () => reject(new Error('cancel')),
    })
  })
}
