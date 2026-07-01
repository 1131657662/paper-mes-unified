import { Button, Descriptions, Drawer, Input, Modal, Space, Spin, Tag, Typography, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { DELIVERY_STATUS, SOURCE_TYPE } from '../../constants/delivery'
import DocumentDetailTable from '../../components/biz/DocumentDetailTable'
import TooltipText from '../../components/biz/TooltipText'
import { useDeliveryDetail } from '../../features/delivery/hooks/useDeliveryDetail'
import { useExportDelivery } from '../../features/delivery/hooks/useExportDelivery'
import { useRemoveDeliveryDetail } from '../../features/delivery/hooks/useRemoveDeliveryDetail'
import { useRollbackDelivery } from '../../features/delivery/hooks/useRollbackDelivery'
import {
  deliveryDetailSpecText,
  deliveryOriginalSnapshotText,
  deliveryProcessSnapshotText,
  formatKg,
  formatTon,
} from '../../features/delivery/utils/deliveryFormatters'
import type { DeliveryDetail } from '../../types/delivery'

interface Props {
  uuid: string | null
  open: boolean
  onClose: () => void
  onChanged?: () => void
}

export default function DeliveryDetailDrawer({ uuid, open, onChanged, onClose }: Props) {
  const detailQuery = useDeliveryDetail(uuid ?? undefined, open)
  const exportMutation = useExportDelivery()
  const rollbackMutation = useRollbackDelivery()
  const removeDetailMutation = useRemoveDeliveryDetail()
  const detail = detailQuery.data
  const order = detail?.order

  const handleExport = async () => {
    if (!uuid) return
    await exportMutation.mutateAsync(uuid)
  }

  const handleRollback = async () => {
    if (!uuid || !order) return
    const reason = await askRollbackReason(order.deliveryNo)
    await rollbackMutation.mutateAsync({ uuid, data: { reason } })
    message.success('已回退为待出库，可继续改单')
    onChanged?.()
  }

  const handleRemove = async (record: DeliveryDetail) => {
    if (!uuid || !order) return
    await confirmRemove(record.finishRollNo)
    await removeDetailMutation.mutateAsync({ uuid, detailUuid: record.uuid })
    message.success('已从本张出库单移出')
    onChanged?.()
  }

  return (
    <Drawer
      title="出库单详情"
      width={1080}
      open={open}
      onClose={onClose}
      destroyOnHidden
      className="mes-detail-drawer"
      extra={order && (
        <Space>
          <Button onClick={handleExport} loading={exportMutation.isPending}>
            导出 Excel
          </Button>
          {order.deliveryStatus === 2 && (
            <Button danger onClick={handleRollback} loading={rollbackMutation.isPending}>
              回退签收
            </Button>
          )}
        </Space>
      )}
    >
      <Spin spinning={detailQuery.isLoading || detailQuery.isFetching}>
        {order && (
          <div className="mes-drawer-content">
            <section className="mes-drawer-section">
              <Descriptions title="出库单信息" column={2} bordered size="small">
                <Descriptions.Item label="出库单号">{order.deliveryNo}</Descriptions.Item>
                <Descriptions.Item label="客户名称">{order.customerName}</Descriptions.Item>
                <Descriptions.Item label="状态">
                  <DeliveryStatusTag status={order.deliveryStatus} />
                </Descriptions.Item>
                <Descriptions.Item label="出库日期">{order.deliveryDate}</Descriptions.Item>
                <Descriptions.Item label="总件数">{order.totalCount}</Descriptions.Item>
                <Descriptions.Item label="总重量">{formatTon(order.totalWeight)}</Descriptions.Item>
                <Descriptions.Item label="提货人">{order.pickerName || '-'}</Descriptions.Item>
                <Descriptions.Item label="车牌号">{order.carNo || '-'}</Descriptions.Item>
                <Descriptions.Item label="柜号">{order.containerNo || '-'}</Descriptions.Item>
                <Descriptions.Item label="签收人">{order.signUser || '-'}</Descriptions.Item>
                <Descriptions.Item label="签收时间">{order.signTime || '-'}</Descriptions.Item>
                <Descriptions.Item label="现结拦截">
                  {order.settleBlockAction === 1
                    ? <Tag className="mes-status-tag" color="orange">警告放行</Tag>
                    : '-'}
                </Descriptions.Item>
                <Descriptions.Item label="备注" span={2}>
                  {order.remark || '-'}
                </Descriptions.Item>
              </Descriptions>
            </section>

            <section className="mes-drawer-section">
              <div className="mes-drawer-section__head">
                <div>
                  <h3>出库明细</h3>
                  <p>包含成品卷规格、件重、来源原纸和本次出库重量。</p>
                </div>
              </div>
              <div className="mes-drawer-table">
                <DocumentDetailTable
                  storageKey="delivery-detail-drawer-items"
                  rowKey="uuid"
                  columns={columns(order.deliveryStatus, handleRemove)}
                  dataSource={detail.details}
                  onReload={() => detailQuery.refetch()}
                  pagination={false}
                  scroll={{ x: 1580 }}
                />
              </div>
            </section>
          </div>
        )}
      </Spin>
    </Drawer>
  )
}

function columns(
  deliveryStatus: number,
  onRemove: (record: DeliveryDetail) => void,
): ColumnsType<DeliveryDetail> {
  const result: ColumnsType<DeliveryDetail> = [
    { title: '加工单', dataIndex: 'orderNo', width: 145, render: textCell },
    {
      title: '卷号',
      dataIndex: 'finishRollNo',
      fixed: 'left',
      width: 125,
      render: (value) => <Typography.Text strong>{value || '-'}</Typography.Text>,
    },
    { title: '品名', dataIndex: 'paperName', width: 130, render: textCell },
    { title: '克重', dataIndex: 'gramWeight', width: 78, render: (value) => value ? `${value}g` : '-' },
    { title: '规格', key: 'spec', width: 155, render: (_, record) => deliveryDetailSpecText(record) },
    { title: '件重', dataIndex: 'actualWeight', align: 'right', width: 110, render: formatKg },
    { title: '出库重量', dataIndex: 'outWeight', align: 'right', width: 110, render: formatKg },
    {
      title: '来源',
      dataIndex: 'sourceType',
      width: 110,
      render: (value) => <SourceTag value={value} />,
    },
    {
      title: '原纸信息',
      dataIndex: 'originalSummary',
      width: 300,
      render: (_, record) => textCell(deliveryOriginalSnapshotText(record)),
    },
    {
      title: '工艺摘要',
      dataIndex: 'processSummary',
      width: 240,
      render: (_, record) => textCell(deliveryProcessSnapshotText(record)),
    },
    { title: '备注', dataIndex: 'remark', width: 150, render: textCell },
    { title: '回录备注', dataIndex: 'actualRemark', width: 150, render: textCell },
  ]
  if (deliveryStatus === 1) {
    result.push({
      title: '操作',
      key: 'actions',
      fixed: 'right',
      width: 86,
      render: (_, record) => (
        <Button type="link" size="small" danger onClick={() => onRemove(record)}>
          移出
        </Button>
      ),
    })
  }
  return result
}

function DeliveryStatusTag({ status }: { status?: number }) {
  const item = status ? DELIVERY_STATUS[status] : undefined
  return item ? <Tag className="mes-status-tag" color={item.color}>{item.text}</Tag> : <>-</>
}

function SourceTag({ value }: { value?: number }) {
  const item = value ? SOURCE_TYPE[value] : undefined
  return item ? <Tag className="mes-status-tag" color={item.color}>{item.text}</Tag> : <>-</>
}

function textCell(value?: string | number) {
  return <TooltipText value={value} />
}

function askRollbackReason(deliveryNo: string) {
  let reason = ''
  return new Promise<string>((resolve, reject) => {
    Modal.confirm({
      title: `回退签收 ${deliveryNo}`,
      content: (
        <div className="delivery-sign-modal">
          <p>回退后成品卷会恢复为已入库，出库单回到待出库状态，可移出装不下的明细后重新签收。</p>
          <Input.TextArea
            rows={3}
            placeholder="请输入回退原因，例如：车辆装不下，需减少本次装车卷数"
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
      content: `确认将 ${finishRollNo || '该成品卷'} 从本张待出库单中移出？不会删除成品卷，移出后可重新勾选出库。`,
      okText: '移出',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => resolve(),
      onCancel: () => reject(new Error('cancel')),
    })
  })
}
