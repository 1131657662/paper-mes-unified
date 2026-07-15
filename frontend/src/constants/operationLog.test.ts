import { describe, expect, it } from 'vitest'

import { ACTION_TYPES } from './operationLog'

describe('operationLog action options', () => {
  it('includes every high-risk action used by backend audit logs', () => {
    expect(Object.keys(ACTION_TYPES)).toEqual(expect.arrayContaining([
      '补打',
      '作废加工单',
      '回退',
      '取消出库',
      '取消收款',
      '作废结算',
      '数据备份',
      '恢复演练',
      '删除备份',
      '清理备份',
      '数据修复',
    ]))
  })
})
