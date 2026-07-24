import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import ServiceStepsLoadGate from './ServiceStepsLoadGate'

describe('附加工艺详情加载门控', () => {
  it('加载失败时不开放编辑内容', () => {
    const markup = renderToStaticMarkup(
      <ServiceStepsLoadGate isError isLoading={false} onRetry={() => undefined}>
        <button>保存附加工艺</button>
      </ServiceStepsLoadGate>,
    )
    expect(markup).toContain('附加工艺配置加载失败')
    expect(markup).toContain('重新加载')
    expect(markup).not.toContain('保存附加工艺')
  })
})
