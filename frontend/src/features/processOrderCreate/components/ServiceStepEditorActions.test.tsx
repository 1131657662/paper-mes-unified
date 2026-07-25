import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import ServiceStepEditorActions from './ServiceStepEditorActions'

describe('附加工艺操作栏', () => {
  it('使用独立且完整的附加工艺动作名称', () => {
    const markup = renderToStaticMarkup(
      <ServiceStepEditorActions
        actions={{ onApply: () => undefined, onReset: () => undefined, onSave: () => undefined }}
        state={{
          analysis: { createCount: 1, excludedCount: 0, targetUuids: ['roll-1'], updateCount: 0 },
          batchSaving: false,
          currentRollUuid: 'roll-1',
          dirty: true,
          disabled: false,
          saving: false,
          selectedRollCount: 1,
          writePending: false,
        }}
      />,
    )

    expect(markup).toContain('还原未保存修改')
    expect(markup).toContain('保存本卷附加工艺')
    expect(markup).toContain('批量应用附加工艺（1 卷）')
    expect(markup).toContain('当前只有本卷')
    expect(markup).not.toContain('保存当前卷')
    expect(markup).not.toContain('应用到选中')
  })
})
