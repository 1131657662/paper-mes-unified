import { describe, expect, it } from 'vitest'
import { mergeCandidateOptions } from './reportCandidateOptions'

describe('报表候选项合并', () => {
  it('远程结果覆盖同值初始项且不会产生重复选项', () => {
    const initial = [{ value: 'a', label: '旧名称' }, { value: 'b', label: '客户B' }]
    const remote = [{ value: 'a', label: '新名称' }, { value: 'c', label: '客户C' }]

    const result = mergeCandidateOptions(initial, remote)

    expect(result).toEqual([
      { value: 'a', label: '新名称' },
      { value: 'b', label: '客户B' },
      { value: 'c', label: '客户C' },
    ])
  })
})
