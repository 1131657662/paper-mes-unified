import type { DeliveryDetail } from '../../types/delivery'
import type { DeliveryCustomerSpec } from './deliveryCustomerSpecTypes'
import type { DeliveryCustomerTableRow } from './deliveryCustomerSorting'

export interface DeliveryCustomerRowResolution {
  rows: DeliveryCustomerTableRow[]
  duplicateDetailCount: number
  missingDetailCount: number
  unmatchedSpecCount: number
}

interface DetailIndexes {
  byUuid: Map<string, DeliveryDetail | null>
  byFinishUuid: Map<string, DeliveryDetail | null>
  byOrderRoll: Map<string, DeliveryDetail | null>
}

interface CustomerRowPartition {
  duplicateDetailCount: number
  extras: DeliveryCustomerTableRow[]
  matched: Set<string>
  orderedByDetail: Map<string, DeliveryCustomerTableRow>
  unmatchedSpecCount: number
}

export function resolveDeliveryCustomerRows(
  details: DeliveryDetail[], specs: DeliveryCustomerSpec[],
): DeliveryCustomerRowResolution {
  const indexes = buildDetailIndexes(details)
  const sourceRows = specs.map((spec) => ({ spec, detail: findDetail(spec, indexes) }))
  const partition = partitionCustomerRows(sourceRows)
  const rows = details.flatMap((detail) => {
    const row = partition.orderedByDetail.get(detail.uuid)
    return row ? [row] : []
  }).concat(partition.extras)
  return {
    rows,
    duplicateDetailCount: partition.duplicateDetailCount,
    missingDetailCount: details.length - partition.matched.size,
    unmatchedSpecCount: partition.unmatchedSpecCount,
  }
}

function partitionCustomerRows(sourceRows: DeliveryCustomerTableRow[]): CustomerRowPartition {
  const matched = new Set<string>()
  const orderedByDetail = new Map<string, DeliveryCustomerTableRow>()
  const extras: DeliveryCustomerTableRow[] = []
  let duplicateDetailCount = 0
  let unmatchedSpecCount = 0

  sourceRows.forEach((row) => {
    if (!row.detail) {
      unmatchedSpecCount += 1
      extras.push(row)
    } else if (matched.has(row.detail.uuid)) {
      duplicateDetailCount += 1
      extras.push(row)
    } else {
      matched.add(row.detail.uuid)
      orderedByDetail.set(row.detail.uuid, row)
    }
  })
  return { duplicateDetailCount, extras, matched, orderedByDetail, unmatchedSpecCount }
}

function buildDetailIndexes(details: DeliveryDetail[]): DetailIndexes {
  const indexes: DetailIndexes = {
    byUuid: new Map(), byFinishUuid: new Map(), byOrderRoll: new Map(),
  }
  details.forEach((detail) => {
    addUnique(indexes.byUuid, detail.uuid, detail)
    addUnique(indexes.byFinishUuid, detail.finishUuid, detail)
    addUnique(indexes.byOrderRoll, orderRollKey(detail.orderNo, detail.finishRollNo), detail)
  })
  return indexes
}

function addUnique(
  index: Map<string, DeliveryDetail | null>, key: string | undefined, detail: DeliveryDetail,
) {
  if (!key) return
  index.set(key, index.has(key) ? null : detail)
}

function findDetail(spec: DeliveryCustomerSpec, indexes: DetailIndexes): DeliveryDetail | undefined {
  const compositeKey = orderRollKey(spec.orderNo, spec.finishRollNo)
  const candidates = [
    indexes.byUuid.get(spec.deliveryDetailUuid),
    indexes.byFinishUuid.get(spec.finishUuid),
    compositeKey ? indexes.byOrderRoll.get(compositeKey) : undefined,
  ]
  return candidates.find((candidate): candidate is DeliveryDetail => Boolean(candidate))
}

function orderRollKey(orderNo?: string, finishRollNo?: string) {
  return orderNo && finishRollNo ? JSON.stringify([orderNo, finishRollNo]) : undefined
}
