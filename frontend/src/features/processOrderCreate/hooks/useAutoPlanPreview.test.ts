import { describe, expect, it } from 'vitest'
import { BizError } from '../../../api/request'
import { previewErrorText } from './useAutoPlanPreview'

describe('加工方案预览错误文案', () => {
  it('保留后端业务错误说明', () => {
    expect(previewErrorText(new BizError('机台不支持当前工艺', 400, 'E003')))
      .toBe('机台不支持当前工艺')
  })

  it('将 HTTP 失败转换为可操作的中文提示', () => {
    const error = { isAxiosError: true, message: 'Request failed', response: { status: 502 } }

    expect(previewErrorText(error)).toBe('预览服务暂时不可用，请重试')
  })

  it('区分请求超时', () => {
    const error = { isAxiosError: true, code: 'ECONNABORTED', message: 'timeout' }

    expect(previewErrorText(error)).toBe('预览请求超时，请重试')
  })

  it('区分无法连接预览服务', () => {
    const error = { isAxiosError: true, message: 'Network Error' }

    expect(previewErrorText(error)).toBe('无法连接预览服务，请检查网络后重试')
  })
})
