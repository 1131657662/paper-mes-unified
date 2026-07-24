import type { DeliveryCustomerRevisionPreview, DeliveryCustomerSpec, DeliveryDocumentView } from '../../features/deliveryCustomerSpec/deliveryCustomerSpecTypes'
import { deliveryOriginalSnapshotText, formatKg, formatTon } from '../../features/delivery/utils/deliveryFormatters'
import type { DeliveryDetail, DeliveryDetailVO } from '../../types/delivery'
import { formatGram, formatMm } from '../../utils/numberFormatters'
import '../../pages/documentModule.css'

interface Props {
  detail: DeliveryDetailVO
  customerSpecs?: DeliveryCustomerRevisionPreview
  variant?: DeliveryDocumentView
}

export default function DeliveryPrintSheet({ detail, customerSpecs, variant = 'customer' }: Props) {
  const { order, details } = detail
  const specIndex = new Map((customerSpecs?.items ?? []).map((item) => [item.deliveryDetailUuid, item]))
  const totalWeight = variant === 'physical' ? order.totalWeight : customerSpecs?.customerTotalWeight ?? order.totalWeight
  return (
    <div className="document-print-area document-print-area--delivery">
      <div className="document-print-sheet">
        <header className="document-print-sheet__header">
          <div>
            <h1>{printTitle(variant, customerSpecs)}</h1>
            <div className="document-print-sheet__meta"><span>出库单号：{order.deliveryNo}</span><span>客户：{order.customerName}</span><span>出库日期：{order.deliveryDate}</span></div>
          </div>
          <div className="document-print-sheet__summary"><span>{order.totalCount} 卷</span><span>{formatTon(totalWeight)}</span><span>车牌：{order.carNo || '-'}</span><span>柜号：{order.containerNo || '-'}</span></div>
        </header>

        <section>
          <h2>{variant === 'customer' ? '提货明细' : variant === 'physical' ? '仓库出库明细' : '客户与实物对照'}</h2>
          <table className="document-print-table">
            <thead><tr><th>序号</th><th>加工单</th><th>卷号</th><th>品名</th><th>克重</th><th>门幅</th><th>单据重量</th><th>{variant === 'trace' ? '来源追溯' : '备注'}</th></tr></thead>
            <tbody>{details.map((item, index) => <PrintRow key={item.uuid} index={index} item={item} spec={specIndex.get(item.uuid)} variant={variant} />)}</tbody>
          </table>
        </section>

        <footer className="document-print-sheet__footer"><span>提货人：{order.pickerName || '-'}</span><span>签收人：{order.signUser || ''}</span><span>仓库签字：</span><span>司机签字：</span></footer>
      </div>
    </div>
  )
}

function PrintRow({ index, item, spec, variant }: { index: number; item: DeliveryDetail; spec?: DeliveryCustomerSpec; variant: DeliveryDocumentView }) {
  const customer = variant !== 'physical'
  return (
    <tr>
      <td>{index + 1}</td><td>{item.orderNo || '-'}</td><td>{printFinishRollNo(item)}</td>
      <td><PrintValue customer={customer ? spec?.customerPaperName : undefined} physical={item.paperName} trace={variant === 'trace'} /></td>
      <td><PrintValue customer={customer ? optionalGram(spec?.customerGramWeight) : undefined} physical={formatGram(item.gramWeight)} trace={variant === 'trace'} /></td>
      <td><PrintValue customer={customer ? optionalWidth(spec?.customerFinishWidth) : undefined} physical={formatMm(item.finishWidth)} trace={variant === 'trace'} /></td>
      <td><PrintValue customer={customer ? optionalWeight(spec?.customerDisplayWeight) : undefined} physical={formatKg(item.outWeight)} trace={variant === 'trace'} /></td>
      <td>{variant === 'trace' ? deliveryOriginalSnapshotText(item) : customer ? spec?.customerRemark || item.actualRemark || '-' : item.actualRemark || '-'}</td>
    </tr>
  )
}

function PrintValue({ customer, physical, trace }: { customer?: string; physical?: string; trace: boolean }) {
  if (!trace) return <>{customer || physical || '-'}</>
  return <div className="document-print-comparison"><strong>{customer || physical || '-'}</strong><span>实物：{physical || '-'}</span></div>
}

function printTitle(variant: DeliveryDocumentView, specs?: DeliveryCustomerRevisionPreview) {
  if (variant === 'physical') return '出库单（仓库实物）'
  if (variant === 'trace') return '出库单（追溯对照）'
  if (specs?.currentRevisionKind === 'USER_REVISION') return `出库单（客户更正版 V${specs.currentRevisionNo}）`
  if (specs?.currentRevisionKind === 'SYSTEM_BASELINE') return `出库单（客户口径 V${specs.currentRevisionNo}）`
  if (specs?.currentRevisionKind === 'HISTORICAL_BASELINE') return '出库单（历史实物基线）'
  return '出库单'
}

function printFinishRollNo(item: { finishRollNo?: string; isRemain?: number }) {
  const rollNo = item.finishRollNo || '-'
  return item.isRemain === 1 ? `${rollNo}（余料）` : rollNo
}

const optionalGram = (value?: number) => value == null ? undefined : formatGram(value)
const optionalWidth = (value?: number) => value == null ? undefined : formatMm(value)
const optionalWeight = (value?: number) => value == null ? undefined : formatKg(value)
