import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import WorkbenchRollItem from './WorkbenchRollItem'

describe('WorkbenchRollItem machine summary', () => {
  it('uses the saved plan machine instead of a stale roll field', () => {
    const markup = renderToStaticMarkup(
      <WorkbenchRollItem
        actions={{ onSelect: () => undefined, onToggle: () => undefined }}
        state={{
          checked: false,
          index: 0,
          interactionDisabled: false,
          machineUuid: 'machine-1',
          machines: [{ uuid: 'machine-1', machineName: '演示复卷机', status: 1 }],
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
          selected: false,
        }}
      />,
    )

    expect(markup).toContain('演示复卷机')
    expect(markup).not.toContain('未选机台')
  })
})
