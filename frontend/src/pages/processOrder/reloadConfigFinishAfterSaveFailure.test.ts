import { describe, expect, it } from 'vitest'
import { reloadConfigFinishAfterSaveFailure } from './reloadConfigFinishAfterSaveFailure'

describe('成品配置保存失败恢复', () => {
  it('重新加载成功时允许使用服务端最新版本继续编辑', async () => {
    const result = await reloadConfigFinishAfterSaveFailure(
      async () => ({ isSuccess: true }),
    )

    expect(result).toEqual({ reloaded: true })
  })

  it('重新加载失败时返回查询错误并保留当前编辑状态', async () => {
    const error = new Error('network unavailable')

    const result = await reloadConfigFinishAfterSaveFailure(
      async () => ({ error, isSuccess: false }),
    )

    expect(result).toEqual({ error, reloaded: false })
  })
})
