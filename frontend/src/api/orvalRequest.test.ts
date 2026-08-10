import { beforeEach, describe, expect, it, vi } from 'vitest'
import request from './request'
import { orvalRequest } from './orvalRequest'

vi.mock('./request', () => ({ default: vi.fn() }))

describe('orvalRequest', () => {
  beforeEach(() => {
    vi.mocked(request).mockReset()
  })

  it('delegates generated calls to the shared MES request pipeline', async () => {
    vi.mocked(request).mockResolvedValue({ uuid: 'customer-1' })

    await orvalRequest(
      {
        url: '/api/customers/customer-1',
        method: 'GET',
        headers: { Accept: 'application/json' },
      },
      { timeout: 3000, headers: { 'X-Test': 'contract' } },
    )

    expect(request).toHaveBeenCalledWith({
      url: '/api/customers/customer-1',
      method: 'GET',
      timeout: 3000,
      headers: { Accept: 'application/json', 'X-Test': 'contract' },
    })
  })
})
