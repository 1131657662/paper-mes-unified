import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { FinishConfigBatchSaveDTO } from '../types/processOrder'

const { requestMock } = vi.hoisted(() => ({ requestMock: vi.fn() }))

vi.mock('./request', () => ({
  default: requestMock,
}))

import { saveFinishConfigBatch } from './processOrder'

describe('saveFinishConfigBatch', () => {
  beforeEach(() => requestMock.mockReset())

  it('submits all mother-roll configurations in one request', async () => {
    const dto: FinishConfigBatchSaveDTO = {
      items: [
        { rollUuid: 'roll-1', config: { processMode: 1, mainStepType: 1 } },
        { rollUuid: 'roll-2', config: { processMode: 1, mainStepType: 2 } },
      ],
    }
    requestMock.mockResolvedValue({ orderUuid: 'order-1', results: [] })

    await saveFinishConfigBatch('order-1', dto)

    expect(requestMock).toHaveBeenCalledOnce()
    expect(requestMock).toHaveBeenCalledWith({
      url: '/api/process-orders/order-1/finish-config/batch',
      method: 'post',
      data: dto,
    })
  })
})
