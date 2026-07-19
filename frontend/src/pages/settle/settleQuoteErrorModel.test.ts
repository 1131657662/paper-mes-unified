import { describe, expect, it } from 'vitest'
import { BizError } from '../../api/request'
import { isCandidateEligibilityError } from './settleQuoteErrorModel'

describe('isCandidateEligibilityError', () => {
  it('resets stale selections for a settled order', () => {
    expect(isCandidateEligibilityError(new BizError('加工单已结算，不可重复结算：JG-1', 400, 'E004'))).toBe(true)
  })

  it('keeps selections for transient network errors', () => {
    expect(isCandidateEligibilityError(new Error('Network Error'))).toBe(false)
  })

  it('keeps selections for pending-price business validation', () => {
    expect(isCandidateEligibilityError(new BizError('存在待核价加工单，请完成核价后再生成结算单', 400, 'E001'))).toBe(false)
  })
})
