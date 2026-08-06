import dayjs from 'dayjs'
import { DISPLAY_TERMS } from '../../constants/displayTerms'
import type { DeliveryCustomerRevisionPreview } from '../../features/deliveryCustomerSpec/deliveryCustomerSpecTypes'
import { deliveryOriginalSnapshotText, formatKg, formatTon } from '../../features/delivery/utils/deliveryFormatters'
import type { DeliveryDetail, DeliveryDetailVO, DeliveryOrder } from '../../types/delivery'
import { formatDateTime } from '../../utils/dateTime'
import { formatGram, formatMm } from '../../utils/numberFormatters'
import type { DeliveryPrintRow, ReadyDeliveryPrintProjection } from './deliveryPrintProjection'
import '../../pages/documentModule.css'
import './DeliveryPrintSheet.css'
import './DeliveryPrintSheet.print.css'

interface Props {
  detail: DeliveryDetailVO
  customerSpecs?: DeliveryCustomerRevisionPreview
  projection: ReadyDeliveryPrintProjection
}

export default function DeliveryPrintSheet({ detail, customerSpecs, projection }: Props) {
  return (
    <div className="document-print-area document-print-area--delivery">
      <article className="document-print-sheet delivery-print-sheet">
        <DeliveryPrintHeader order={detail.order} title={printTitle(projection.variant, customerSpecs)} />
        <DeliveryPrintTable projection={projection} />
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

function DeliveryPrintTable({ projection }: { projection: ReadyDeliveryPrintProjection }) {
  const trace = projection.variant === 'trace'
  return (
    <section className="delivery-print-details">
      <h2>{projection.variant === 'customer' ? '提货明细' : projection.variant === 'physical' ? '仓库出库明细' : '客户与实物对照'}</h2>
      <table className="document-print-table delivery-print-table">
        <colgroup><col /><col /><col /><col /><col /><col /><col /></colgroup>
        <thead><tr><th>序号</th><th>加工单</th><th>卷号</th><th>品名</th><th>规格</th><th>重量/kg</th><th>{trace ? '来源追溯' : '备注'}</th></tr></thead>
        <tbody>{projection.rows.map((row, index) => <PrintRow key={row.key} index={index} row={row} />)}</tbody>
        <tbody className="delivery-print-table__total"><tr><td colSpan={5}>合计：{projection.rows.length} 卷</td><td>{formatTon(projection.totalWeight)}</td><td /></tr></tbody>
      </table>
    </section>
  )
}

function PrintRow({ index, row }: { index: number; row: DeliveryPrintRow }) {
  if (row.kind === 'physical') return <PhysicalPrintRow index={index} item={row.detail} />
  if (row.kind === 'customer') return <CustomerPrintRow index={index} row={row} />
  return <TracePrintRow index={index} row={row} />
}

function PhysicalPrintRow({ index, item }: { index: number; item: DeliveryDetail }) {
  return (
    <tr>
      <td>{index + 1}</td><td>{item.orderNo || '-'}</td><td>{printFinishRollNo(item)}</td>
      <td>{item.paperName || '-'}</td><td>{physicalSpec(item)}</td>
      <td className="delivery-print-table__weight">{formatKg(item.outWeight)}</td>
      <td>{item.actualRemark || '-'}</td>
    </tr>
  )
}

function CustomerPrintRow({ index, row }: { index: number; row: Extract<DeliveryPrintRow, { kind: 'customer' }> }) {
  const { detail, spec } = row
  return (
    <tr>
      <td>{index + 1}</td><td>{detail.orderNo || '-'}</td><td>{printFinishRollNo(detail)}</td>
      <td>{spec.customerPaperName || '-'}</td><td>{customerSpec(spec)}</td>
      <td className="delivery-print-table__weight">{formatKg(spec.customerDisplayWeight)}</td>
      <td>{spec.customerRemark || detail.actualRemark || '-'}</td>
    </tr>
  )
}

function TracePrintRow({ index, row }: { index: number; row: Extract<DeliveryPrintRow, { kind: 'trace' }> }) {
  const { detail, spec } = row
  return (
    <tr>
      <td>{index + 1}</td><td>{detail.orderNo || '-'}</td><td>{printFinishRollNo(detail)}</td>
      <td><ComparisonValue primary={spec.customerPaperName} secondary={detail.paperName} /></td>
      <td><ComparisonValue primary={customerSpec(spec)} secondary={physicalSpec(detail)} /></td>
      <td className="delivery-print-table__weight"><ComparisonValue primary={formatKg(spec.customerDisplayWeight)} secondary={formatKg(detail.outWeight)} /></td>
      <td>{deliveryOriginalSnapshotText(detail)}</td>
    </tr>
  )
}

function ComparisonValue({ primary, secondary }: { primary?: string; secondary?: string }) {
  return <div className="document-print-comparison"><strong>{primary || '-'}</strong><span>实物：{secondary || '-'}</span></div>
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

function printTitle(variant: ReadyDeliveryPrintProjection['variant'], specs?: DeliveryCustomerRevisionPreview) {
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

function customerSpec(spec: Extract<DeliveryPrintRow, { kind: 'customer' | 'trace' }>['spec']) {
  return `${formatGram(spec.customerGramWeight)} × ${formatMm(spec.customerFinishWidth)}`
}
