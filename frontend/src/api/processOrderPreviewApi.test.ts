import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { ProcessPlanPreviewRequestDTO } from '../types/processOrder'

const { requestMock } = vi.hoisted(() => ({ requestMock: vi.fn() }))

vi.mock('./request', () => ({
  default: requestMock,
}))

import { previewProcessPlan } from './processOrder'

describe('previewProcessPlan', () => {
  beforeEach(() => requestMock.mockReset())

  it('leaves preview failures to the inline preview error state', async () => {
    const dto: ProcessPlanPreviewRequestDTO = {
      expectedVersion: 3,
      originalUuid: 'roll-1',
      plan: { processMode: 1, mainStepType: 2 },
    }
    const controller = new AbortController()
    requestMock.mockResolvedValue({ ready: true })

    await previewProcessPlan('order-1', dto, controller.signal)

    expect(requestMock).toHaveBeenCalledWith({
      url: '/api/process-orders/order-1/rolls/plan-preview',
      method: 'post',
      data: dto,
      signal: controller.signal,
      silentError: true,
    })
  })
})
