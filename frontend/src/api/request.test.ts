import { describe, expect, it } from 'vitest'
import { BizError, businessErrorFromResponse } from './request'

describe('HTTP 认证错误解包', () => {
  it('将 HTTP 错误响应体恢复为业务错误', () => {
    const error = businessErrorFromResponse({ code: 401, message: '请先登录', errorCode: 'E001' })

    expect(error).toBeInstanceOf(BizError)
    expect(error).toMatchObject({ code: 401, message: '请先登录', errorCode: 'E001' })
  })

  it('忽略非业务响应和成功响应', () => {
    expect(businessErrorFromResponse({ code: 200, data: null })).toBeNull()
    expect(businessErrorFromResponse('<html>error</html>')).toBeNull()
  })
})
