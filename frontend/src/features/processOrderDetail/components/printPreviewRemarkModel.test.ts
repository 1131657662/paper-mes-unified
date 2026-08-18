import { describe, expect, it } from 'vitest'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import { printPreviewRemarks } from './printPreviewRemarkModel'

describe('printPreviewRemarks', () => {
  it('keeps server-built instructions separate from customer remarks', () => {
    const result = printPreviewRemarks(orderDetail())

    expect(result.workshopInstructions).toEqual([
      '1000mm母卷（R1-R3），共3件：锯纸；每件成品900mm；每件切边余料100mm。',
    ])
    expect(result.customerRemark).toBe('加急；客户原话')
  })

  it('never parses AI requirement JSON into workshop instructions', () => {
    const detail = orderDetail()
    detail.workshopInstructions = undefined
    detail.order.aiRequirementJson = JSON.stringify({ intent: { dangerous: '聊天中的旧配置' } })

    const result = printPreviewRemarks(detail)

    expect(result.workshopInstructions).toEqual([])
    expect(JSON.stringify(result)).not.toContain('聊天中的旧配置')
  })

  it('deduplicates identical short and long remarks', () => {
    const detail = orderDetail()
    detail.order.remark = '同一条要求'
    detail.order.remarkLong = '同一条要求'

    expect(printPreviewRemarks(detail).customerRemark).toBe('同一条要求')
  })
})

function orderDetail(): ProcessOrderDetailVO {
  return {
    order: { uuid: 'order-1', remark: '加急', remarkLong: '客户原话' },
    originalRolls: [],
    rolls: [],
    finishRolls: [],
    steps: [],
    workshopInstructions: [{
      sourceRows: [1, 2, 3],
      sourceWidthMm: 1000,
      sourcePieceCount: 3,
      instruction: '锯纸；每件成品900mm；每件切边余料100mm。',
      text: '1000mm母卷（R1-R3），共3件：锯纸；每件成品900mm；每件切边余料100mm。',
    }],
  }
}
