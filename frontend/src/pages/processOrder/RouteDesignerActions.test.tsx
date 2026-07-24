import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { RouteDesignerActions } from './RouteDesignerWorkspace'
import type { RouteDesignerCommands } from './RouteDesignerWorkspace'

const commands: RouteDesignerCommands = {
  onApply: () => undefined,
  onBack: () => undefined,
  onDeleteFrom: () => undefined,
  onPreview: () => undefined,
  onQuickAppend: () => undefined,
  onRedo: () => undefined,
  onSave: () => undefined,
  onSelect: () => undefined,
  onStagesChange: () => undefined,
  onUndo: () => undefined,
}

describe('工艺路线操作区', () => {
  it('不可操作时禁用命令并说明原因', () => {
    const markup = renderToStaticMarkup(
      <RouteDesignerActions
        commands={commands}
        state={{
          applyDisabledReason: '没有其他同规格母卷',
          canRedo: false,
          canUndo: false,
          previewDisabledReason: '请先配置并加入至少一道工艺',
          saveDisabledReason: '当前路线没有待保存修改',
        }}
      />,
    )

    expect(markup).toContain('aria-label="应用到同规格：没有其他同规格母卷"')
    expect(markup).toContain('aria-label="撤销：当前没有可撤销的修改"')
    expect(markup).toContain('aria-label="预览校验：请先配置并加入至少一道工艺"')
    expect(markup).toContain('aria-label="保存草稿：当前路线没有待保存修改"')
  })

  it('存在可执行操作时保持按钮可用', () => {
    const markup = renderToStaticMarkup(
      <RouteDesignerActions
        commands={commands}
        state={{ canRedo: true, canUndo: true }}
      />,
    )

    expect(markup).toContain('aria-label="应用到同规格"')
    expect(markup).toContain('aria-label="撤销"')
    expect(markup).not.toContain('aria-label="保存草稿" disabled')
  })
})
