import { Table, Tag, Tooltip, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import TooltipText from '../../../components/biz/TooltipText'
import type { SettleCandidateVO } from '../../../types/settle'
import { formatMoney, formatTon, settleModeText } from '../utils/settleFormatters'
import { isSelectableCandidate } from '../../../pages/settle/settleCandidateSelectionModel'

interface Props {
  data: SettleCandidateVO[]
  emptyText?: React.ReactNode
  loading: boolean
  lockedCustomerUuid?: string
  selectable?: boolean
  selectedRowKeys: React.Key[]
  scrollY?: number | string
  onSelectionChange: (keys: React.Key[]) => void
}

export default function SettleCandidateTable({
  data,
  emptyText,
  loading,
  lockedCustomerUuid,
  onSelectionChange,
  selectable = true,
  scrollY = '100%',
  selectedRowKeys,
}: Props) {
  const activeSelectedKeys = selectable ? selectedRowKeys : []

  return (
    <Table<SettleCandidateVO>
      className="mes-table-card"
      rowKey="orderUuid"
      size="small"
      loading={loading}
      columns={columns}
      dataSource={data}
      locale={emptyText ? { emptyText } : undefined}
      pagination={false}
      rowSelection={selectable ? {
        selectedRowKeys: activeSelectedKeys,
        onChange: onSelectionChange,
        getCheckboxProps: (record) => ({
          disabled: !isSelectableCandidate(record, lockedCustomerUuid),
        }),
        preserveSelectedRowKeys: true,
      } : undefined}
      rowClassName={(record) => (activeSelectedKeys.includes(record.orderUuid) ? 'is-selected' : '')}
      onRow={(record) => selectable ? ({
        onClick: (event) => {
          if (isSelectionControlTarget(event.target)) return
          toggleKey(record, activeSelectedKeys, onSelectionChange, lockedCustomerUuid)
        },
      }) : {}}
      scroll={{ x: 1437, y: scrollY }}
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
        <span>制单 {record.orderDate ?? '-'}</span>
      </div>
    ),
  },
  { title: '归属日期', dataIndex: 'accountingDate', width: 112, render: textCell },
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
        <span>{formatTon(record.originalRollWeight)}</span>
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
        <span>{formatTon(record.finishRollWeight)}</span>
      </div>
    ),
  },
  { title: '锯纸费', dataIndex: 'sawAmount', align: 'right', width: 105, render: (value) => formatMoney(value) },
  { title: '复卷费', dataIndex: 'rewindAmount', align: 'right', width: 105, render: (value) => formatMoney(value) },
  { title: '服务工序费', dataIndex: 'serviceAmount', align: 'right', width: 120, render: (value) => formatMoney(value) },
  {
    title: '计价调整',
    dataIndex: 'pricingAdjustmentAmount',
    align: 'right',
    width: 125,
    render: (value, record) => Number(value ?? 0) !== 0
      ? <Tooltip title={`标准加工费 ${formatMoney(record.standardProcessAmount)}`}>
        <Tag color={Number(value) < 0 ? 'gold' : 'blue'}>{formatMoney(value)}</Tag>
      </Tooltip>
      : <Typography.Text type="secondary">-</Typography.Text>,
  },
  { title: '额外费', dataIndex: 'extraAmount', align: 'right', width: 105, render: (value) => formatMoney(value) },
  {
    title: '应收',
    dataIndex: 'totalAmount',
    align: 'right',
    fixed: 'right',
    width: 125,
    render: (value) => Number(value ?? 0) > 0
      ? <Typography.Text strong>{formatMoney(value)}</Typography.Text>
      : <Tag className="mes-status-tag" color="warning">待核价</Tag>,
  },
]

function textCell(value?: string | number) {
  return <TooltipText value={value} />
}

function isSelectionControlTarget(target: EventTarget | null): boolean {
  return target instanceof HTMLElement
    && Boolean(target.closest('.ant-checkbox-wrapper, .ant-checkbox, input[type="checkbox"]'))
}

function toggleKey(
  record: SettleCandidateVO,
  selectedRowKeys: React.Key[],
  onSelectionChange: (keys: React.Key[]) => void,
  lockedCustomerUuid?: string,
) {
  if (!isSelectableCandidate(record, lockedCustomerUuid)) return
  const key = record.orderUuid
  if (selectedRowKeys.includes(key)) {
    onSelectionChange(selectedRowKeys.filter((item) => item !== key))
    return
  }
  onSelectionChange([...selectedRowKeys, key])
}
