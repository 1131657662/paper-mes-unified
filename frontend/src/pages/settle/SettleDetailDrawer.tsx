import { useEffect, useState } from 'react'
import { Descriptions, Drawer, Spin, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { getSettleOrderDetail } from '../../api/settle'
import DocumentDetailTable from '../../components/biz/DocumentDetailTable'
import type { SettleDetail, SettleDetailVO, ReceiveRecord } from '../../types/settle'
import { SETTLE_STATUS, SETTLE_TYPE, PAY_METHOD, INVOICE_TYPE } from '../../constants/settle'

interface Props {
  uuid: string | null
  open: boolean
  onClose: () => void
}

export default function SettleDetailDrawer({ uuid, open, onClose }: Props) {
  const [loading, setLoading] = useState(false)
  const [detail, setDetail] = useState<SettleDetailVO | null>(null)

  useEffect(() => {
    if (open && uuid) {
      loadDetail()
    } else {
      setDetail(null)
    }
  }, [open, uuid])

  const loadDetail = async () => {
    if (!uuid) return
    setLoading(true)
    try {
      const res = await getSettleOrderDetail(uuid)
      setDetail(res)
    } finally {
      setLoading(false)
    }
  }

  const detailColumns: ColumnsType<SettleDetail> = [
    {
      title: '加工单号',
      dataIndex: 'orderNo',
      fixed: 'left',
      width: 140,
    },
    {
      title: '锯纸费',
      dataIndex: 'sawAmount',
      width: 100,
    },
    {
      title: '复卷费',
      dataIndex: 'rewindAmount',
      width: 100,
    },
    {
      title: '额外费用',
      dataIndex: 'extraAmount',
      width: 100,
    },
    {
      title: '本单金额',
      dataIndex: 'orderAmount',
      width: 100,
    },
  ]

  const receiveColumns: ColumnsType<ReceiveRecord> = [
    {
      title: '收款时间',
      dataIndex: 'receiveDate',
      fixed: 'left',
      width: 160,
    },
    {
      title: '收款金额',
      dataIndex: 'receiveAmount',
      width: 100,
    },
    {
      title: '收款方式',
      dataIndex: 'payMethod',
      width: 80,
      render: (v) => PAY_METHOD[v] || '-',
    },
    {
      title: '流水号',
      dataIndex: 'payNo',
      width: 120,
      render: (v) => v || '-',
    },
    {
      title: '经办人',
      dataIndex: 'operator',
      width: 80,
      render: (v) => v || '-',
    },
    {
      title: '备注',
      dataIndex: 'remark',
      width: 150,
      render: (v) => v || '-',
    },
  ]

  const order = detail?.order

  return (
    <Drawer
      title="结算单详情"
      width={960}
      open={open}
      onClose={onClose}
      destroyOnHidden
      className="mes-detail-drawer"
    >
      <Spin spinning={loading}>
        {order && (
          <div className="mes-drawer-content">
            <section className="mes-drawer-section">
              <Descriptions title="结算单信息" column={2} bordered size="small">
                <Descriptions.Item label="结算单号">{order.settleNo}</Descriptions.Item>
                <Descriptions.Item label="客户名称">{order.customerName}</Descriptions.Item>
                <Descriptions.Item label="结算类型">
                  {SETTLE_TYPE[order.settleType] || '-'}
                </Descriptions.Item>
                <Descriptions.Item label="结算状态">
                  {order.settleStatus && SETTLE_STATUS[order.settleStatus] ? (
                    <Tag color={SETTLE_STATUS[order.settleStatus].color}>
                      {SETTLE_STATUS[order.settleStatus].text}
                    </Tag>
                  ) : (
                    '-'
                  )}
                </Descriptions.Item>
                <Descriptions.Item label="结算日期">{order.settleDate}</Descriptions.Item>
                {order.settleType === 2 && (
                  <Descriptions.Item label="账期">
                    {order.periodStart} ~ {order.periodEnd}
                  </Descriptions.Item>
                )}
                <Descriptions.Item label="锯纸费">{order.sawAmount}</Descriptions.Item>
                <Descriptions.Item label="复卷费">{order.rewindAmount}</Descriptions.Item>
                <Descriptions.Item label="额外费用">{order.extraAmount}</Descriptions.Item>
                <Descriptions.Item label="未税金额">{order.amountNoTax}</Descriptions.Item>
                <Descriptions.Item label="开票加价">{order.taxAmount}</Descriptions.Item>
                <Descriptions.Item label="应收总额">{order.totalAmount}</Descriptions.Item>
                <Descriptions.Item label="已收金额">{order.receivedAmount}</Descriptions.Item>
                <Descriptions.Item label="未收金额">{order.unreceivedAmount}</Descriptions.Item>
                <Descriptions.Item label="是否开票">
                  {INVOICE_TYPE[order.isInvoice] || '-'}
                </Descriptions.Item>
                <Descriptions.Item label="备注" span={2}>
                  {order.remark || '-'}
                </Descriptions.Item>
              </Descriptions>
            </section>

            <section className="mes-drawer-section">
              <div className="mes-drawer-section__head">
                <div>
                  <h3>加工单明细</h3>
                  <p>本张结算单包含的加工单费用组成。</p>
                </div>
              </div>
              <div className="mes-drawer-table">
                <DocumentDetailTable
                  storageKey="settle-detail-drawer-fees"
                  rowKey="uuid"
                  columns={detailColumns}
                  dataSource={detail.details}
                  onReload={loadDetail}
                  pagination={false}
                  scroll={{ x: 'max-content' }}
                />
              </div>
            </section>

            <section className="mes-drawer-section">
              <div className="mes-drawer-section__head">
                <div>
                  <h3>收款记录</h3>
                  <p>登记过的收款流水和经办信息。</p>
                </div>
              </div>
              <div className="mes-drawer-table">
                <DocumentDetailTable
                  storageKey="settle-detail-drawer-receives"
                  rowKey="uuid"
                  columns={receiveColumns}
                  dataSource={detail.receives}
                  onReload={loadDetail}
                  pagination={false}
                  scroll={{ x: 'max-content' }}
                />
              </div>
            </section>
          </div>
        )}
      </Spin>
    </Drawer>
  )
}
