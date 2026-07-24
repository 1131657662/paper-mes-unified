import { Form } from 'antd'
import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it, vi } from 'vitest'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import { PrintDrawerContent } from './PrintIssueDrawer'

vi.mock('./PrintPreviewSheet', () => ({ default: () => <section>打印版本正文</section> }))

describe('加工单打印版本加载态', () => {
  it('版本加载完成前不使用当前详情渲染临时预览', () => {
    const markup = renderToStaticMarkup(<LoadingContent />)

    expect(markup).toContain('aria-label="打印版本加载中"')
    expect(markup).not.toContain('打印版本正文')
  })

  it('补打完成后再次输出仍保留补打原因表单', () => {
    const markup = renderToStaticMarkup(<ReprintContent />)

    expect(markup).toContain('补打原因')
  })
})

function LoadingContent() {
  const [form] = Form.useForm()
  return (
    <PrintDrawerContent
      copies={1}
      detail={detail()}
      form={form}
      loading
      mode="preview"
      pendingConfirmation={null}
      result={null}
      version="ISSUED"
      onCopiesChange={() => undefined}
      onVersionChange={() => undefined}
    />
  )
}

function ReprintContent() {
  const [form] = Form.useForm()
  return (
    <PrintDrawerContent
      copies={1}
      detail={detail()}
      form={form}
      loading={false}
      mode="reprint"
      pendingConfirmation={null}
      result={{ printStatus: 1, reprint: true }}
      version="ISSUED"
      onCopiesChange={() => undefined}
      onVersionChange={() => undefined}
    />
  )
}

function detail(): ProcessOrderDetailVO {
  return { order: { uuid: 'order-1' }, originalRolls: [], rolls: [], finishRolls: [], steps: [] }
}
