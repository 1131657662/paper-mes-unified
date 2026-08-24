import { describe, expect, it } from 'vitest'
import { actionableClarificationOptions, displayedClarificationOptions } from './processAiClarificationModel'
import type { ProcessAiClarificationQuestion } from './types'

describe('processAiClarificationModel', () => {
  it('adds an explicit unknown branch when the server allows it', () => {
    expect(displayedClarificationOptions(question(true)).map((option) => option.code))
      .toEqual(['PER_SOURCE', 'UNKNOWN'])
  })

  it('does not duplicate an unknown option supplied by the server', () => {
    const value = question(true)
    value.options.push({ code: 'UNKNOWN', label: '不确定' })
    expect(displayedClarificationOptions(value).filter((option) => option.code === 'UNKNOWN'))
      .toHaveLength(1)
  })

  it('does not add unknown when the server disallows it', () => {
    expect(displayedClarificationOptions(question(false)).map((option) => option.code))
      .toEqual(['PER_SOURCE'])
  })

  it('hides the text sentinel from option buttons', () => {
    const value = question(true)
    value.options.push({ code: 'ANSWER_TEXT', label: '补充说明' })
    expect(actionableClarificationOptions(value).map((option) => option.code))
      .toEqual(['PER_SOURCE', 'UNKNOWN'])
  })
})

function question(allowUnknown: boolean): ProcessAiClarificationQuestion {
  return {
    questionId: 'quantity-scope', field: 'quantityScope', parseRevision: 1,
    question: '请选择数量范围', options: [{ code: 'PER_SOURCE', label: '每条母卷' }],
    allowUnknown,
  }
}
