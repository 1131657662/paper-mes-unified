import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import DocumentListShell from './DocumentListShell'

describe('DocumentListShell page heading', () => {
  it('renders the document title as the route heading', () => {
    const markup = renderToStaticMarkup(
      <DocumentListShell
        title="出库管理"
        createText="新建出库单"
        queue="all"
        queueOptions={[{ label: '全部', value: 'all' }]}
        onCreate={() => undefined}
        onQueueChange={() => undefined}
      >
        <div />
      </DocumentListShell>,
    )

    expect(markup).toContain(
      '<h1 class="document-list-shell__title">出库管理</h1>',
    )
  })
})
