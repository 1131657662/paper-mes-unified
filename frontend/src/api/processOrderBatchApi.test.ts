import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { BackRecordReopenDTO, FinishConfigBatchSaveDTO } from '../types/processOrder'

const { requestMock } = vi.hoisted(() => ({ requestMock: vi.fn() }))

vi.mock('./request', () => ({
  default: requestMock,
}))

import { reopenBackRecordBatch, saveFinishConfigBatch } from './processOrder'

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

    await saveFinishConfigBatch('order-1', dto, 7)

    expect(requestMock).toHaveBeenCalledOnce()
    expect(requestMock).toHaveBeenCalledWith({
      url: '/api/process-orders/order-1/finish-config/batch',
      method: 'post',
      params: { expectedVersion: 7 },
      data: dto,
    })
  })
})

describe('reopenBackRecordBatch', () => {
  beforeEach(() => requestMock.mockReset())

  it('submits the order version and selected mother rolls', async () => {
    const dto: BackRecordReopenDTO = {
      expectedVersion: 8,
      rollUuids: ['roll-1', 'roll-2'],
    }
    requestMock.mockResolvedValue(undefined)

    await reopenBackRecordBatch('order-1', dto)

    expect(requestMock).toHaveBeenCalledOnce()
    expect(requestMock).toHaveBeenCalledWith({
      url: '/api/process-orders/order-1/back-record/reopen',
      method: 'post',
      data: dto,
    })
  })
})
