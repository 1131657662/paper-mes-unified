import { describe, expect, it } from 'vitest'
import { STANDARD_WEIGHT_FORMULA } from '../processOrderCustomerSpec/customerSpecDraftModel'
import {
  applyDeliveryBulkSpecification,
  applyDeliveryPaste,
  applyDeliveryWeightRule,
  buildDeliveryCustomerRevisionRequest,
  createDeliveryCustomerDrafts,
  prepareDeliveryCustomerPreviewRows,
} from './deliveryCustomerDraftModel'
import type { DeliveryCustomerSpec } from './deliveryCustomerSpecTypes'

describe('提货单客户口径草稿', () => {
  it('只批量修改选中明细的品名克重和门幅', () => {
    const rows = createDeliveryCustomerDrafts([spec('d1', 'A1'), spec('d2', 'A2')])

    const result = applyDeliveryBulkSpecification(rows, ['d1'], {
      paperName: '食品卡', gramWeight: 75, finishWidth: 900,
    })

    expect(result[0]).toMatchObject({
      customerPaperName: '食品卡', customerGramWeight: 75, customerFinishWidth: 900,
    })
    expect(result[1]).toEqual(rows[1])
  })

  it('把公式规则完整写入所选明细的预览请求', () => {
    const rows = createDeliveryCustomerDrafts([spec('d1', 'A1'), spec('d2', 'A2')])
    const changed = applyDeliveryWeightRule(rows, ['d2'], {
      mode: 'FORMULA', formula: STANDARD_WEIGHT_FORMULA, skipZero: true,
    })

    const request = buildDeliveryCustomerRevisionRequest(6, '客户要求改单', 'req-1', changed, ['d2'])

    expect(request).toMatchObject({ requestId: 'req-1', expectedDeliveryVersion: 6, reason: '客户要求改单' })
    expect(request.items).toEqual([expect.objectContaining({
      deliveryDetailUuid: 'd2', expectedDetailVersion: 1, calculationMode: 'FORMULA',
      formulaExpression: STANDARD_WEIGHT_FORMULA, zeroPolicy: 'SKIP', roundingScale: 0,
    })])
  })

  it('预览时自动应用尚未点击应用的重量规则', () => {
    const rows = createDeliveryCustomerDrafts([spec('d1', 'A1'), spec('d2', 'A2')])

    const result = prepareDeliveryCustomerPreviewRows(rows, ['d2'], {
      mode: 'DELTA', operand: 25, skipZero: true,
    }, true)

    expect(result[0]).toEqual(rows[0])
    expect(result[1]).toMatchObject({ calculationMode: 'DELTA', weightOperand: 25 })
  })

  it('按卷号匹配粘贴内容且仅有重量时切换为逐件录入', () => {
    const rows = createDeliveryCustomerDrafts([spec('d1', 'A1'), spec('d2', 'A2')])

    const result = applyDeliveryPaste(rows, '卷号\t品名\t克重\t门幅\t重量\nA2\t食品卡\t75\t900\t1186')

    expect(result[0]).toEqual(rows[0])
    expect(result[1]).toMatchObject({
      customerPaperName: '食品卡', customerGramWeight: 75, customerFinishWidth: 900,
      customerDisplayWeight: 1186, calculationMode: 'MANUAL',
    })
  })
})

function spec(deliveryDetailUuid: string, finishRollNo: string): DeliveryCustomerSpec {
  return {
    deliveryDetailUuid, finishRollNo, detailVersion: 1, finishUuid: `finish-${deliveryDetailUuid}`,
    physicalPaperName: '白卡', physicalGramWeight: 70, physicalFinishWidth: 1000,
    physicalDeliveryWeight: 1000, customerPaperName: '白卡', customerGramWeight: 70,
    customerFinishWidth: 1000, customerDisplayWeight: 1000, calculationMode: 'KEEP',
    valueSource: 'PHYSICAL', specificationChanged: false, weightChanged: false, valid: true,
  }
}
