import { beforeEach, describe, expect, it, vi } from 'vitest'
import { request } from '../../../api/request'
import { aiService } from './aiService'

vi.mock('../../../api/request', () => ({ request: vi.fn() }))

const requestMock = vi.mocked(request)

describe('aiService', () => {
  beforeEach(() => requestMock.mockReset())

  it('sends only the approved FAQ context fields', async () => {
    requestMock.mockResolvedValue({ decision: 'CLARIFY' })
    const payload = {
      question: 'E001 为什么不能操作？',
      pageTemplate: 'process-orders',
      contextEpoch: 'opaque-key',
    }

    await aiService.assist(payload)

    expect(requestMock).toHaveBeenCalledWith(expect.objectContaining({
      url: '/api/ai/assist',
      method: 'post',
      data: payload,
    }))
    expect(Object.keys(requestMock.mock.calls[0]![0].data)).toEqual([
      'question',
      'pageTemplate',
      'contextEpoch',
    ])
  })

  it('forwards the abort signal to the shared request client', async () => {
    requestMock.mockResolvedValue({ decision: 'CLARIFY' })
    const controller = new AbortController()

    await aiService.assist({
      question: 'E001',
      pageTemplate: 'process-orders',
      contextEpoch: 'opaque-key',
    }, controller.signal)

    expect(requestMock).toHaveBeenCalledWith(expect.objectContaining({ signal: controller.signal }))
  })
})
