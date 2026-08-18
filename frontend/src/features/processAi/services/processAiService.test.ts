import { beforeEach, describe, expect, it, vi } from 'vitest'
import { request } from '../../../api/request'
import { processAiService } from './processAiService'

vi.mock('../../../api/request', () => ({ request: vi.fn() }))

const requestMock = vi.mocked(request)

describe('processAiService provider credentials', () => {
  beforeEach(() => requestMock.mockReset())

  it('uses an isolated settings route for each managed provider', async () => {
    requestMock.mockResolvedValue({ provider: 'ZHIPU' })

    await processAiService.providerSettings('deepseek')
    await processAiService.providerSettings('zhipu')

    expect(requestMock.mock.calls[0]?.[0]).toMatchObject({
      url: '/api/ai/provider-settings/deepseek',
      method: 'get',
    })
    expect(requestMock.mock.calls[1]?.[0]).toMatchObject({
      url: '/api/ai/provider-settings/zhipu',
      method: 'get',
    })
  })

  it('sends the key only to the selected provider route', async () => {
    requestMock.mockResolvedValue({ provider: 'ZHIPU' })

    await processAiService.updateProviderKey('zhipu', 'glm-secret-1234')
    await processAiService.deleteProviderKey('zhipu')

    expect(requestMock.mock.calls[0]?.[0]).toMatchObject({
      url: '/api/ai/provider-settings/zhipu',
      method: 'put',
      data: { apiKey: 'glm-secret-1234' },
    })
    expect(requestMock.mock.calls[1]?.[0]).toMatchObject({
      url: '/api/ai/provider-settings/zhipu',
      method: 'delete',
    })
  })
})
