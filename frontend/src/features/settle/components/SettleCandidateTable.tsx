import { Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import TooltipText from '../../../components/biz/TooltipText'
import type { SettleCandidateVO } from '../../../types/settle'
import { formatKg, formatMoney, settleModeText } from '../utils/settleFormatters'

interface Props {
  data: SettleCandidateVO[]
  loading: boolean
  selectedRowKeys: React.Key[]
  scrollY?: number | string
  onSelectionChange: (keys: React.Key[]) => void
}

export default function SettleCandidateTable({
  data,
  loading,
  onSelectionChange,
  scrollY = 'calc(100vh - 520px)',
  selectedRowKeys,
}: Props) {
  return (
    <Table<SettleCandidateVO>
      className="mes-table-card"
      rowKey="orderUuid"
      size="small"
      loading={loading}
      columns={columns}
      dataSource={data}
      pagination={false}
      rowSelection={{ selectedRowKeys, onChange: onSelectionChange }}
      rowClassName={(record) => (selectedRowKeys.includes(record.orderUuid) ? 'is-selected' : '')}
      onRow={(record) => ({
        onClick: () => toggleKey(record.orderUuid, selectedRowKeys, onSelectionChange),
      })}
      scroll={{ x: 1080, y: scrollY }}
    />
  )
}

const columns: ColumnsType<SettleCandidateVO> = [
  {
    title: '加工单',
    dataIndex: 'orderNo',
    fixed: 'left',
    width: 155,
    render: (value, record) => (
      <div className="settle-cell-stack mes-cell-stack">
        <Typography.Text strong>{value}</Typography.Text>
        <span>{record.orderDate ?? '-'}</span>
      </div>
    ),
  },
  { title: '客户', dataIndex: 'customerName', width: 150, render: textCell },
  {
    title: '结算方式',
    dataIndex: 'settleType',
    width: 115,
    render: (_, record) => (
      <Tag className="mes-status-tag" color={record.settleType === 1 ? 'orange' : 'blue'}>
        {settleModeText(record.settleType, record.settleDay)}
      </Tag>
    ),
  },
  {
    title: '原卷',
    dataIndex: 'originalRollCount',
    width: 130,
    render: (_, record) => (
      <div className="settle-cell-stack mes-cell-stack">
        <span>{record.originalRollCount ?? 0} 卷</span>
        <span>{formatKg(record.originalRollWeight)}</span>
      </div>
    ),
  },
  {
    title: '成品',
    dataIndex: 'finishRollCount',
    width: 130,
    render: (_, record) => (
      <div className="settle-cell-stack mes-cell-stack">
        <span>{record.finishRollCount ?? 0} 卷</span>
        <span>{formatKg(record.finishRollWeight)}</span>
      </div>
    ),
  },
  { title: '锯纸费', dataIndex: 'sawAmount', align: 'right', width: 105, render: (value) => formatMoney(value) },
  { title: '复卷费', dataIndex: 'rewindAmount', align: 'right', width: 105, render: (value) => formatMoney(value) },
  { title: '额外费', dataIndex: 'extraAmount', align: 'right', width: 105, render: (value) => formatMoney(value) },
  { title: '应收', dataIndex: 'totalAmount', align: 'right', width: 115, render: (value) => <Typography.Text strong>{formatMoney(value)}</Typography.Text> },
]

function textCell(value?: string | number) {
  return <TooltipText value={value} />
}

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
