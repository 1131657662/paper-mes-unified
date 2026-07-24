import { Empty, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { CustomerProcessPrice } from '../../types/customer'
import { formatMoney } from '../../features/processOrderDetail/orderDetailUtils'

export default function CustomerServicePriceSummary({ prices = [] }: { prices?: CustomerProcessPrice[] }) {
  if (!prices.length) return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂未配置附加工艺价格" />
  return <Table size="small" pagination={false} rowKey={(row) => `${row.catalogUuid}-${row.billingBasis}`}
    columns={columns} dataSource={prices} />
}

const columns: ColumnsType<CustomerProcessPrice> = [
  { title: '工艺', dataIndex: 'processName', width: 140 },
  { title: '计价方案', width: 130, render: (_, row) => basisLabel(row) },
  { title: '价格', width: 140, align: 'right', render: (_, row) => priceText(row) },
  { title: '使用顺序', width: 100, render: (_, row) => row.defaultOption ? <Tag color="blue">默认</Tag> : <Tag>备选</Tag> },
]

function basisLabel(row: CustomerProcessPrice) {
  if (row.billingBasis === 'PIECE') return '按件'
  if (row.billingBasis === 'TON') return '按吨'
  return '固定金额'
}

function priceText(row: CustomerProcessPrice) {
  if (row.billingBasis === 'FIXED') return `${formatMoney(row.price)} / 单`
  return `${formatMoney(row.price)} / ${row.billingUnitName ?? '-'}`
}
