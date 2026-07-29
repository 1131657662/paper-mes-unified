import { beforeEach, describe, expect, it, vi } from 'vitest'

const { requestMock } = vi.hoisted(() => ({ requestMock: vi.fn() }))

vi.mock('./request', () => ({ default: requestMock }))

import {
  addDraftProcessStep,
  addDraftProcessStepsBatch,
  deleteDraftProcessStep,
  updateDraftProcessStep,
  type ProcessStepDTO,
} from './processOrder'

const step: ProcessStepDTO = { originalUuid: 'roll-1', stepType: 3, isMain: 0 }

describe('draft additional-process API', () => {
  beforeEach(() => requestMock.mockReset())

  it('uses ORDER_CREATE-scoped draft endpoints and defers uncertain errors', async () => {
    requestMock.mockResolvedValue(undefined)

    await addDraftProcessStep('order-1', step, 4)
    await addDraftProcessStepsBatch('order-1', { steps: [step] }, 5)
    await updateDraftProcessStep('step-1', step, 6)
    await deleteDraftProcessStep('step-1', 7)

    expect(requestMock.mock.calls).toEqual([
      [{
        url: '/api/process-orders/order-1/draft-steps', method: 'post',
        data: { ...step, expectedVersion: 4 },
        deferUncertainErrorNotification: true,
      }],
      [{
        url: '/api/process-orders/order-1/draft-steps/batch', method: 'post',
        data: { steps: [step], expectedVersion: 5 },
        deferUncertainErrorNotification: true,
      }],
      [{
        url: '/api/process-orders/draft-steps/step-1', method: 'put',
        data: { ...step, expectedVersion: 6 },
        deferUncertainErrorNotification: true,
      }],
      [{
        url: '/api/process-orders/draft-steps/step-1', method: 'delete', params: { expectedVersion: 7 },
        deferUncertainErrorNotification: true,
      }],
    ])
  })
})
