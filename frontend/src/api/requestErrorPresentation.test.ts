import { describe, expect, it } from 'vitest'
import { formatRequestErrorText } from './requestErrorPresentation'

describe('请求错误提示', () => {
  it.each([400, 401, 403, 404, 409])('HTTP %s 不展示技术请求编号', (httpStatus) => {
    const text = formatRequestErrorText('业务提示', error(httpStatus))

    expect(text).toBe('业务提示')
  })

  it.each([500, 502, 503])('HTTP %s 展示技术请求编号', (httpStatus) => {
    const text = formatRequestErrorText('服务器异常', error(httpStatus))

    expect(text).toBe('服务器异常（请求编号：request-123）')
  })

  it('没有请求编号时保持服务端异常文案不变', () => {
    const text = formatRequestErrorText('服务器异常', { code: 500 })

    expect(text).toBe('服务器异常')
  })
})

function error(httpStatus: number) {
  return { code: httpStatus, httpStatus, requestId: 'request-123' }
}
