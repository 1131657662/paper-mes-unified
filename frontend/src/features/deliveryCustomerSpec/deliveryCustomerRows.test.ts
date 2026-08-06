import { describe, expect, it } from 'vitest'
import type { DeliveryDetail } from '../../types/delivery'
import type { DeliveryCustomerSpec } from './deliveryCustomerSpecTypes'
import { resolveDeliveryCustomerRows } from './deliveryCustomerRows'

describe('resolveDeliveryCustomerRows', () => {
  it('matches historical rows by finish id and order roll identity', () => {
    const details = [detail('d2', 'f2', 'A002'), detail('d1', 'f1', 'A001')]
    const specs = [spec('old-1', 'f1', 'A001'), spec('old-2', 'missing', 'A002')]

    const result = resolveDeliveryCustomerRows(details, specs)

    expect(result.rows.map((row) => row.detail?.uuid)).toEqual(['d2', 'd1'])
    expect(result.unmatchedSpecCount).toBe(0)
    expect(result.missingDetailCount).toBe(0)
  })

  it('reports duplicate customer rows for the same delivery detail', () => {
    const details = [detail('d1', 'f1', 'A001'), detail('d2', 'f2', 'A002')]
    const specs = [spec('d1', 'f1', 'A001'), spec('legacy', 'f1', 'A001')]

    const result = resolveDeliveryCustomerRows(details, specs)

    expect(result.duplicateDetailCount).toBe(1)
    expect(result.missingDetailCount).toBe(1)
  })

  it('reports customer rows that cannot match any physical detail', () => {
    const result = resolveDeliveryCustomerRows(
      [detail('d1', 'f1', 'A001')],
      [spec('missing', 'missing', 'A999')],
    )

    expect(result.unmatchedSpecCount).toBe(1)
    expect(result.missingDetailCount).toBe(1)
  })
})

function detail(uuid: string, finishUuid: string, finishRollNo: string): DeliveryDetail {
  return {
    uuid, finishUuid, finishRollNo, deliveryUuid: 'delivery', orderUuid: 'order',
    orderNo: 'JG001', paperName: '白卡纸', outWeight: 100,
  }
}

function spec(deliveryDetailUuid: string, finishUuid: string, finishRollNo: string): DeliveryCustomerSpec {
  return {
    deliveryDetailUuid, finishUuid, finishRollNo, orderNo: 'JG001', detailVersion: 1,
    customerPaperName: '客户白卡', customerDisplayWeight: 100, calculationMode: 'KEEP',
    valueSource: 'PHYSICAL', specificationChanged: false, weightChanged: false, valid: true,
  }
}
