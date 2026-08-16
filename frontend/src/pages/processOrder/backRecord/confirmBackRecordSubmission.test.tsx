import { Modal } from 'antd'
import { renderToStaticMarkup } from 'react-dom/server'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { confirmBackRecordSubmission } from './confirmBackRecordSubmission'

describe('confirmBackRecordSubmission', () => {
  afterEach(() => vi.restoreAllMocks())

  it('describes completion-only as a state close without another receipt', () => {
    const confirm = vi.spyOn(Modal, 'confirm').mockReturnValue({
      destroy: vi.fn(),
      update: vi.fn(),
    })

    void confirmBackRecordSubmission({
      completeOrder: true,
      orderNo: 'JG-001',
      selectedCount: 0,
      warehouseName: '历史仓库',
    })

    const config = confirm.mock.calls[0]![0]
    const markup = renderToStaticMarkup(<>{config.content}</>)
    expect(config.title).toBe('确认关闭整单？')
    expect(markup).toContain('不会重复入库')
    expect(markup).toContain('不会改写已回录明细')
    expect(markup).not.toContain('历史仓库')
  })
})
