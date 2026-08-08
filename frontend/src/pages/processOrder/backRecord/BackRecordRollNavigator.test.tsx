import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { FinishRoll } from '../../../types/processOrder'
import BackRecordRollNavigator from './BackRecordRollNavigator'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'

describe('BackRecordRollNavigator', () => {
  it('does not count trim rows as finished products', () => {
    const markup = renderNavigator([
      ...createFinishes('product', 8),
      ...createFinishes('trim', 8, { isRemain: 1 }),
    ])

    expect(markup).toContain('8 件成品')
    expect(markup).not.toContain('16 件成品')
  })

  it('does not count spare rows as finished products', () => {
    const markup = renderNavigator([
      ...createFinishes('product', 2),
      ...createFinishes('spare', 1, { isSpare: 1 }),
    ])

    expect(markup).toContain('2 件成品')
    expect(markup).not.toContain('3 件成品')
  })
})

function renderNavigator(finishes: FinishRoll[]) {
  const item: BackRecordWorkItem = {
    key: 'roll-1',
    kind: 'roll',
    title: '母卷 1',
    roll: { uuid: 'roll-1', pieceNum: 8, processMode: 1 },
    rollProductions: [],
    isMergeGroup: false,
    sourceMode: 'linked',
    finishes: finishes.map((finish) => ({ finish, bindMode: 'linked' })),
  }

  return renderToStaticMarkup(
    <BackRecordRollNavigator
      activeKey={item.key}
      items={[item]}
      onClear={() => undefined}
      onReopen={() => undefined}
      onSelect={() => undefined}
      onSelectAll={() => undefined}
      onSelectOnly={() => undefined}
      onToggle={() => undefined}
      reopening={false}
      selectedKeys={new Set()}
      values={{}}
    />,
  )
}

function createFinishes(
  prefix: string,
  count: number,
  overrides: Partial<FinishRoll> = {},
): FinishRoll[] {
  return Array.from({ length: count }, (_, index) => ({
    uuid: `${prefix}-${index + 1}`,
    isRemain: 0,
    isSpare: 0,
    ...overrides,
  }))
}
