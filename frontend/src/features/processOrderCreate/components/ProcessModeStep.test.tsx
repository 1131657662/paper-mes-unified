import { renderToStaticMarkup } from 'react-dom/server'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { RollDraft } from '../types'
import ProcessModeStep from './ProcessModeStep'

interface MockButtonProps {
  children?: ReactNode
  disabled?: boolean
  onClick?: () => void
}

const buttonCapture = vi.hoisted(() => ({ items: [] as MockButtonProps[] }))

vi.mock('antd', async () => {
  const actual = await vi.importActual<typeof import('antd')>('antd')
  return {
    ...actual,
    Button: ({ children, disabled, onClick }: MockButtonProps) => {
      buttonCapture.items.push({ children, disabled, onClick })
      return <button disabled={disabled}>{children}</button>
    },
  }
})

describe('加工方式批量设置', () => {
  beforeEach(() => {
    buttonCapture.items = []
  })

  it('显示现场定尺批量选项', () => {
    const markup = renderStep([roll('selected')], vi.fn())

    expect(markup).toContain('应用到已选 1 卷：现场定尺')
  })

  it('现场定尺批量选项默认使用复卷主工艺且只更新已选卷', () => {
    const onChange = vi.fn()
    renderStep([roll('selected'), roll('other')], onChange)

    findButton('应用到已选 1 卷：现场定尺').onClick?.()

    expect(onChange).toHaveBeenCalledWith([
      expect.objectContaining({ localId: 'selected', processMode: 2, mainStepType: 2 }),
      expect.objectContaining({ localId: 'other', processMode: 1, mainStepType: 1 }),
    ])
  })

  it('允许选中直发卷并批量改回标准加工', () => {
    const onChange = vi.fn()
    renderStep([roll('selected', 3), roll('other')], onChange)

    const applyStandard = findButton('应用到已选 1 卷：标准复卷')
    expect(applyStandard.disabled).toBe(false)
    applyStandard.onClick?.()

    expect(onChange).toHaveBeenCalledWith([
      expect.objectContaining({ localId: 'selected', processMode: 1, mainStepType: 2 }),
      expect.objectContaining({ localId: 'other', processMode: 1, mainStepType: 1 }),
    ])
  })
})

function renderStep(rolls: RollDraft[], onChange: (rolls: RollDraft[]) => void) {
  return renderToStaticMarkup(
    <ProcessModeStep
      rolls={rolls}
      machines={[]}
      selectedId="selected"
      loading={false}
      onSelect={() => undefined}
      onChange={onChange}
      onPrev={() => undefined}
      onNext={() => undefined}
    />,
  )
}

function findButton(label: string) {
  const button = buttonCapture.items.find((item) => renderToStaticMarkup(<>{item.children}</>) === label)
  if (!button) throw new Error(`Button not found: ${label}`)
  return button
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
