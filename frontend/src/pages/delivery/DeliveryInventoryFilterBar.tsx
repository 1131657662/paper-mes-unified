import { Input, InputNumber, Segmented, Select } from 'antd'
import type { DeliveryInventoryFilter } from '../../types/deliveryInventory'
import type { Warehouse } from '../../types/warehouse'
import { inventoryTypeFrom, stockStateFrom } from './deliveryInventoryModel'

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
      <Segmented
        aria-label="库存状态"
        value={filters.stockState ?? 0}
        options={[{ label: '全部', value: 0 }, { label: '可出库', value: 1 }, { label: '已占用', value: 2 }]}
        onChange={(value) => onChange({ ...filters, stockState: stockStateFrom(value) })}
      />
      <InputNumber
        aria-label="最小库龄（天）"
        min={0}
        max={36500}
        controls={false}
        placeholder="库龄 ≥ 天"
        value={filters.stockAgeMinDays}
        onChange={(value) => onChange({ ...filters, stockAgeMinDays: value ?? undefined })}
      />
      <Select
        allowClear
        placeholder="全部类型"
        value={filters.inventoryType}
        options={[
          { label: '普通成品', value: 1 },
          { label: '余料', value: 2 },
          { label: '原纸直发', value: 3 },
        ]}
        onChange={(value) => onChange({ ...filters, inventoryType: inventoryTypeFrom(value) })}
      />
    </div>
  )
}
