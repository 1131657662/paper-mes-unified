import { describe, expect, it } from 'vitest'
import { getBackRecordWorkspaceViewState } from './backRecordWorkspaceViewModel'

describe('回录工作台查询状态', () => {
  it.each([
    [{ isLoadingDetail: true, isDetailError: false, hasDetail: false }, 'loading'],
    [{ isLoadingDetail: false, isDetailError: true, hasDetail: false }, 'error'],
    [{ isLoadingDetail: false, isDetailError: false, hasDetail: false }, 'empty'],
    [{ isLoadingDetail: false, isDetailError: false, hasDetail: true }, 'ready'],
  ] as const)('正确区分 %s 状态', (queryState, expected) => {
    expect(getBackRecordWorkspaceViewState(queryState)).toBe(expected)
  })

  it('接口失败优先于缓存详情，避免进入可提交表单', () => {
    expect(getBackRecordWorkspaceViewState({
      isLoadingDetail: false,
      isDetailError: true,
      hasDetail: true,
    })).toBe('error')
  })
})
