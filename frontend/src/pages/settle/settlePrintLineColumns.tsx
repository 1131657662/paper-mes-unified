import { Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import TooltipText from '../../components/biz/TooltipText'
import { INVOICE_TYPE } from '../../constants/settle'
import { formatKg, formatMoney } from '../../features/settle/utils/settleFormatters'
import type { SettlePrintLine } from '../../types/settle'
import { formatGram, formatMm, formatUnit } from '../../utils/numberFormatters'
import { SettleFeeSourceCell } from './SettleFeeLinesView'

export const settlePrintLineColumns: ColumnsType<SettlePrintLine> = [
  { title: '加工单', dataIndex: 'orderNo', fixed: 'left', width: 145 },
  {
    title: '原纸',
    dataIndex: 'originalLabel',
    fixed: 'left',
    width: 170,
    render: (_, record) => (
      <div className="settle-cell-stack mes-cell-stack">
        <Typography.Text strong>{record.originalLabel || '-'}</Typography.Text>
        <span>{originalIdentity(record)}</span>
      </div>
    ),
  },
  {
    title: '品名/规格',
    dataIndex: 'paperName',
    width: 210,
    render: (_, record) => (
      <div className="settle-cell-stack mes-cell-stack">
        <span>{record.paperName || '-'}</span>
        <span>{originalSpec(record)}</span>
      </div>
    ),
  },
  { title: '原纸重量', dataIndex: 'originalWeight', align: 'right', width: 110, render: formatKg },
  { title: '加工内容', dataIndex: 'processText', width: 220, render: (_, record) => processCell(record) },
  {
    title: '成品摘要',
    dataIndex: 'finishSummary',
    width: 260,
    render: (_, record) => textCell(record.finishDetailSummary || record.finishSummary),
  },
  { title: '成品数', dataIndex: 'finishCount', align: 'right', width: 86 },
  { title: '成品重量', dataIndex: 'finishWeight', align: 'right', width: 110, render: formatKg },
  {
    title: '切边',
    dataIndex: 'trimWeight',
    align: 'right',
    width: 130,
    render: (_, record) => amountWithHint({ amount: record.trimWeight, formatter: formatKg, hint: record.trimSummary }),
  },
  {
    title: '锯纸单价',
    dataIndex: 'sawUnitPrice',
    align: 'right',
    width: 130,
    render: (_, record) => unitPriceCell({ invoicePrice: record.sawInvoiceUnitPrice, price: record.sawUnitPrice }),
  },
  { title: '锯纸费', dataIndex: 'sawAmount', align: 'right', width: 105, render: formatMoney },
  {
    title: '复卷单价',
    dataIndex: 'rewindUnitPrice',
    align: 'right',
    width: 130,
    render: (_, record) => unitPriceCell({ invoicePrice: record.rewindInvoiceUnitPrice, price: record.rewindUnitPrice }),
  },
  { title: '复卷费', dataIndex: 'rewindAmount', align: 'right', width: 105, render: formatMoney },
  { title: '加工费', dataIndex: 'processAmount', align: 'right', width: 110, render: formatMoney },
  { title: '费用来源', dataIndex: 'feeLines', width: 300, render: (_, record) => <SettleFeeSourceCell feeLines={record.feeLines} /> },
  {
    title: '额外费',
    dataIndex: 'extraAmount',
    align: 'right',
    width: 180,
    render: (_, record) => amountWithHint({ amount: record.extraAmount, hint: record.extraFeeSummary }),
  },
  { title: '开票', dataIndex: 'isInvoice', width: 96, render: (value, record) => invoiceCell(value, record.taxRate) },
  {
    title: '应收合计',
    dataIndex: 'lineAmount',
    align: 'right',
    width: 120,
    render: (value) => <Typography.Text strong>{formatMoney(value)}</Typography.Text>,
  },
]

function textCell(value?: string | number) {
  return <TooltipText value={value} />
}

function processCell(record: SettlePrintLine) {
  return (
    <div className="settle-cell-stack mes-cell-stack">
      <TooltipText value={record.processStepSummary || record.processText} />
      {record.machineName && <span>机台 {record.machineName}</span>}
    </div>
  )
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

function invoiceCell(value?: number, taxRate?: number) {
  return (
    <div className="settle-cell-stack mes-cell-stack">
      <Tag className="mes-status-tag">{INVOICE_TYPE[value ?? 0] || '-'}</Tag>
      {value === 1 && taxRate != null && <span>税点 {taxRate}%</span>}
    </div>
  )
}

function originalIdentity(record: SettlePrintLine) {
  const parts = [
    record.originalRollNo && `卷号${record.originalRollNo}`,
    record.originalExtraNo && `编号${record.originalExtraNo}`,
  ].filter(Boolean)
  return parts.length ? parts.join(' / ') : '无卷号'
}

function originalSpec(record: SettlePrintLine) {
  const parts = [
    record.actualGramWeight ? formatGram(record.actualGramWeight) : record.gramWeight ? formatGram(record.gramWeight) : undefined,
    record.actualWidth ? formatMm(record.actualWidth) : record.originalWidth ? formatMm(record.originalWidth) : undefined,
    record.originalDiameter ? `直径 ${formatMm(record.originalDiameter)}` : undefined,
    record.coreDiameter ? `纸芯 ${formatMm(record.coreDiameter)}` : undefined,
    record.originalLength ? formatUnit(record.originalLength, 'm') : undefined,
  ].filter(Boolean)
  return parts.length ? parts.join(' / ') : '-'
}

function unitPriceCell({ invoicePrice, price }: { invoicePrice?: number; price?: number }) {
  const showInvoice = invoicePrice != null && invoicePrice !== price
  return (
    <div className="document-money-stack">
      <Typography.Text>{formatMoney(price)}</Typography.Text>
      {showInvoice && <span>开票价 {formatMoney(invoicePrice)}</span>}
    </div>
  )
}
