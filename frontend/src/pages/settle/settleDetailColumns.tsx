import { Button, Space, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PAY_METHOD } from '../../constants/settle'
import TooltipText from '../../components/biz/TooltipText'
import { formatMoney } from '../../features/settle/utils/settleFormatters'
import type { ReceiveRecord, SettleDetail } from '../../types/settle'

export { settlePrintLineColumns } from './settlePrintLineColumns'

export function buildSettleDetailColumns(extraFeeByOrder: Record<string, string> = {}): ColumnsType<SettleDetail> {
  return [
    { title: '加工单号', dataIndex: 'orderNo', fixed: 'left', width: 150 },
    { title: '锯纸费', dataIndex: 'sawAmount', align: 'right', width: 110, render: formatMoney },
    { title: '复卷费', dataIndex: 'rewindAmount', align: 'right', width: 110, render: formatMoney },
    {
      title: '额外费用',
      dataIndex: 'extraAmount',
      align: 'right',
      width: 190,
      render: (value, record) => amountWithHint({ amount: value, hint: extraFeeByOrder[record.orderUuid] }),
    },
    {
      title: '本单金额',
      dataIndex: 'orderAmount',
      align: 'right',
      width: 120,
      render: (value) => <Typography.Text strong>{formatMoney(value)}</Typography.Text>,
    },
  ]
}

export const settleDetailColumns: ColumnsType<SettleDetail> = buildSettleDetailColumns()

export const receiveColumns: ColumnsType<ReceiveRecord> = buildReceiveColumns()

export function buildReceiveColumns(options: {
  cancelLoading?: boolean
  onCancelReceive?: (record: ReceiveRecord) => void
} = {}): ColumnsType<ReceiveRecord> {
  const columns: ColumnsType<ReceiveRecord> = [
    { title: '收款时间', dataIndex: 'receiveDate', fixed: 'left', width: 170 },
    {
      title: '收款金额',
      dataIndex: 'receiveAmount',
      align: 'right',
      width: 120,
      render: (value, record) => (
        <Typography.Text delete={record.recordStatus === 2}>{formatMoney(value)}</Typography.Text>
      ),
    },
    { title: '收款方式', dataIndex: 'payMethod', width: 90, render: (value) => PAY_METHOD[value] || '-' },
    { title: '流水号', dataIndex: 'payNo', width: 150, render: textCell },
    { title: '经办人', dataIndex: 'operator', width: 100, render: textCell },
    {
      title: '状态',
      dataIndex: 'recordStatus',
      width: 96,
      render: (value) => value === 2
        ? <Tag className="mes-status-tag" color="default">已撤销</Tag>
        : <Tag className="mes-status-tag" color="success">有效</Tag>,
    },
    {
      title: '备注/撤销原因',
      dataIndex: 'remark',
      width: 240,
      render: (_, record) => record.recordStatus === 2
        ? <TooltipText value={`撤销：${record.cancelReason || '-'}`} />
        : textCell(record.remark),
    },
  ]

  if (options.onCancelReceive) {
    columns.push({
      title: '操作',
      key: 'actions',
      fixed: 'right',
      width: 88,
      render: (_, record) => (
        <Space size={8}>
          {record.recordStatus !== 2 && (
          <Button
            danger
            size="small"
            type="link"
            loading={options.cancelLoading}
            onClick={() => options.onCancelReceive?.(record)}
          >
            撤销
          </Button>
        )}
        </Space>
      ),
    })
  }

  return columns
}

function textCell(value?: string | number) {
  return <TooltipText value={value} />
}

function amountWithHint({
  amount,
  formatter = formatMoney,
  hint,
}: {
  amount?: number
  formatter?: (value?: number) => string
  hint?: string
}) {
  return (
    <div className="document-money-stack">
      <Typography.Text strong>{formatter(amount)}</Typography.Text>
      {hint && <span>{hint}</span>}
    </div>
  )
}
