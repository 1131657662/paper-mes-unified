import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import AppendOrderVersionAlert from './AppendOrderVersionAlert'

describe('AppendOrderVersionAlert', () => {
  it('shows the base and current order versions for a resumed session', () => {
    const markup = renderToStaticMarkup(<AppendOrderVersionAlert session={{
      sessionUuid: 'session-1', orderUuid: 'order-1', baseOrderVersion: 8, currentOrderVersion: 9,
    }} />)

    expect(markup).toContain('已恢复未完成的追加会话')
    expect(markup).toContain('V8')
    expect(markup).toContain('V9')
  })
})
