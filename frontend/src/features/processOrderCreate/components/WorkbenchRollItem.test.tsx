import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import WorkbenchRollItem from './WorkbenchRollItem'

describe('工艺工作台母卷项', () => {
  it('保存锁定期间当前卷显示当前标识', () => {
    const markup = renderToStaticMarkup(
      <WorkbenchRollItem
        actions={{
          onSelect: () => undefined,
          onToggle: () => undefined,
        }}
        state={{
          checked: true,
          batchDisabledReason: undefined,
          index: 0,
          interactionDisabled: true,
          machines: [],
          previewStatus: { color: 'success', label: '已保存' },
          roll: {
            localId: 'roll-1',
            uuid: 'uuid-roll-1',
            paperName: '白卡',
            gramWeight: 300,
            originalWidth: 1200,
            rollWeight: 800,
            processMode: 1,
            mainStepType: 2,
          },
          selected: true,
        }}
      />,
    )

    expect(markup).toContain('当前')
    expect(markup).not.toContain('选择母卷 1')
  })

  it('当前编辑母卷显示当前标识而不是可勾选框', () => {
    const markup = renderToStaticMarkup(
      <WorkbenchRollItem actions={{ onSelect: () => undefined, onToggle: () => undefined }}
        state={{ checked: false, index: 0, interactionDisabled: false,
          machines: [], previewStatus: { color: 'success', label: '已保存' },
          roll: { localId: 'roll-1', uuid: 'uuid-roll-1', paperName: '白卡', gramWeight: 300,
            originalWidth: 1200, rollWeight: 800, processMode: 1, mainStepType: 2 }, selected: true }} />,
    )

    expect(markup).toContain('当前')
    expect(markup).not.toContain('选择母卷 1')
  })

  it('不兼容母卷的选择框禁用并保留原因', () => {
    const markup = renderToStaticMarkup(
      <WorkbenchRollItem actions={{ onSelect: () => undefined, onToggle: () => undefined }}
        state={{ checked: false, index: 1, interactionDisabled: false,
          batchDisabledReason: '规格与当前母卷不同', machines: [],
          previewStatus: { color: 'success', label: '已保存' },
          roll: { localId: 'roll-2', uuid: 'uuid-roll-2', paperName: '白卡', gramWeight: 300,
            originalWidth: 1300, rollWeight: 800, processMode: 1, mainStepType: 2 }, selected: false }} />,
    )

    expect(markup).toContain('规格与当前母卷不同')
    expect(markup).toContain('ant-checkbox-disabled')
  })
})
