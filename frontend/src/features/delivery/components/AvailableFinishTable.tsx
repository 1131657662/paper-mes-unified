import { Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { AvailableFinishVO } from '../../../types/delivery'
import { SOURCE_TYPE } from '../../../constants/delivery'
import { availableFinishWeight, finishSpecText, formatKg, settleText } from '../utils/deliveryFormatters'

interface Props {
  data: AvailableFinishVO[]
  loading: boolean
  selectedRowKeys: React.Key[]
  onSelectionChange: (keys: React.Key[]) => void
}

export default function AvailableFinishTable({
  data,
  loading,
  onSelectionChange,
  selectedRowKeys,
}: Props) {
  return (
    <Table<AvailableFinishVO>
      className="delivery-stock-table mes-table-card"
      rowKey="finishUuid"
      size="small"
      loading={loading}
      columns={columns}
      dataSource={data}
      pagination={false}
      rowSelection={{ selectedRowKeys, onChange: onSelectionChange }}
      rowClassName={(record) => (selectedRowKeys.includes(record.finishUuid) ? 'is-selected' : '')}
      onRow={(record) => ({
        onClick: () => toggleKey(record.finishUuid, selectedRowKeys, onSelectionChange),
      })}
      scroll={{ x: 1010, y: 'calc(100vh - 510px)' }}
    />
  )
}

const columns: ColumnsType<AvailableFinishVO> = [
  {
    title: '成品卷号',
    dataIndex: 'finishRollNo',
    fixed: 'left',
    width: 130,
    render: (value) => <Typography.Text strong>{value}</Typography.Text>,
  },
  {
    title: '加工单',
    dataIndex: 'orderNo',
    width: 150,
    render: (value, record) => (
      <div className="delivery-cell-stack mes-cell-stack">
        <Typography.Text>{value}</Typography.Text>
        <span>{record.orderDate ?? '-'}</span>
      </div>
    ),
  },
  {
    title: '品名/规格',
    dataIndex: 'paperName',
    width: 220,
    render: (value, record) => (
      <div className="delivery-cell-stack mes-cell-stack">
        <Typography.Text>{value}</Typography.Text>
        <span>{finishSpecText(record)}</span>
      </div>
    ),
  },
  {
    title: '重量',
    dataIndex: 'actualWeight',
    align: 'right',
    width: 140,
    render: (_, record) => (
      <div className="delivery-cell-stack mes-cell-stack">
        <Typography.Text>{formatKg(record.actualWeight)}</Typography.Text>
        <span>可出库 {formatKg(availableFinishWeight(record))}</span>
      </div>
    ),
  },
  {
    title: '来源',
    dataIndex: 'sourceType',
    width: 105,
    render: (value) => {
      const source = SOURCE_TYPE[value]
        return source ? <Tag className="mes-status-tag" color={source.color}>{source.text}</Tag> : '-'
    },
  },
  {
    title: '结算/开票',
    dataIndex: 'settleType',
    width: 150,
    render: (_, record) => (
      <div className="delivery-cell-stack mes-cell-stack">
        <span>{settleText(record.settleType, record.settleDay)}</span>
        <span>{record.isInvoice === 1 ? '开票' : '不开票'}</span>
      </div>
    ),
  },
  {
    title: '风险',
    dataIndex: 'settlementRisk',
    width: 110,
    render: (value) => (
      value
        ? <Tag className="mes-status-tag" color="orange">待收款确认</Tag>
        : <Tag className="mes-status-tag">正常</Tag>
    ),
  },
]

function toggleKey(
  key: string,
  selectedRowKeys: React.Key[],
  onSelectionChange: (keys: React.Key[]) => void,
) {
  if (selectedRowKeys.includes(key)) {
    onSelectionChange(selectedRowKeys.filter((item) => item !== key))
    return
  }
  onSelectionChange([...selectedRowKeys, key])
}
