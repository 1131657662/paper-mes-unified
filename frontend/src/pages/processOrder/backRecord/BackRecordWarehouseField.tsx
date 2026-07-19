import { Form, Select, Typography } from 'antd'
import type { Warehouse } from '../../../types/warehouse'

interface Props {
  error: boolean
  loading: boolean
  warehouses: Warehouse[]
}

export default function BackRecordWarehouseField({ error, loading, warehouses }: Props) {
  const options = warehouses.map((warehouse) => ({
    label: warehouseLabel(warehouse),
    value: warehouse.uuid,
  }))

  return (
    <div className="back-record-warehouse-field">
      <Form.Item
        label="入库仓库"
        name="warehouseUuid"
        rules={[{ required: true, message: '请选择本次回录的入库仓库' }]}
      >
        <Select
          showSearch
          loading={loading}
          options={options}
          optionFilterProp="label"
          placeholder={error ? '仓库档案加载失败' : '请选择入库仓库'}
          status={error ? 'error' : undefined}
        />
      </Form.Item>
      {error && <Typography.Text type="danger">无法加载仓库档案，请刷新后重试</Typography.Text>}
    </div>
  )
}

function warehouseLabel(warehouse: Warehouse) {
  const defaultPrefix = warehouse.isDefault === 1 ? '默认 · ' : ''
  const location = warehouse.location ? ` · ${warehouse.location}` : ''
  return `${defaultPrefix}${warehouse.warehouseName}${location}`
}
