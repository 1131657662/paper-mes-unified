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
    expect(markup).toContain('请先保存当前修改')
    expect(markup).toContain('disabled=""')
  })

  it('有待办卷时将下一步改为定位动作', () => {
    const markup = renderToStaticMarkup(
      <ConfigStepFooter
        hasUnsavedServiceChanges={false}
        onNext={() => undefined}
        onPrev={() => undefined}
        progress={{ noConfigCount: 0, pendingCount: 2, savedCount: 1, totalCount: 3 }}
        saving={false}
        serviceWritePending={false}
      />,
    )

    expect(markup).toContain('定位下一待办（2 卷）')
  })

  it('names the automatic completion action when auto configuration is enabled', () => {
    const markup = renderToStaticMarkup(
      <ConfigStepFooter
        autoFinishConfigEnabled
        hasUnsavedServiceChanges={false}
        onNext={() => undefined}
        onPrev={() => undefined}
        progress={{ noConfigCount: 0, pendingCount: 2, savedCount: 1, totalCount: 3 }}
        saving={false}
        serviceWritePending={false}
      />,
    )

    expect(markup).toContain('保存待处理方案并继续（2 卷）')
    expect(markup).not.toContain('定位下一待办')
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

  it('加工方案保存期间同时禁用前后导航', () => {
    const markup = renderToStaticMarkup(
      <ConfigStepFooter
        hasUnsavedServiceChanges={false}
        onNext={() => undefined}
        onPrev={() => undefined}
        progress={{ noConfigCount: 0, pendingCount: 0, savedCount: 1, totalCount: 1 }}
        saving
        serviceWritePending={false}
      />,
    )

    expect(markup.match(/disabled=""/g)).toHaveLength(2)
  })
})
