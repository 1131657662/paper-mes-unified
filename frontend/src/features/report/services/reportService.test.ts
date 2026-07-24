import { beforeEach, describe, expect, it, vi } from 'vitest'

const api = vi.hoisted(() => ({
  metadata: vi.fn(),
  snapshot: vi.fn(),
}))

vi.mock('../../../api/report', async (importOriginal) => ({
  ...await importOriginal<typeof import('../../../api/report')>(),
  getReportQueryMetadata: api.metadata,
  createReportQuerySnapshot: api.snapshot,
}))

import { reportService } from './reportService'

describe('reportService', () => {
  beforeEach(() => vi.clearAllMocks())

  it('loads display metadata without creating a persistent snapshot', async () => {
    api.metadata.mockResolvedValue({ queryId: 'query-1' })

    await reportService.queryMetadata({ dateFrom: '2026-07-01' })

    expect(api.metadata).toHaveBeenCalledWith({ dateFrom: '2026-07-01' })
    expect(api.snapshot).not.toHaveBeenCalled()
  })
})
