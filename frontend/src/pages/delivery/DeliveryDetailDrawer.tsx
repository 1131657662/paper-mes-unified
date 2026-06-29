import { useEffect, useState } from 'react'
import { Descriptions, Drawer, Spin, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { getDeliveryOrderDetail } from '../../api/delivery'
import type { DeliveryDetail, DeliveryDetailVO } from '../../types/delivery'
import { DELIVERY_STATUS } from '../../constants/delivery'

interface Props {
  uuid: string | null
  open: boolean
  onClose: () => void
}

export default function DeliveryDetailDrawer({ uuid, open, onClose }: Props) {
  const [loading, setLoading] = useState(false)
  const [detail, setDetail] = useState<DeliveryDetailVO | null>(null)

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
      const res = await getDeliveryOrderDetail(uuid)
      setDetail(res)
    } finally {
      setLoading(false)
    }
  }

  const columns: ColumnsType<DeliveryDetail> = [
    {
      title: '成品卷号',
      dataIndex: 'finishRollNo',
      width: 120,
    },
    {
      title: '品名',
      dataIndex: 'paperName',
      width: 120,
    },
    {
      title: '出库重量(kg)',
      dataIndex: 'outWeight',
      width: 100,
    },
    {
      title: '备注',
      dataIndex: 'remark',
      width: 200,
    },
  ]

  const order = detail?.order

  return (
    <Drawer
      title="出库单详情"
      width={900}
      open={open}
      onClose={onClose}
      destroyOnClose
      className="mes-detail-drawer"
    >
      <Spin spinning={loading}>
        {order && (
          <div className="mes-drawer-content">
            <section className="mes-drawer-section">
              <Descriptions title="出库单信息" column={2} bordered size="small">
                <Descriptions.Item label="出库单号">{order.deliveryNo}</Descriptions.Item>
                <Descriptions.Item label="客户名称">{order.customerName}</Descriptions.Item>
                <Descriptions.Item label="状态">
                  {order.deliveryStatus && DELIVERY_STATUS[order.deliveryStatus] ? (
                    <Tag color={DELIVERY_STATUS[order.deliveryStatus].color}>
                      {DELIVERY_STATUS[order.deliveryStatus].text}
                    </Tag>
                  ) : (
                    '-'
                  )}
                </Descriptions.Item>
                <Descriptions.Item label="出库日期">{order.deliveryDate}</Descriptions.Item>
                <Descriptions.Item label="总件数">{order.totalCount}</Descriptions.Item>
                <Descriptions.Item label="总重量(kg)">{order.totalWeight}</Descriptions.Item>
                <Descriptions.Item label="提货人">{order.pickerName || '-'}</Descriptions.Item>
                <Descriptions.Item label="车牌号">{order.carNo || '-'}</Descriptions.Item>
                <Descriptions.Item label="柜号">{order.containerNo || '-'}</Descriptions.Item>
                {order.deliveryStatus === 2 && (
                  <>
                    <Descriptions.Item label="签收人">{order.signUser || '-'}</Descriptions.Item>
                    <Descriptions.Item label="签收时间">{order.signTime || '-'}</Descriptions.Item>
                  </>
                )}
                {order.settleBlockAction === 1 && (
                  <Descriptions.Item label="现结拦截">
                    <Tag color="orange">警告放行</Tag>
                  </Descriptions.Item>
                )}
                <Descriptions.Item label="备注" span={2}>
                  {order.remark || '-'}
                </Descriptions.Item>
              </Descriptions>
            </section>

            <section className="mes-drawer-section">
              <div className="mes-drawer-section__head">
                <div>
                  <h3>出库明细</h3>
                  <p>本次出库关联的成品卷与出库重量。</p>
                </div>
              </div>
              <div className="mes-drawer-table">
                <Table
                  rowKey="uuid"
                  size="small"
                  columns={columns}
                  dataSource={detail.details}
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
