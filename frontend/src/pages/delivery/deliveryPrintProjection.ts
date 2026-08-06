import { resolveDeliveryCustomerRows } from '../../features/deliveryCustomerSpec/deliveryCustomerRows'
import {
  sortDeliveryCustomerRows,
  type DeliveryCustomerTableRow,
} from '../../features/deliveryCustomerSpec/deliveryCustomerSorting'
import type {
  DeliveryCustomerRevisionPreview,
  DeliveryCustomerSpec,
  DeliveryDocumentView,
} from '../../features/deliveryCustomerSpec/deliveryCustomerSpecTypes'
import type { DeliveryDetail, DeliveryDetailVO } from '../../types/delivery'
import type {
  DeliveryCustomerSortSpec,
  DeliveryExportSortChains,
  DeliverySortSpec,
} from '../../types/deliverySort'
import { sortDeliveryDetails } from './deliveryDetailSorting'

export type DeliveryPrintRow =
  | { kind: 'physical'; key: string; detail: DeliveryDetail }
  | { kind: 'customer'; key: string; detail: DeliveryDetail; spec: DeliveryCustomerSpec }
  | { kind: 'trace'; key: string; detail: DeliveryDetail; spec: DeliveryCustomerSpec }

export interface ReadyDeliveryPrintProjection {
  status: 'ready'
  variant: DeliveryDocumentView
  rows: DeliveryPrintRow[]
  sortChain: DeliverySortSpec[] | DeliveryCustomerSortSpec[]
  totalWeight: number
}

export interface InvalidDeliveryPrintProjection {
  status: 'invalid'
  variant: DeliveryDocumentView
  message: string
  sortChain: DeliverySortSpec[] | DeliveryCustomerSortSpec[]
}

export type DeliveryPrintProjection =
  | ReadyDeliveryPrintProjection
  | InvalidDeliveryPrintProjection

interface ProjectionOptions {
  detail: DeliveryDetailVO
  customerSpecs?: DeliveryCustomerRevisionPreview
  variant: DeliveryDocumentView
  sortChains: DeliveryExportSortChains
}

interface CustomerProjectionOptions {
  detail: DeliveryDetailVO
  specs?: DeliveryCustomerRevisionPreview
  variant: 'customer' | 'trace'
  sortChain: DeliveryCustomerSortSpec[]
}

export function buildDeliveryPrintProjection(options: ProjectionOptions): DeliveryPrintProjection {
  if (options.variant === 'physical') return physicalProjection(options.detail, options.sortChains.physical)
  const sortChain = options.variant === 'trace' ? options.sortChains.trace : options.sortChains.customer
  return customerProjection({
    detail: options.detail, specs: options.customerSpecs, variant: options.variant, sortChain,
  })
}

function physicalProjection(
  detail: DeliveryDetailVO, sortChain: DeliverySortSpec[],
): ReadyDeliveryPrintProjection {
  const rows: DeliveryPrintRow[] = sortDeliveryDetails(detail.details, sortChain)
    .map((item) => ({ kind: 'physical', key: item.uuid, detail: item }))
  return { status: 'ready', variant: 'physical', rows, sortChain, totalWeight: detail.order.totalWeight }
}

function customerProjection(options: CustomerProjectionOptions): DeliveryPrintProjection {
  const { detail, specs, variant, sortChain } = options
  if (!specs) return invalidProjection(variant, sortChain, '客户单据口径尚未加载，请稍后重试')
  const resolution = resolveDeliveryCustomerRows(detail.details, specs.items)
  const message = customerProjectionIssue(detail, specs, resolution)
  if (message) return invalidProjection(variant, sortChain, message)
  const completeRows = resolution.rows.filter(hasDetail)
  const sortedRows = sortDeliveryCustomerRows(completeRows, sortChain)
  const rows: DeliveryPrintRow[] = variant === 'trace'
    ? sortedRows.map(({ detail: item, spec }) => ({ kind: 'trace', key: item.uuid, detail: item, spec }))
    : sortedRows.map(({ detail: item, spec }) => ({ kind: 'customer', key: item.uuid, detail: item, spec }))
  return { status: 'ready', variant, rows, sortChain, totalWeight: specs.customerTotalWeight }
}

function customerProjectionIssue(
  detail: DeliveryDetailVO,
  specs: DeliveryCustomerRevisionPreview,
  resolution: ReturnType<typeof resolveDeliveryCustomerRows>,
) {
  if (specs.hasErrors || specs.items.some((item) => !item.valid)) return '客户单据存在校验错误，请修正后再打印'
  if (specs.itemCount !== detail.details.length || specs.items.length !== detail.details.length) {
    return `客户单据件数与出库明细不一致（${specs.items.length}/${detail.details.length}），请刷新后重试`
  }
  if (specs.validItemCount !== specs.itemCount) return '客户单据存在未通过校验的明细，请修正后再打印'
  if (resolution.unmatchedSpecCount > 0) return `有 ${resolution.unmatchedSpecCount} 条客户明细无法关联实物`
  if (resolution.duplicateDetailCount > 0) return `有 ${resolution.duplicateDetailCount} 条客户明细重复关联实物`
  if (resolution.missingDetailCount > 0) return `有 ${resolution.missingDetailCount} 条实物明细缺少客户口径`
  return undefined
}

function invalidProjection(
  variant: 'customer' | 'trace', sortChain: DeliveryCustomerSortSpec[], message: string,
): InvalidDeliveryPrintProjection {
  return { status: 'invalid', variant, message, sortChain }
}

function hasDetail(
  row: DeliveryCustomerTableRow,
): row is DeliveryCustomerTableRow & { detail: DeliveryDetail } {
  return Boolean(row.detail)
}
