import { describe, expect, it } from 'vitest'
import { buildPricingBatchRequest, initialPricingValues, pricingSteps } from './pricingBatchModel'

describe('工序单价批量核定模型', () => {
  it('混合选择时按切纸和复卷分别生成计价分组', () => {
    const selectedSteps = pricingSteps([
      { uuid: 'saw-1', stepType: 1, unitPrice: 120 },
      { uuid: 'rewind-1', stepType: 2, unitPrice: 180 },
    ])

    const request = buildPricingBatchRequest({
      orderVersion: 6,
      selectedSteps,
      values: { reason: '协议价格', sawPrice: 100, rewindPrice: 150 },
    })

    expect(request.groups).toEqual([
      { stepType: 1, stepUuids: ['saw-1'], restoreStandard: false, billingUnitPrice: 100 },
      { stepType: 2, stepUuids: ['rewind-1'], restoreStandard: false, billingUnitPrice: 150 },
    ])
  })

  it('恢复标准价时不提交核定单价', () => {
    const selectedSteps = pricingSteps([{ uuid: 'rewind-1', stepType: 2, unitPrice: 180, billingUnitPrice: 150 }])

    const request = buildPricingBatchRequest({
      orderVersion: 2,
      selectedSteps,
      values: { reason: '恢复合同价', rewindPrice: 150, rewindRestore: true },
    })

    expect(request.groups[0]).toEqual({
      stepType: 2, stepUuids: ['rewind-1'], restoreStandard: true, billingUnitPrice: undefined,
    })
  })

  it('同组价格不一致时不预填误导性单价', () => {
    const values = initialPricingValues(pricingSteps([
      { uuid: 'saw-1', stepType: 1, unitPrice: 100 },
      { uuid: 'saw-2', stepType: 1, unitPrice: 120 },
    ]))

    expect(values.sawPrice).toBeUndefined()
  })

  it('附加工艺按吨批量核价时不提交人工数量', () => {
    const selectedSteps = pricingSteps([
      { uuid: 'strip-1', stepType: 3, billingMode: 1, billingBasis: 'PIECE', serviceQuantity: 1 },
      { uuid: 'strip-2', stepType: 3, billingMode: 1, billingBasis: 'PIECE', serviceQuantity: 2 },
    ])

    const request = buildPricingBatchRequest({
      orderVersion: 3,
      selectedSteps,
      values: { reason: '完工后按实重核价', stripMode: 1, stripBasis: 'TON', stripPrice: 95 },
    })

    expect(request.groups).toEqual([{
      stepType: 3,
      stepUuids: ['strip-1', 'strip-2'],
      restoreStandard: false,
      billingMode: 1,
      billingBasis: 'TON',
      billingUnitPrice: 95,
    }])
  })

  it('附加工艺固定金额按所选工序合计提交', () => {
    const selectedSteps = pricingSteps([
      { uuid: 'pack-1', stepType: 4 },
      { uuid: 'pack-2', stepType: 4 },
    ])

    const request = buildPricingBatchRequest({
      orderVersion: 3,
      selectedSteps,
      values: { reason: '客户确认整批价格', repackMode: 3, repackAmount: 300 },
    })

    expect(request.groups[0]).toMatchObject({
      stepType: 4,
      billingMode: 3,
      billingAmount: 300,
    })
  })
})
