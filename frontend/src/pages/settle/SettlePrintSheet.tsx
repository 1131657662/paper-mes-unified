import { INVOICE_TYPE } from '../../constants/settle'
import { formatKg, formatMoney } from '../../features/settle/utils/settleFormatters'
import type { SettleDetailVO } from '../../types/settle'
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
      <div className="document-print-sheet">
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
            <span>已收：{formatMoney(order.receivedAmount)}</span>
            <span>未收：{formatMoney(order.unreceivedAmount)}</span>
          </div>
        </header>

        <section>
          <h2>结算明细</h2>
          {groups.map((group) => <PrintGroup group={group} key={group.key} />)}
        </section>

        <footer className="document-print-sheet__footer">
          <span>锯纸费：{formatMoney(order.sawAmount)}</span>
          <span>复卷费：{formatMoney(order.rewindAmount)}</span>
          <span>额外费：{formatMoney(order.extraAmount)}</span>
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
        <span>原纸：{group.lines.length} 卷 / {formatKg(group.originalWeight)}</span>
        <span>成品：{group.finishCount} 卷 / {formatKg(group.finishWeight)}</span>
        <span>合计：{formatMoney(group.lineAmount)}</span>
      </div>
      <table className="document-print-table document-print-table--settle">
        <thead>
          <tr>
            <th>原纸</th>
            <th>品名/规格</th>
            <th>原纸重量</th>
            <th>加工内容</th>
            <th>成品摘要</th>
            <th>成品重量</th>
            <th>切边</th>
            <th>锯纸单价/金额</th>
            <th>复卷单价/金额</th>
            <th>额外费</th>
            <th>应收合计</th>
          </tr>
        </thead>
        <tbody>
          {group.lines.map((line) => (
            <tr key={`${line.orderUuid}-${line.originalUuid}`}>
              <td>{line.originalLabel}</td>
              <td>{line.paperName || '-'} / {line.gramWeight ? `${line.gramWeight}g` : '-'} / {line.originalWidth ? `${line.originalWidth}mm` : '-'}</td>
              <td>{formatKg(line.originalWeight)}</td>
              <td>{line.processText || '-'}</td>
              <td>{line.finishSummary || '-'}</td>
              <td>{formatKg(line.finishWeight)}</td>
              <td>{formatKg(line.trimWeight)}</td>
              <td><PrintUnitPrice price={line.sawUnitPrice} invoicePrice={line.sawInvoiceUnitPrice} amount={line.sawAmount} /></td>
              <td><PrintUnitPrice price={line.rewindUnitPrice} invoicePrice={line.rewindInvoiceUnitPrice} amount={line.rewindAmount} /></td>
              <td>
                <PrintAmountWithHint amount={line.extraAmount} hint={line.extraFeeSummary} />
              </td>
              <td>{formatMoney(line.lineAmount)}</td>
            </tr>
          ))}
          <tr className="document-print-table__subtotal">
            <td colSpan={2}>加工单小计</td>
            <td>{formatKg(group.originalWeight)}</td>
            <td colSpan={2}>加工费 {formatMoney(group.processAmount)}</td>
            <td>{formatKg(group.finishWeight)}</td>
            <td>{formatKg(group.trimWeight)}</td>
            <td colSpan={2}>额外费 {formatMoney(group.extraAmount)}{group.extraFeeSummary ? `（${group.extraFeeSummary}）` : ''}</td>
            <td>开票价已计入应收</td>
            <td>{formatMoney(group.lineAmount)}</td>
          </tr>
        </tbody>
      </table>
    </div>
  )
}

function PrintUnitPrice({ amount, invoicePrice, price }: { amount?: number; invoicePrice?: number; price?: number }) {
  const showInvoice = invoicePrice != null && invoicePrice !== price
  return (
    <span className="document-print-money">
      <strong>{formatMoney(price)} / {formatMoney(amount)}</strong>
      {showInvoice && <em>开票价 {formatMoney(invoicePrice)}</em>}
    </span>
  )
}

function PrintAmountWithHint({ amount, hint }: { amount?: number; hint?: string }) {
  return (
    <span className="document-print-money">
      <strong>{formatMoney(amount)}</strong>
      {hint && <em>{hint}</em>}
    </span>
  )
}
