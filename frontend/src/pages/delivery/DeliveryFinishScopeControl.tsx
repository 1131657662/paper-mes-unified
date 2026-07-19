import { ColumnWidthOutlined } from '@ant-design/icons'
import { Button, Segmented, Space, Tooltip } from 'antd'
import type { ReactElement } from 'react'
import { resetResizableTableWidths } from '../../components/resizableTableStorage'
import { DELIVERY_SELECTION_TABLE_STORAGE_KEY } from './deliverySelectionTableConfig'
import { filterFinishesByScope, type DeliveryFinishScope } from './deliveryFinishScope'

interface Props {
  finishes: { finishUuid: string; isRemain?: number }[]
  selectedRowKeys: React.Key[]
  totalCount?: number
  scopeTotals?: { product: number; remain: number }
  value: DeliveryFinishScope
  onChange: (value: DeliveryFinishScope) => void
}

export function DeliveryFinishScopeControl({ finishes, onChange, selectedRowKeys, scopeTotals, totalCount, value }: Props): ReactElement {
  const visibleTotal = totalCount ?? filterFinishesByScope(finishes, value).length
  const productTotal = scopeTotals?.product ?? (value === 'product' ? visibleTotal : filterFinishesByScope(finishes, 'product').length)
  const remainTotal = scopeTotals?.remain ?? (value === 'remain' ? visibleTotal : filterFinishesByScope(finishes, 'remain').length)
  const selectedKeys = new Set(selectedRowKeys.map(String))
  const selectedProducts = filterFinishesByScope(finishes, 'product')
    .filter((item) => selectedKeys.has(item.finishUuid)).length
  const selectedRemains = filterFinishesByScope(finishes, 'remain')
    .filter((item) => selectedKeys.has(item.finishUuid)).length

  return (
    <Space className="delivery-finish-scope" size={8} wrap>
      <Segmented
        aria-label="出库库存类型"
        value={value}
        options={[
          { label: scopeLabel('成品', productTotal, selectedProducts), value: 'product' },
          { label: scopeLabel('余料', remainTotal, selectedRemains), value: 'remain' },
        ]}
        onChange={(nextValue) => onChange(nextValue as DeliveryFinishScope)}
      />
      <Tooltip title="恢复默认列宽">
        <Button
          aria-label="恢复默认列宽"
          icon={<ColumnWidthOutlined />}
          onClick={() => resetResizableTableWidths(DELIVERY_SELECTION_TABLE_STORAGE_KEY)}
        />
      </Tooltip>
    </Space>
  )
}

function scopeLabel(name: string, total: number | undefined, selected: number) {
  const count = total == null ? '' : ` ${total}`
  return selected > 0 ? `${name}${count} · 已选 ${selected}` : `${name}${count}`
}
