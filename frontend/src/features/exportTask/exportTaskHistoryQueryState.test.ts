import { describe, expect, it } from 'vitest'
import { exportTaskHistoryQueryReducer, initialExportTaskHistoryQuery } from './exportTaskHistoryQueryState'

describe('下载任务历史查询状态', () => {
  it('开启待处理时清除不兼容的非终态筛选', () => {
    const query = { ...initialExportTaskHistoryQuery, current: 3, taskStatus: 2 }

    expect(exportTaskHistoryQueryReducer(query, { type: 'attention', attentionOnly: true }))
      .toEqual({ ...query, current: 1, taskStatus: undefined, attentionOnly: true })
  })

  it('选择非终态时自动退出待处理模式', () => {
    const query = { ...initialExportTaskHistoryQuery, attentionOnly: true }

    expect(exportTaskHistoryQueryReducer(query, { type: 'status', taskStatus: 1 }))
      .toEqual({ ...query, current: 1, taskStatus: 1, attentionOnly: false })
  })

  it('终态筛选可以继续细分待处理任务', () => {
    const query = { ...initialExportTaskHistoryQuery, attentionOnly: true }

    expect(exportTaskHistoryQueryReducer(query, { type: 'status', taskStatus: 4 }).attentionOnly).toBe(true)
  })

  it('修改每页数量时回到第一页', () => {
    const query = { ...initialExportTaskHistoryQuery, current: 4 }

    expect(exportTaskHistoryQueryReducer(query, { type: 'page', current: 2, size: 20 }))
      .toEqual({ ...query, current: 1, size: 20 })
  })

  it('搜索关键词时清理空白并回到第一页', () => {
    const query = { ...initialExportTaskHistoryQuery, current: 4 }

    expect(exportTaskHistoryQueryReducer(query, { type: 'keyword', keyword: '  JS2026  ' }))
      .toEqual({ ...query, current: 1, keyword: 'JS2026' })
  })
})
