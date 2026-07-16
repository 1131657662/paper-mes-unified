import { INVOICE_TYPE } from '../../constants/settle'
import { formatKg, formatMoney, formatTon } from '../../features/settle/utils/settleFormatters'
import type { SettleDetailVO, SettleFeeLine, SettlePrintLine } from '../../types/settle'
import { formatGram, formatMm } from '../../utils/numberFormatters'
import { buildSettleBillGroups, type SettleBillGroup } from './settleBillGroups'
import '../documentModule.css'

interface Props {
  detail: SettleDetailVO
}

export default function SettlePrintSheet({ detail }: Props) {
  const { order, printLines = [] } = detail
  const groups = buildSettleBillGroups(printLines)

  return (
    <div className="document-print-area document-print-area--settle">
      <div className="document-print-sheet document-print-sheet--settle">
        <header className="document-print-sheet__header">
          <div>
            <h1>客户结算单</h1>
            <div className="document-print-sheet__meta">
              <span>结算单号：{order.settleNo}</span>
              <span>客户：{order.customerName}</span>
              <span>结算日期：{order.settleDate}</span>
              <span>开票：{INVOICE_TYPE[order.isInvoice] || '-'}</span>
            </div>
          </div>
          <div className="document-print-sheet__summary">
            <span>应收：{formatMoney(order.totalAmount)}</span>
            <span>累计结清：{formatMoney(order.receivedAmount)}</span>
            <span>现金到账：{formatMoney(order.cashReceivedAmount)}</span>
            <span>废纸抵扣：{formatMoney(order.scrapOffsetAmount)}</span>
            <span>优惠核销：{formatMoney(order.discountAmount)}</span>
            <span>未收：{formatMoney(order.unreceivedAmount)}</span>
          </div>
        </header>

        <section>
          <h2>结算明细</h2>
          {groups.map((group) => <PrintGroup group={group} key={group.key} />)}
        </section>

        <footer className="document-print-sheet__footer document-print-sheet__footer--settle">
          <span>未税金额：{formatMoney(order.amountNoTax)}</span>
          <span>额外费用：{formatMoney(order.extraAmount)}</span>
          <span>税费：{formatMoney(order.taxAmount)}</span>
          <strong>应收合计：{formatMoney(order.totalAmount)}</strong>
          <span>客户确认：</span>
          <span>经办：</span>
        </footer>
      </div>
    </div>
  )
}

function PrintGroup({ group }: { group: SettleBillGroup }) {
  return (
    <div className="document-print-group">
      <div className="document-print-group__head">
        <strong>{group.orderNo}</strong>
        {group.orderDate && <span>日期：{group.orderDate}</span>}
        <span>原纸：{group.lines.length} 卷 / {formatTon(group.originalWeight)}</span>
        <span>成品：{group.finishCount} 卷 / {formatTon(group.finishWeight)}</span>
      </div>
      <table className="document-print-table document-print-table--settle">
        <thead>
          <tr>
            <th>原纸</th>
            <th>品名/规格</th>
            <th>原纸重量</th>
            <th>加工项目</th>
            <th>计费依据</th>
            <th>成品结果</th>
            <th>切边</th>
            <th>加工费</th>
          </tr>
        </thead>
        <tbody>{group.lines.map((line) => <PrintLine line={line} key={`${line.orderUuid}-${line.originalUuid}`} />)}</tbody>
      </table>
      <GroupTotals group={group} />
    </div>
  )
}

function PrintLine({ line }: { line: SettlePrintLine }) {
  return (
    <tr>
      <td>{originalLabel(line)}</td>
      <td>{originalSpec(line)}</td>
      <td>{formatKg(line.originalWeight)}</td>
      <td>{processNames(line)}</td>
      <td><PrintFeeBasis line={line} /></td>
      <td>{finishResult(line)}</td>
      <td>{trimText(line)}</td>
      <td><strong>{formatMoney(line.processAmount)}</strong></td>
    </tr>
  )
}

function PrintFeeBasis({ line }: { line: SettlePrintLine }) {
  const fees = processFees(line)
  if (fees.length === 0) return <span>{line.processStepSummary || line.processText || '-'}</span>
  return (
    <span className="settle-print-basis">
      {fees.map((fee, index) => (
        <span key={`${fee.feeType}-${fee.stageLevel ?? 'x'}-${index}`}>
          {feeFormula(fee)} = {formatMoney(fee.amountNoTax)}
        </span>
      ))}
    </span>
  )
}

function GroupTotals({ group }: { group: SettleBillGroup }) {
  return (
    <div className="document-print-group__totals">
      <span>加工费：{formatMoney(group.processAmount)}</span>
      <span>额外费：{formatMoney(group.extraAmount)}{group.extraFeeSummary ? `（${group.extraFeeSummary}）` : ''}</span>
      <span>税费：{formatMoney(group.taxAmount)}</span>
      <strong>本单应收：{formatMoney(group.lineAmount)}</strong>
    </div>
  )
}

function originalLabel(line: SettlePrintLine) {
  return [line.originalLabel || '-', line.originalRollNo && line.originalRollNo !== line.originalLabel && `卷号 ${line.originalRollNo}`, line.originalExtraNo && `编号 ${line.originalExtraNo}`]
    .filter(Boolean).join(' / ')
}

function originalSpec(line: SettlePrintLine) {
  const gram = line.actualGramWeight ?? line.gramWeight
  const width = line.actualWidth ?? line.originalWidth
  return [line.paperName || '-', gram == null ? undefined : formatGram(gram), width == null ? undefined : formatMm(width)]
    .filter(Boolean).join(' / ')
}

function processNames(line: SettlePrintLine) {
  const names = Array.from(new Set(processFees(line).map((fee) => fee.feeName.replace(/费$/, ''))))
  return names.join(' / ') || line.processText || '-'
}

function processFees(line: SettlePrintLine) {
  return (line.feeLines ?? []).filter((fee) => fee.feeType === 'saw' || fee.feeType === 'rewind')
}

function feeFormula(fee: SettleFeeLine) {
  if (fee.formulaText) return fee.formulaText
  if (fee.quantity == null || fee.unitPrice == null) return fee.feeName
  return `${fee.quantity}${fee.quantityUnit || ''} × ${formatMoney(fee.unitPrice)}`
}

function finishResult(line: SettlePrintLine) {
  const summary = `${line.finishCount ?? 0} 卷 / ${formatKg(line.finishWeight)}`
  return line.finishSummary && line.finishSummary !== '-' ? `${summary}（${line.finishSummary}）` : summary
}

function trimText(line: SettlePrintLine) {
  return line.trimSummary && line.trimSummary !== '-' ? line.trimSummary : formatKg(line.trimWeight)
}
