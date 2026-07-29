import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { RollDraft } from '../types'
import ProcessModeStep from './ProcessModeStep'

describe('加工方式配置', () => {
  it('默认使用单卷模式并提供主工艺选择', () => {
    const markup = renderStep([roll('selected')])

    expect(markup).toContain('批量设置')
    expect(markup).toContain('主工艺')
    expect(markup).toContain('锯纸')
    expect(markup).toContain('复卷')
    expect(markup).not.toContain('选择母卷 1')
  })

  it('直发卷隐藏主工艺并说明后续流程', () => {
    const markup = renderStep([roll('selected', 3)])

    expect(markup).not.toContain('当前母卷主工艺')
    expect(markup).toContain('直发卷不进入工艺配置')
  })
})

function renderStep(rolls: RollDraft[]) {
  return renderToStaticMarkup(
    <ProcessModeStep
      rolls={rolls}
      machines={[]}
      selectedId="selected"
      loading={false}
      onSelect={() => undefined}
      onChange={() => undefined}
      onPrev={() => undefined}
      onNext={() => undefined}
    />,
  )
}

function roll(localId: string, processMode = 1): RollDraft {
  return {
    localId,
    uuid: `uuid-${localId}`,
    paperName: '白卡',
    gramWeight: 300,
    originalWidth: 1200,
    rollWeight: 800,
    processMode,
    mainStepType: processMode === 3 ? undefined : 1,
  }
}
