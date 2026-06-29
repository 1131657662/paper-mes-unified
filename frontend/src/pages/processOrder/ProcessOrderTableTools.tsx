import { Button, Checkbox, Dropdown, Popover, Space, Tooltip } from 'antd'
import { ColumnHeightOutlined, ReloadOutlined, SettingOutlined } from '@ant-design/icons'
import type { MenuProps } from 'antd'
import type { ProColumns, ProTableProps } from '@ant-design/pro-components'
import type { ProcessOrder } from '../../types/processOrder'

type TableDensity = 'large' | 'middle' | 'small'

interface Props {
  columns: ProColumns<ProcessOrder>[]
  columnsState: ProTableProps<ProcessOrder, any>['columnsState']
  tableSize: TableDensity
  onReload: () => void
  onTableSizeChange: (size: TableDensity) => void
}

export default function ProcessOrderTableTools({
  columns,
  columnsState,
  onReload,
  onTableSizeChange,
  tableSize,
}: Props) {
  return (
    <Space size={8} className="process-order-shell__tools">
      <Tooltip title="刷新">
        <Button type="text" icon={<ReloadOutlined />} onClick={onReload} />
      </Tooltip>
      <Dropdown menu={{ items: densityItems(onTableSizeChange), selectedKeys: [tableSize ?? 'middle'] }} trigger={['click']}>
        <Tooltip title="表格密度">
          <Button type="text" icon={<ColumnHeightOutlined />} />
        </Tooltip>
      </Dropdown>
      <Popover
        trigger="click"
        placement="bottomRight"
        title="列展示"
        content={<ColumnVisibility columns={columns} columnsState={columnsState} />}
      >
        <Tooltip title="列设置">
          <Button type="text" icon={<SettingOutlined />} />
        </Tooltip>
      </Popover>
    </Space>
  )
}

function ColumnVisibility({
  columns,
  columnsState,
}: {
  columns: ProColumns<ProcessOrder>[]
  columnsState: ProTableProps<ProcessOrder, any>['columnsState']
}) {
  const value = (columnsState?.value ?? {}) as Record<string, { show?: boolean }>
  return (
    <div className="process-order-column-settings">
      {columns.filter(isConfigurableColumn).map((column, index) => {
        const key = columnKey(column, index)
        if (!key) return null
        return (
          <Checkbox
            key={key}
            checked={value[key]?.show !== false}
            onChange={(event) => updateColumnShow(columnsState, key, event.target.checked)}
          >
            {columnTitle(column)}
          </Checkbox>
        )
      })}
    </div>
  )
}

function densityItems(onChange: (size: TableDensity) => void): MenuProps['items'] {
  return [
    { key: 'large', label: '宽松', onClick: () => onChange('large') },
    { key: 'middle', label: '中等', onClick: () => onChange('middle') },
    { key: 'small', label: '紧凑', onClick: () => onChange('small') },
  ]
}

function updateColumnShow(
  columnsState: ProTableProps<ProcessOrder, any>['columnsState'],
  key: string,
  show: boolean,
) {
  const value = (columnsState?.value ?? {}) as Record<string, { show?: boolean }>
  columnsState?.onChange?.({ ...value, [key]: { ...value[key], show } })
}

function isConfigurableColumn(column: ProColumns<ProcessOrder>) {
  return !column.hideInTable && column.valueType !== 'option' && column.hideInSetting !== true
}

function columnKey(column: ProColumns<ProcessOrder>, index?: number) {
  if (column.key != null) return String(column.key)
  if (Array.isArray(column.dataIndex)) return column.dataIndex.join('.')
  if (column.dataIndex != null) return String(column.dataIndex)
  return index == null ? undefined : String(index)
}

function columnTitle(column: ProColumns<ProcessOrder>) {
  return typeof column.title === 'string' ? column.title : '未命名列'
}
