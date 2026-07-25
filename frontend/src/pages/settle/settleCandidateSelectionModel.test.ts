import { describe, expect, it } from 'vitest'
import type { SettleCandidateVO } from '../../types/settle'
import {
  isSelectableCandidate,
  mergeCandidateSelection,
  resolveCandidateSelection,
} from './settleCandidateSelectionModel'

describe('结算候选选择', () => {
  it('翻页后保留前页选择并合并当前页选择', () => {
    const first = candidate('order-1', 'customer-1')
    const second = candidate('order-2', 'customer-1')

    const result = mergeCandidateSelection(
      { [first.orderUuid]: first }, [second], new Set([first.orderUuid, second.orderUuid]))

    expect(Object.keys(result)).toEqual(['order-1', 'order-2'])
  })

  it('点击选择后在重渲染中保留已选加工单', () => {
    const first = candidate('order-1', 'customer-1')
    const selectedByUuid = mergeCandidateSelection({}, [first], new Set([first.orderUuid]))

    const result = resolveCandidateSelection(selectedByUuid, [first], [])

    expect(Object.keys(result)).toEqual(['order-1'])
  })

  it('取消选择后不会从候选列表重新加入加工单', () => {
    const first = candidate('order-1', 'customer-1')
    const selectedByUuid = mergeCandidateSelection({ [first.orderUuid]: first }, [first], new Set())

    const result = resolveCandidateSelection(selectedByUuid, [first], [])

    expect(Object.keys(result)).toEqual([])
  })

  it('候选加载后恢复页面带入的初始选择', () => {
    const first = candidate('order-1', 'customer-1')

    const result = resolveCandidateSelection({}, [first], [first.orderUuid])

    expect(Object.keys(result)).toEqual(['order-1'])
  })

  it('选定客户后禁用其他客户和待核价加工单', () => {
    expect(isSelectableCandidate(candidate('order-2', 'customer-2'), 'customer-1')).toBe(false)
    expect(isSelectableCandidate(candidate('order-3', 'customer-1', 0), 'customer-1')).toBe(false)
    expect(isSelectableCandidate(candidate('order-4', 'customer-1'), 'customer-1')).toBe(true)
  })
})

function candidate(orderUuid: string, customerUuid: string, totalAmount = 100): SettleCandidateVO {
  return { orderUuid, orderNo: orderUuid, customerUuid, customerName: customerUuid, totalAmount }
}
