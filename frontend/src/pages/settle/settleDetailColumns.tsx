import { Button, Space, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { INVOICE_TYPE, PAY_METHOD } from '../../constants/settle'
import TooltipText from '../../components/biz/TooltipText'
import { formatKg, formatMoney } from '../../features/settle/utils/settleFormatters'
import type { ReceiveRecord, SettleDetail, SettlePrintLine } from '../../types/settle'

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
    render: (value, record) => <AmountWithHint amount={value} hint={extraFeeByOrder[record.orderUuid]} />,
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

export const settlePrintLineColumns: ColumnsType<SettlePrintLine> = [
  { title: '加工单', dataIndex: 'orderNo', fixed: 'left', width: 145 },
  { title: '原纸', dataIndex: 'originalLabel', fixed: 'left', width: 130 },
  {
    title: '品名/规格',
    dataIndex: 'paperName',
    width: 180,
    render: (_, record) => (
      <div className="settle-cell-stack mes-cell-stack">
        <span>{record.paperName || '-'}</span>
        <span>{record.gramWeight ? `${record.gramWeight}g` : '-'} / {record.originalWidth ? `${record.originalWidth}mm` : '-'}</span>
      </div>
    ),
  },
  { title: '原纸重量', dataIndex: 'originalWeight', align: 'right', width: 110, render: formatKg },
  { title: '加工内容', dataIndex: 'processText', width: 120, render: textCell },
  { title: '成品摘要', dataIndex: 'finishSummary', width: 210, render: textCell },
  { title: '成品数', dataIndex: 'finishCount', align: 'right', width: 86 },
  { title: '成品重量', dataIndex: 'finishWeight', align: 'right', width: 110, render: formatKg },
  { title: '切边', dataIndex: 'trimWeight', align: 'right', width: 100, render: formatKg },
  {
    title: '锯纸单价',
    dataIndex: 'sawUnitPrice',
    align: 'right',
    width: 130,
    render: (_, record) => (
      <UnitPriceCell invoicePrice={record.sawInvoiceUnitPrice} price={record.sawUnitPrice} />
    ),
  },
  { title: '锯纸费', dataIndex: 'sawAmount', align: 'right', width: 105, render: formatMoney },
  {
    title: '复卷单价',
    dataIndex: 'rewindUnitPrice',
    align: 'right',
    width: 130,
    render: (_, record) => (
      <UnitPriceCell invoicePrice={record.rewindInvoiceUnitPrice} price={record.rewindUnitPrice} />
    ),
  },
  { title: '复卷费', dataIndex: 'rewindAmount', align: 'right', width: 105, render: formatMoney },
  { title: '加工费', dataIndex: 'processAmount', align: 'right', width: 110, render: formatMoney },
  {
    title: '额外费',
    dataIndex: 'extraAmount',
    align: 'right',
    width: 180,
    render: (_, record) => <AmountWithHint amount={record.extraAmount} hint={record.extraFeeSummary} />,
  },
  { title: '开票加价', dataIndex: 'taxAmount', align: 'right', width: 110, render: formatMoney },
  {
    title: '开票',
    dataIndex: 'isInvoice',
    width: 82,
    render: (value) => <Tag className="mes-status-tag">{INVOICE_TYPE[value] || '-'}</Tag>,
  },
  {
    title: '应收合计',
    dataIndex: 'lineAmount',
    align: 'right',
    width: 120,
    render: (value) => <Typography.Text strong>{formatMoney(value)}</Typography.Text>,
  },
]

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

function AmountWithHint({ amount, hint }: { amount?: number; hint?: string }) {
  return (
    <div className="document-money-stack">
      <Typography.Text strong>{formatMoney(amount)}</Typography.Text>
      {hint && <span>{hint}</span>}
    </div>
  )
}

function UnitPriceCell({ invoicePrice, price }: { invoicePrice?: number; price?: number }) {
  const showInvoice = invoicePrice != null && invoicePrice !== price
  return (
    <div className="document-money-stack">
      <Typography.Text>{formatMoney(price)}</Typography.Text>
      {showInvoice && <span>开票 {formatMoney(invoicePrice)}</span>}
    </div>
  )
}
