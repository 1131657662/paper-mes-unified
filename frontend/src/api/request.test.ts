import { describe, expect, it } from 'vitest'
import { BizError, businessErrorFromResponse, shouldNotifyBusinessError } from './request'

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

  it('允许页面流程接管指定业务错误', () => {
    const staleCandidate = new BizError('加工单已结算', 400, 'E004')

    expect(shouldNotifyBusinessError(staleCandidate, { silentBusinessErrorCodes: ['E004'] })).toBe(false)
    expect(shouldNotifyBusinessError(staleCandidate, { silentBusinessErrorCodes: ['E001'] })).toBe(true)
  })

  it('静默请求不触发全局业务错误通知', () => {
    const error = new BizError('任务中心暂不可用', 503)

    expect(shouldNotifyBusinessError(error, { silentError: true })).toBe(false)
  })
})
