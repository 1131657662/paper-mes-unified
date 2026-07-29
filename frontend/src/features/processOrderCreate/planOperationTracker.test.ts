import { describe, expect, it } from 'vitest'
import { PlanOperationTracker } from './planOperationTracker'

describe('加工方案请求版本跟踪', () => {
  it('编辑后拒绝旧保存响应', () => {
    const tracker = new PlanOperationTracker()
    const token = tracker.begin('save', 'roll-a')

    tracker.markEdited('roll-a')

    expect(tracker.isCurrent(token)).toBe(false)
  })

  it('同一母卷只接受最后一次预览响应', () => {
    const tracker = new PlanOperationTracker()
    const first = tracker.begin('preview', 'roll-a')

    const second = tracker.begin('preview', 'roll-a')

    expect(tracker.isCurrent(first)).toBe(false)
    expect(tracker.isCurrent(second)).toBe(true)
  })

  it('其他母卷的编辑不影响当前响应', () => {
    const tracker = new PlanOperationTracker()
    const token = tracker.begin('save', 'roll-a')

    tracker.markEdited('roll-b')

    expect(tracker.isCurrent(token)).toBe(true)
  })
})
