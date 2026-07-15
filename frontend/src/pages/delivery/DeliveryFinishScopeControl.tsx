import { ColumnWidthOutlined } from '@ant-design/icons'
import { Button, Segmented, Space, Tooltip } from 'antd'
import type { ReactElement } from 'react'
import { resetResizableTableWidths } from '../../components/resizableTableStorage'
import { DELIVERY_SELECTION_TABLE_STORAGE_KEY } from './deliverySelectionTableConfig'
import { filterFinishesByScope, type DeliveryFinishScope } from './deliveryFinishScope'

interface Props {
  finishes: { finishUuid: string; isRemain?: number }[]
  selectedRowKeys: React.Key[]
  value: DeliveryFinishScope
  onChange: (value: DeliveryFinishScope) => void
}

export function DeliveryFinishScopeControl({ finishes, onChange, selectedRowKeys, value }: Props): ReactElement {
  const productCount = filterFinishesByScope(finishes, 'product').length
  const remainCount = filterFinishesByScope(finishes, 'remain').length
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
          { label: scopeLabel('成品', productCount, selectedProducts), value: 'product' },
          { label: scopeLabel('余料', remainCount, selectedRemains), value: 'remain' },
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

function scopeLabel(name: string, total: number, selected: number) {
  return selected > 0 ? `${name} ${total} · 已选 ${selected}` : `${name} ${total}`
}
