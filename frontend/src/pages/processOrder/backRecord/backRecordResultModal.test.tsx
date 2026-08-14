import { Modal } from 'antd'
import { renderToStaticMarkup } from 'react-dom/server'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { showBackRecordResult } from './backRecordResultModal'

describe('showBackRecordResult', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('labels the recorded source count as rolls instead of merge groups', () => {
    const info = vi.spyOn(Modal, 'info').mockReturnValue({
      destroy: vi.fn(),
      update: vi.fn(),
    })

    showBackRecordResult({
      orderCompleted: true,
      orderNo: 'JG-001',
      recordedRollCount: 3,
      remainingRollCount: 0,
      rollChecks: [],
    })

    const content = info.mock.calls[0]![0].content
    const markup = renderToStaticMarkup(<>{content}</>)
    expect(markup).toContain('本批母卷')
    expect(markup).toContain('3 卷')
    expect(markup).not.toContain('3 组')
  })
})
