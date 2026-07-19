import { Button, Space, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PAY_METHOD, RECEIVE_TYPE } from '../../constants/settle'
import TooltipText from '../../components/biz/TooltipText'
import { formatMoney } from '../../features/settle/utils/settleFormatters'
import type { ReceiveRecord, SettleDetail } from '../../types/settle'
import { formatTrimmedNumber } from '../../utils/numberFormatters'
import { formatDateTime } from '../../utils/dateTime'

export { settlePrintLineColumns } from './settlePrintLineColumns'

export function buildSettleDetailColumns(extraFeeByOrder: Record<string, string> = {}): ColumnsType<SettleDetail> {
  return [
    { title: '加工单号', dataIndex: 'orderNo', fixed: 'left', width: 150 },
    { title: '锯纸费', dataIndex: 'sawAmount', align: 'right', width: 110, render: formatMoney },
    { title: '复卷费', dataIndex: 'rewindAmount', align: 'right', width: 110, render: formatMoney },
    {
      title: '计价调整',
      dataIndex: 'pricingAdjustmentAmount',
      align: 'right',
      width: 180,
      render: (value, record) => value ? amountWithHint({ amount: value, hint: record.pricingAdjustmentReason }) : '-',
    },
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
    { title: '收款时间', dataIndex: 'receiveDate', fixed: 'left', width: 170, render: formatDateTime },
    { title: '类型', dataIndex: 'receiveType', width: 100, render: receiveTypeCell },
    {
      title: '本次结清',
      dataIndex: 'receiveAmount',
      align: 'right',
      width: 120,
      render: (value, record) => (
        <Typography.Text delete={record.recordStatus === 2}>{formatMoney(value)}</Typography.Text>
      ),
    },
    { title: '结清构成', key: 'breakdown', width: 250, render: (_, record) => receiveBreakdown(record) },
    { title: '支付信息', key: 'payment', width: 180, render: (_, record) => paymentText(record) },
    { title: '经办人', dataIndex: 'operator', width: 100, render: textCell },
    { title: '状态', dataIndex: 'recordStatus', width: 96, render: statusCell },
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
      render: (_, record) => record.recordStatus !== 2 && (
        <Space size={8}>
          <Button
            danger
            size="small"
            type="link"
            loading={options.cancelLoading}
            onClick={() => options.onCancelReceive?.(record)}
          >
            撤销
          </Button>
        </Space>
      ),
    })
  }

  return columns
}

function receiveTypeCell(value?: number) {
  const item = value ? RECEIVE_TYPE[value] : RECEIVE_TYPE[1]
  return <Tag className="mes-status-tag" color={item?.color ?? 'blue'}>{item?.text ?? '-'}</Tag>
}

function statusCell(value?: number) {
  return value === 2
    ? <Tag className="mes-status-tag" color="default">已撤销</Tag>
    : <Tag className="mes-status-tag" color="success">有效</Tag>
}

function textCell(value?: string | number) {
  return <TooltipText value={value} />
}

function numberText(value?: number) {
  if (value == null) return '-'
  return formatTrimmedNumber(value, 3)
}

function unitPriceText(value?: number) {
  if (value == null || Number(value) <= 0) return '-'
  return `${formatTrimmedNumber(value, 4)} 元/kg`
}

function receiveBreakdown(record: ReceiveRecord) {
  const scrapDetail = record.scrapWeight && record.scrapUnitPrice
    ? `（${numberText(record.scrapWeight)} kg × ${unitPriceText(record.scrapUnitPrice)}）`
    : ''
  return <div className="document-money-stack">
    <span>到账 {formatMoney(record.cashAmount)}</span>
    <span>废纸 {formatMoney(record.scrapOffsetAmount)}{scrapDetail}</span>
    <span>优惠 {formatMoney(record.discountAmount)}</span>
  </div>
}

function paymentText(record: ReceiveRecord) {
  return <div className="document-money-stack">
    <span>{record.payMethod ? PAY_METHOD[record.payMethod] || '-' : '-'}</span>
    <TooltipText value={record.payNo} />
  </div>
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
