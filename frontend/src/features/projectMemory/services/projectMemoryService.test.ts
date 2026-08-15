import { beforeEach, describe, expect, it, vi } from 'vitest'
import { request } from '../../../api/request'
import { projectMemoryService } from './projectMemoryService'

vi.mock('../../../api/request', () => ({ request: vi.fn() }))

const requestMock = vi.mocked(request)

describe('projectMemoryService', () => {
  beforeEach(() => requestMock.mockReset())

  it('sends patch concurrency and idempotency fields unchanged', async () => {
    requestMock.mockResolvedValue({ memoryVersion: '1.0.1' })
    const payload = {
      expectedMemoryVersion: '1.0.0',
      idempotencyKey: 'patch-key',
      operations: [{ op: 'replace' as const, path: '/rules/r1/status', value: 'ACTIVE' }],
      reason: '现场确认',
    }

    await projectMemoryService.patch(payload)

    expect(requestMock).toHaveBeenCalledWith({
      url: '/api/ai/project-memory/patch',
      method: 'post',
      data: payload,
    })
  })

  it('loads version metadata from the dedicated endpoint', async () => {
    requestMock.mockResolvedValue([])

    await projectMemoryService.versions()

    expect(requestMock).toHaveBeenCalledWith({
      url: '/api/ai/project-memory/versions',
      method: 'get',
    })
  })
})
