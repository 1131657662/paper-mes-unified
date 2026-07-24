import { describe, expect, it } from 'vitest'
import { pricingPreview } from './processStepPricingModel'

describe('附加工艺后定价预览', () => {
  it('从按件切换按吨时使用回录实重而不是旧服务数量', () => {
    const preview = pricingPreview({
      step: { uuid: 'step-1', stepType: 3, billingBasis: 'PIECE', serviceQuantity: 2 },
      originalRoll: { uuid: 'roll-1', actualWeight: 2285, pieceNum: 2 },
      mode: 1,
      billingBasis: 'TON',
      billingUnitPrice: 100,
    })

    expect(preview.quantity).toBe(2.285)
    expect(preview.finalAmount).toBe(229)
  })

  it('按件时直接取母卷件数', () => {
    const preview = pricingPreview({
      step: { uuid: 'step-1', stepType: 4 },
      originalRoll: { uuid: 'roll-1', pieceNum: 3, totalWeight: 2400 },
      mode: 1,
      billingBasis: 'PIECE',
      billingUnitPrice: 20,
    })

    expect(preview.quantity).toBe(3)
    expect(preview.finalAmount).toBe(60)
  })
})
