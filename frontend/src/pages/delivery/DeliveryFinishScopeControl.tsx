import { Segmented, Space } from 'antd'
import type { ReactElement } from 'react'

export type DeliveryFinishScope = 'product' | 'remain'

interface FinishKindLike {
  isRemain?: number
}

interface Props {
  finishes: FinishKindLike[]
  value: DeliveryFinishScope
  onChange: (value: DeliveryFinishScope) => void
}

export function DeliveryFinishScopeControl({ finishes, onChange, value }: Props): ReactElement {
  const productCount = filterFinishesByScope(finishes, 'product').length
  const remainCount = filterFinishesByScope(finishes, 'remain').length

  return (
    <Space size={12} wrap>
      <Segmented
        value={value}
        options={[
          { label: `成品 ${productCount}`, value: 'product' },
          { label: `余料 ${remainCount}`, value: 'remain' },
        ]}
        onChange={(nextValue) => onChange(nextValue as DeliveryFinishScope)}
      />
    </Space>
  )
}

export function filterFinishesByScope<T extends FinishKindLike>(
  finishes: T[],
  scope: DeliveryFinishScope,
): T[] {
  return finishes.filter((item) => (scope === 'remain' ? item.isRemain === 1 : item.isRemain !== 1))
}

export function finishScopeName(scope: DeliveryFinishScope): string {
  return scope === 'remain' ? '余料' : '成品'
}
