import { Input, InputNumber, Segmented, Select } from 'antd'
import type { DeliveryInventoryFilter } from '../../types/deliveryInventory'
import type { Warehouse } from '../../types/warehouse'
import {
  filtersForInventoryQuickFilter,
  inventoryProductLabel,
  inventoryQuickFilterFrom,
  inventoryQuickFilterValue,
  stockStateFrom,
} from './deliveryInventoryModel'

interface Props {
  filters: DeliveryInventoryFilter
  onChange: (filters: DeliveryInventoryFilter) => void
  onSearch: (keyword?: string) => void
  warehouses?: Warehouse[]
}

export default function DeliveryInventoryFilterBar({ filters, onChange, onSearch, warehouses = [] }: Props) {
  return (
    <div className="delivery-inventory-filters">
      <Input.Search
        key={filters.keyword ?? ''}
        allowClear
        defaultValue={filters.keyword}
        placeholder="客户 / 卷号 / 加工单 / 品名"
        onSearch={(value) => onSearch(value.trim() || undefined)}
      />
      <Select
        allowClear
        showSearch
        placeholder="全部仓库"
        value={filters.warehouseUuid}
        options={warehouses.filter((item) => item.status === 1).map((item) => ({ label: item.warehouseName, value: item.uuid }))}
        optionFilterProp="label"
        onChange={(value) => onChange({ ...filters, warehouseUuid: value })}
      />
      <div className="delivery-inventory-filter-group">
        <span className="delivery-inventory-filter-group__label">库存状态</span>
        <Segmented
          aria-label="库存状态"
          value={filters.stockState ?? 0}
          options={[{ label: '全部', value: 0 }, { label: '可出库', value: 1 }, { label: '已占用', value: 2 }]}
          onChange={(value) => onChange({ ...filters, stockState: stockStateFrom(value) })}
        />
      </div>
      <InputNumber
        aria-label="最小库龄（天）"
        min={0}
        max={36500}
        controls={false}
        placeholder="库龄 ≥ 天"
        value={filters.stockAgeMinDays}
        onChange={(value) => onChange({ ...filters, stockAgeMinDays: value ?? undefined })}
      />
      <div className="delivery-inventory-filter-group delivery-inventory-scope-group">
        <span className="delivery-inventory-filter-group__label">库存类型</span>
        <Segmented
          className="delivery-inventory-scope-segmented"
          aria-label="库存类型"
          value={inventoryQuickFilterValue(filters)}
          options={[
            { label: '全部', value: 'all' },
            { label: inventoryProductLabel(filters), value: 'product' },
            { label: '余料', value: 'remain' },
            { label: '原纸直发', value: 'direct' },
          ]}
          onChange={(value) => onChange(filtersForInventoryQuickFilter(filters, inventoryQuickFilterFrom(value)))}
        />
      </div>
    </div>
  )
}
