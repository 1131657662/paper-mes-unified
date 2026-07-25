import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import ConfigStepFooter from './ConfigStepFooter'

describe('工艺配置底栏', () => {
  it('只展示导航动作和持久化进度摘要', () => {
    const markup = renderToStaticMarkup(
      <ConfigStepFooter
        hasUnsavedServiceChanges
        onNext={() => undefined}
        onPrev={() => undefined}
        progress={{ noConfigCount: 1, pendingCount: 2, savedCount: 3, totalCount: 6 }}
        saving={false}
        serviceWritePending={false}
      />,
    )

    expect(markup.match(/<button/g)).toHaveLength(2)
    expect(markup).toContain('上一步')
    expect(markup).toContain('共 6 卷 · 已保存 3 · 无需配置 1 · 待处理 2')
    expect(markup).toContain('当前卷附加工艺有未保存修改')
    expect(markup).toContain('下一步：预览确认')
  })

  it('附加工艺写入期间禁用下一步', () => {
    const markup = renderToStaticMarkup(
      <ConfigStepFooter
        hasUnsavedServiceChanges={false}
        onNext={() => undefined}
        onPrev={() => undefined}
        progress={{ noConfigCount: 0, pendingCount: 0, savedCount: 1, totalCount: 1 }}
        saving={false}
        serviceWritePending
      />,
    )

    expect(markup).toContain('附加工艺正在保存或删除')
    expect(markup).toContain('disabled=""')
  })
})
