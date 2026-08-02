import dayjs from 'dayjs'
import { DISPLAY_TERMS } from '../../constants/displayTerms'
import type { DeliveryCustomerRevisionPreview, DeliveryCustomerSpec, DeliveryDocumentView } from '../../features/deliveryCustomerSpec/deliveryCustomerSpecTypes'
import { deliveryOriginalSnapshotText, formatKg, formatTon } from '../../features/delivery/utils/deliveryFormatters'
import type { DeliveryDetail, DeliveryDetailVO, DeliveryOrder } from '../../types/delivery'
import { formatDateTime } from '../../utils/dateTime'
import { formatGram, formatMm } from '../../utils/numberFormatters'
import '../../pages/documentModule.css'
import './DeliveryPrintSheet.css'
import './DeliveryPrintSheet.print.css'

interface Props {
  detail: DeliveryDetailVO
  customerSpecs?: DeliveryCustomerRevisionPreview
  variant?: DeliveryDocumentView
}

interface PrintTableProps extends Props {
  totalWeight: number
}

export default function DeliveryPrintSheet({ detail, customerSpecs, variant = 'customer' }: Props) {
  const totalWeight = variant === 'physical'
    ? detail.order.totalWeight
    : customerSpecs?.customerTotalWeight ?? detail.order.totalWeight
  return (
    <div className="document-print-area document-print-area--delivery">
      <article className="document-print-sheet delivery-print-sheet">
        <DeliveryPrintHeader order={detail.order} title={printTitle(variant, customerSpecs)} />
        <DeliveryPrintTable detail={detail} customerSpecs={customerSpecs} variant={variant} totalWeight={totalWeight} />
        {detail.order.remark && <div className="delivery-print-remark"><strong>出库备注</strong><span>{detail.order.remark}</span></div>}
        <DeliveryPrintSignatures order={detail.order} />
        <div className="delivery-print-page-footer" aria-hidden="true">
          <span>{detail.order.deliveryNo}</span><span>打印时间：{dayjs().format('YYYY-MM-DD HH:mm')}</span>
        </div>
      </article>
    </div>
  )
}

function DeliveryPrintHeader({ order, title }: { order: DeliveryOrder; title: string }) {
  return (
    <header className="delivery-print-header">
      <h1>{title}</h1>
      <dl className="delivery-print-info">
        <PrintInfo className="delivery-print-info__wide" label="出库单号" value={order.deliveryNo} />
        <PrintInfo label="出库日期" value={order.deliveryDate} />
        <PrintInfo label="出库仓库" value={order.warehouseName || '-'} />
        <PrintInfo className="delivery-print-info__wide" label="货主" value={order.customerName} />
        <PrintInfo className="delivery-print-info__wide" label="收货客户" value={order.receiverCustomerName || ''} />
        <PrintInfo className="delivery-print-info__wide" label="车牌号" value={order.carNo || ''} />
        <PrintInfo className="delivery-print-info__wide" label="柜号" value={order.containerNo || ''} />
      </dl>
    </header>
  )
}

function PrintInfo({ className = '', label, value }: { className?: string; label: string; value: string }) {
  return <div className={`delivery-print-info__item ${className}`.trim()}><dt>{label}</dt><dd>{value}</dd></div>
}

function DeliveryPrintTable({ detail, customerSpecs, variant = 'customer', totalWeight }: PrintTableProps) {
  const specIndex = new Map((customerSpecs?.items ?? []).map((item) => [item.deliveryDetailUuid, item]))
  return (
    <section className="delivery-print-details">
      <h2>{variant === 'customer' ? '提货明细' : variant === 'physical' ? '仓库出库明细' : '客户与实物对照'}</h2>
      <table className="document-print-table delivery-print-table">
        <colgroup><col /><col /><col /><col /><col /><col /><col /></colgroup>
        <thead><tr><th>序号</th><th>加工单</th><th>卷号</th><th>品名</th><th>规格</th><th>重量/kg</th><th>{variant === 'trace' ? '来源追溯' : '备注'}</th></tr></thead>
        <tbody>{detail.details.map((item, index) => <PrintRow key={item.uuid} index={index} item={item} spec={specIndex.get(item.uuid)} variant={variant} />)}</tbody>
        <tbody className="delivery-print-table__total"><tr><td colSpan={5}>合计：{detail.order.totalCount} 卷</td><td>{formatTon(totalWeight)}</td><td /></tr></tbody>
      </table>
    </section>
  )
}

function PrintRow({ index, item, spec, variant }: { index: number; item: DeliveryDetail; spec?: DeliveryCustomerSpec; variant: DeliveryDocumentView }) {
  const customer = variant !== 'physical'
  return (
    <tr>
      <td>{index + 1}</td><td>{item.orderNo || '-'}</td><td>{printFinishRollNo(item)}</td>
      <td><PrintValue customer={customer ? spec?.customerPaperName : undefined} physical={item.paperName} trace={variant === 'trace'} /></td>
      <td><PrintValue customer={customer ? customerSpec(item, spec) : undefined} physical={physicalSpec(item)} trace={variant === 'trace'} /></td>
      <td className="delivery-print-table__weight"><PrintValue customer={customer ? optionalWeight(spec?.customerDisplayWeight) : undefined} physical={formatKg(item.outWeight)} trace={variant === 'trace'} /></td>
      <td>{variant === 'trace' ? deliveryOriginalSnapshotText(item) : customer ? spec?.customerRemark || item.actualRemark || '-' : item.actualRemark || '-'}</td>
    </tr>
  )
}

function PrintValue({ customer, physical, trace }: { customer?: string; physical?: string; trace: boolean }) {
  if (!trace) return <>{customer || physical || '-'}</>
  return <div className="document-print-comparison"><strong>{customer || physical || '-'}</strong><span>实物：{physical || '-'}</span></div>
}

function DeliveryPrintSignatures({ order }: { order: DeliveryOrder }) {
  return (
    <footer className="delivery-print-signatures">
      <div><span>提货人</span><strong /></div>
      <div><span>仓库复核</span><strong /></div>
      <div><span>司机签字</span><strong /></div>
      <div><span>签收时间</span><strong>{order.signTime ? formatDateTime(order.signTime) : ''}</strong></div>
    </footer>
  )
}

function printTitle(variant: DeliveryDocumentView, specs?: DeliveryCustomerRevisionPreview) {
  if (variant === 'physical') return '出库单（仓库实物）'
  if (variant === 'trace') return '出库单（追溯对照）'
  if (specs?.currentRevisionKind === 'USER_REVISION') return `出库单（客户更正版 V${specs.currentRevisionNo}）`
  if (specs?.currentRevisionKind === 'SYSTEM_BASELINE') {
    return `出库单（${DISPLAY_TERMS.customerSpecification} V${specs.currentRevisionNo}）`
  }
  if (specs?.currentRevisionKind === 'HISTORICAL_BASELINE') return '出库单（历史实物基线）'
  return '出库单'
}

function printFinishRollNo(item: Pick<DeliveryDetail, 'finishRollNo' | 'isRemain'>) {
  return item.isRemain === 1 ? `${item.finishRollNo || '-'}（余料）` : item.finishRollNo || '-'
}

function physicalSpec(item: Pick<DeliveryDetail, 'gramWeight' | 'finishWidth'>) {
  return `${formatGram(item.gramWeight)} × ${formatMm(item.finishWidth)}`
}

function customerSpec(item: Pick<DeliveryDetail, 'gramWeight' | 'finishWidth'>, spec?: DeliveryCustomerSpec) {
  const gram = spec?.customerGramWeight == null ? formatGram(item.gramWeight) : formatGram(spec.customerGramWeight)
  const width = spec?.customerFinishWidth == null ? formatMm(item.finishWidth) : formatMm(spec.customerFinishWidth)
  return `${gram} × ${width}`
}

const optionalWeight = (value?: number) => value == null ? undefined : formatKg(value)
