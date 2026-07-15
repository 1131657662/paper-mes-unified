import { describe, expect, it } from 'vitest'
import type { AvailableFinishVO } from '../../types/delivery'
import {
  deliverySelectionError,
  deliveryWeightFeedback,
  selectedDeliveryFinishes,
  summarizeDeliverySelection,
} from './deliverySelectionModel'

describe('deliverySelectionModel', () => {
  const product = finish({ actualWeight: 500, finishUuid: 'product', isRemain: 0 })
  const remain = finish({ actualWeight: 120, finishUuid: 'remain', isRemain: 1 })

  it('按勾选顺序之外的原始库存顺序返回选择项', () => {
    expect(selectedDeliveryFinishes([product, remain], ['remain']).map((item) => item.finishUuid))
      .toEqual(['remain'])
  })

  it('分别汇总成品和余料，并使用本次出库重量', () => {
    const summary = summarizeDeliverySelection([product, remain], { product: { outWeight: 300 } })
    expect(summary).toMatchObject({ totalCount: 2, totalWeight: 420, productWeight: 300, remainWeight: 120 })
  })

  it('阻止零重量和超过库存重量的出库', () => {
    expect(deliveryWeightFeedback(product, { outWeight: 0 }).status).toBe('error')
    expect(deliverySelectionError([product], { product: { outWeight: 501 } })).toContain('超过可出库重量')
  })
})

function finish(values: Partial<AvailableFinishVO>): AvailableFinishVO {
  return { finishRollNo: values.finishUuid ?? '卷号', remainingWeight: values.actualWeight, ...values } as AvailableFinishVO
}
